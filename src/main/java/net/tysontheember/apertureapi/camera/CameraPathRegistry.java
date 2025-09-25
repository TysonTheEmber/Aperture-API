package net.tysontheember.apertureapi.camera;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Datapack JSON loader + in-memory registry for camera paths. Loads files at
 * data/apertureapi/camera_paths/*.json
 */
public class CameraPathRegistry extends SimpleJsonResourceReloadListener {
  public static final Logger LOGGER = LogManager.getLogger("CameraPathRegistry");
  private static final Gson GSON = new GsonBuilder().setLenient().create();
  // Folder inside data packs (namespace is handled by the resource system)
  public static final String FOLDER = "camera_paths";

  private static Map<String, CameraPathDef> PATHS = Collections.emptyMap();

  public CameraPathRegistry() {
    super(GSON, FOLDER);
  }

  @Override
  protected void apply(
      Map<ResourceLocation, JsonElement> input,
      ResourceManager resourceManager,
      ProfilerFiller profiler) {
    Map<String, CameraPathDef> temp = new HashMap<>();
    LOGGER.info(
        "CameraPathRegistry: scanning folder '{}' with {} resource(s)", FOLDER, input.size());
    input.keySet().forEach(rl -> LOGGER.debug("Found resource candidate: {}", rl));
    input.forEach(
        (rl, json) -> {
          try {
            JsonObject obj = json.getAsJsonObject();
            CameraPathDef def = GSON.fromJson(obj, CameraPathDef.class);
            if (!validate(def, rl)) {
              return;
            }
            if (temp.containsKey(def.id)) {
              LOGGER.warn(
                  "Duplicate camera path id '{}' encountered. Keeping the last one ({}).",
                  def.id,
                  rl);
            }
            temp.put(def.id, def);
          } catch (Exception e) {
            LOGGER.error("Failed to parse camera path {}: {}", rl, e.toString());
          }
        });
    PATHS = Map.copyOf(temp);
    LOGGER.info("Loaded {} camera path(s) from datapacks.", PATHS.size());
  }

  private static boolean validate(CameraPathDef def, ResourceLocation rl) {
    if (def == null) {
      LOGGER.warn("Skipping camera path {} (null root)", rl);
      return false;
    }
    if (def.id == null || def.id.isBlank()) {
      LOGGER.warn("Skipping camera path {} (missing id)", rl);
      return false;
    }
    if (def.keyframes == null || def.keyframes.isEmpty()) {
      LOGGER.warn("Skipping '{}' in {}: no keyframes.", def.id, rl);
      return false;
    }
    float last = -Float.MAX_VALUE;
    int idx = 0;
    for (CameraPathDef.KeyframeDef kf : def.keyframes) {
      if (kf == null) {
        LOGGER.warn("'{}' in {}: keyframe {} is null", def.id, rl, idx);
        return false;
      }
      if (kf.pos == null) {
        LOGGER.warn("'{}' in {}: keyframe {} missing pos", def.id, rl, idx);
        return false;
      }
      if (kf.lookAt == null && kf.rot == null) {
        LOGGER.debug(
            "'{}' in {}: keyframe {} has no rot or lookAt; rotation will be resolved at runtime",
            def.id,
            rl,
            idx);
      }
      if (kf.time < last) {
        LOGGER.warn(
            "'{}' in {}: keyframe {} time {} < previous {} (must be non-decreasing)",
            def.id,
            rl,
            idx,
            kf.time,
            last);
        return false;
      }
      last = kf.time;
      idx++;
    }
    return true;
  }

  public static Map<String, CameraPathDef> all() {
    return PATHS;
  }

  public static CameraPathDef get(String id) {
    return PATHS.get(id);
  }

  public static boolean exists(String id) {
    return PATHS.containsKey(id);
  }
}
