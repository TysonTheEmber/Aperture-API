package net.tysontheember.apertureapi.commandutil;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.fml.loading.FMLPaths;
import net.tysontheember.apertureapi.common.GlobalCameraSavedData;
import net.tysontheember.apertureapi.common.animation.CameraKeyframe;
import net.tysontheember.apertureapi.common.animation.GlobalCameraPath;
import net.tysontheember.apertureapi.common.animation.PathInterpolator;
import net.tysontheember.apertureapi.common.animation.TimeBezierController;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class CameraPathService {
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  public record ExportResult(boolean success, String message, Path path) {}

  public record ImportResult(boolean success, String message, int count) {}

  public static List<String> list(ServerLevel level) {
    var data = GlobalCameraSavedData.getData(level);
    List<String> ids = new ArrayList<>();
    for (GlobalCameraPath p : data.getPaths()) ids.add(p.getId());
    ids.sort(String::compareToIgnoreCase);
    return ids;
  }

  public static boolean create(ServerLevel level, String name) {
    var data = GlobalCameraSavedData.getData(level);
    if (data.getPath(name) != null) return false;
    GlobalCameraPath path = new GlobalCameraPath(name, null);
    data.addPath(path);
    return true;
  }

  public static boolean delete(ServerLevel level, String name) {
    var data = GlobalCameraSavedData.getData(level);
    if (data.getPath(name) == null) return false;
    data.removePath(name);
    return true;
  }

  public static boolean rename(ServerLevel level, String oldName, String newName) {
    var data = GlobalCameraSavedData.getData(level);
    if (data.getPath(newName) != null) return false;
    var path = data.getPath(oldName);
    if (path == null) return false;
    // Use provided resetID to generate a new instance with the same content
    GlobalCameraPath renamed = path.resetID(newName);
    data.addPath(renamed);
    data.removePath(oldName);
    return true;
  }

  public static ExportResult exportOne(ServerLevel level, String name) {
    return exportAs(level, name, name + ".json");
  }

  public static ExportResult exportAs(ServerLevel level, String name, String fileName) {
    var data = GlobalCameraSavedData.getData(level);
    var path = data.getPath(name);
    if (path == null) return new ExportResult(false, "Unknown path: " + name, null);
    try {
      Path dir = ensureConfigDir();
      String fn = fileName.endsWith(".json") ? fileName : (fileName + ".json");
      Path file = dir.resolve(safeFileName(fn));
      JsonObject root = toJson(path);
      String json = GSON.toJson(root);
      Path tmp = file.resolveSibling(file.getFileName().toString() + ".tmp");
      Files.writeString(tmp, json, StandardCharsets.UTF_8);
      Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      return new ExportResult(true, "", file);
    } catch (IOException e) {
      return new ExportResult(false, "Failed to export: " + e.getMessage(), null);
    }
  }

  public static ImportResult importFile(ServerLevel level, String fileOrDir, boolean overwrite) {
    Path p = Path.of(fileOrDir);
    if (!p.isAbsolute())
      p = FMLPaths.CONFIGDIR.get().resolve("aperture").resolve("paths").resolve(fileOrDir);
    List<Path> files = new ArrayList<>();
    if (Files.isDirectory(p)) {
      try (var s = Files.list(p)) {
        s.filter(f -> f.toString().endsWith(".json")).forEach(files::add);
      } catch (IOException e) {
        return new ImportResult(false, e.getMessage(), 0);
      }
    } else {
      files.add(p);
    }
    int count = 0;
    var data = GlobalCameraSavedData.getData(level);
    for (Path f : files) {
      try {
        String text = Files.readString(f, StandardCharsets.UTF_8);
        JsonObject obj = GSON.fromJson(text, JsonObject.class);
        GlobalCameraPath path = fromJson(obj);
        if (!overwrite && data.getPath(path.getId()) != null) continue;
        data.addPath(path);
        count++;
      } catch (Exception e) {
        return new ImportResult(
            false, "Failed at " + f.getFileName() + ": " + e.getMessage(), count);
      }
    }
    return new ImportResult(true, "", count);
  }

  private static Path ensureConfigDir() throws IOException {
    Path dir = FMLPaths.CONFIGDIR.get().resolve("aperture").resolve("paths");
    Files.createDirectories(dir);
    return dir;
  }

  private static String safeFileName(String id) {
    return id.replaceAll("[^a-zA-Z0-9_.-]+", "_");
  }

  private static JsonObject toJson(GlobalCameraPath path) {
    JsonObject root = new JsonObject();
    root.addProperty("id", path.getId());
    root.addProperty("version", path.getVersion());
    UUID last = path.getLastModifier();
    root.addProperty(
        "lastModifier", last == null ? "00000000-0000-0000-0000-000000000000" : last.toString());
    root.addProperty("native", path.isNativeMode());
    JsonArray keyframes = new JsonArray();
    for (Int2ObjectMap.Entry<CameraKeyframe> e : path.getEntries()) {
      int time = e.getIntKey();
      var kf = e.getValue();
      JsonObject k = new JsonObject();
      k.addProperty("time", time);
      var pos = new JsonArray();
      var p = kf.getPos();
      pos.add(p.x);
      pos.add(p.y);
      pos.add(p.z);
      k.add("pos", pos);
      var rot = new JsonArray();
      var r = kf.getRot();
      rot.add(r.x);
      rot.add(r.y);
      rot.add(r.z);
      k.add("rot", rot);
      k.addProperty("fov", kf.getFov());
      k.addProperty("pathType", kf.getPathInterpolator().index);
      k.addProperty("posType", kf.getPosTimeInterpolator().index);
      k.addProperty("rotType", kf.getRotTimeInterpolator().index);
      k.addProperty("fovType", kf.getFovTimeInterpolator().index);
      if (kf.getPathInterpolator()
          == net.tysontheember.apertureapi.common.animation.PathInterpolator.BEZIER) {
        JsonObject bez = new JsonObject();
        var left = new JsonArray();
        var right = new JsonArray();
        var L = kf.getPathBezier().getLeft();
        var R = kf.getPathBezier().getRight();
        left.add(L.x);
        left.add(L.y);
        left.add(L.z);
        right.add(R.x);
        right.add(R.y);
        right.add(R.z);
        bez.add("left", left);
        bez.add("right", right);
        k.add("pathBezier", bez);
      }
      if (kf.getPosTimeInterpolator()
          == net.tysontheember.apertureapi.common.animation.TimeInterpolator.BEZIER)
        k.add("posBezier", bez2(kf.getPosBezier()));
      if (kf.getRotTimeInterpolator()
          == net.tysontheember.apertureapi.common.animation.TimeInterpolator.BEZIER)
        k.add("rotBezier", bez2(kf.getRotBezier()));
      if (kf.getFovTimeInterpolator()
          == net.tysontheember.apertureapi.common.animation.TimeInterpolator.BEZIER)
        k.add("fovBezier", bez2(kf.getFovBezier()));
      JsonObject entry = new JsonObject();
      entry.addProperty("time", time);
      entry.add("keyframe", k);
      keyframes.add(entry);
    }
    root.add("keyframes", keyframes);
    return root;
  }

  private static JsonObject bez2(TimeBezierController c) {
    JsonObject o = new JsonObject();
    Vector2f L = c.getLeft();
    Vector2f R = c.getRight();
    JsonArray left = new JsonArray();
    JsonArray right = new JsonArray();
    left.add(L.x);
    left.add(L.y);
    right.add(R.x);
    right.add(R.y);
    o.add("left", left);
    o.add("right", right);
    return o;
  }

  private static GlobalCameraPath fromJson(JsonObject root) {
    String id = root.get("id").getAsString();
    boolean nativeMode = root.has("native") && root.get("native").getAsBoolean();
    GlobalCameraPath path = new GlobalCameraPath(id, null);
    if (nativeMode) path.setNativeMode(true);
    for (var el : root.getAsJsonArray("keyframes")) {
      JsonObject entry = el.getAsJsonObject();
      int time = entry.get("time").getAsInt();
      JsonObject k = entry.getAsJsonObject("keyframe");
      var posArr = k.getAsJsonArray("pos");
      var rotArr = k.getAsJsonArray("rot");
      Vector3f pos =
          new Vector3f(
              posArr.get(0).getAsFloat(), posArr.get(1).getAsFloat(), posArr.get(2).getAsFloat());
      Vector3f rot =
          new Vector3f(
              rotArr.get(0).getAsFloat(), rotArr.get(1).getAsFloat(), rotArr.get(2).getAsFloat());
      float fov = k.get("fov").getAsFloat();
      var keyframe =
          new net.tysontheember.apertureapi.common.animation.CameraKeyframe(pos, rot, fov);
      int pathType = k.get("pathType").getAsInt();
      keyframe.setPathInterpolator(PathInterpolator.values()[pathType]);
      keyframe.setPosTimeInterpolator(
          net.tysontheember.apertureapi.common.animation.TimeInterpolator.values()[
              k.get("posType").getAsInt()]);
      keyframe.setRotTimeInterpolator(
          net.tysontheember.apertureapi.common.animation.TimeInterpolator.values()[
              k.get("rotType").getAsInt()]);
      keyframe.setFovTimeInterpolator(
          net.tysontheember.apertureapi.common.animation.TimeInterpolator.values()[
              k.get("fovType").getAsInt()]);
      path.add(time, keyframe);
    }
    return path;
  }
}
