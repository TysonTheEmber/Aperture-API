package net.tysontheember.apertureapi.client.register;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tysontheember.apertureapi.ApertureAPI;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(
    modid = ApertureAPI.MODID,
    value = Dist.CLIENT,
    bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModKeyMapping {
  private static ArrayList<Lazy<KeyMapping>> list = new ArrayList<>();

  public static final Lazy<KeyMapping> ADD_GLOBAL_CAMERA_POINT =
      register(
          new KeyMapping(
              "key." + ApertureAPI.MODID + ".add_global_camera_point",
              KeyConflictContext.IN_GAME,
              InputConstants.Type.KEYSYM,
              GLFW.GLFW_KEY_UNKNOWN,
              "key.categories." + ApertureAPI.MODID));

  public static final Lazy<KeyMapping> DELETE_GLOBAL_CAMERA_POINT =
      register(
          new KeyMapping(
              "key." + ApertureAPI.MODID + ".delete_global_camera_point",
              KeyConflictContext.IN_GAME,
              InputConstants.Type.KEYSYM,
              GLFW.GLFW_KEY_UNKNOWN,
              "key.categories." + ApertureAPI.MODID));

  public static final Lazy<KeyMapping> EDIT_MODE =
      register(
          new KeyMapping(
              "key." + ApertureAPI.MODID + ".edit_mode",
              KeyConflictContext.IN_GAME,
              InputConstants.Type.KEYSYM,
              GLFW.GLFW_KEY_UNKNOWN,
              "key.categories." + ApertureAPI.MODID));

  public static final Lazy<KeyMapping> VIEW_MODE =
      register(
          new KeyMapping(
              "key." + ApertureAPI.MODID + ".view_mode",
              KeyConflictContext.IN_GAME,
              InputConstants.Type.KEYSYM,
              GLFW.GLFW_KEY_UNKNOWN,
              "key.categories." + ApertureAPI.MODID));

  public static final Lazy<KeyMapping> ROTATE_MODE =
      register(
          new KeyMapping(
              "key." + ApertureAPI.MODID + ".rotate_mode",
              KeyConflictContext.IN_GAME,
              InputConstants.Type.KEYSYM,
              GLFW.GLFW_KEY_UNKNOWN,
              "key.categories." + ApertureAPI.MODID));

  public static final Lazy<KeyMapping> POINT_SETTING =
      register(
          new KeyMapping(
              "key." + ApertureAPI.MODID + ".point_setting",
              KeyConflictContext.IN_GAME,
              InputConstants.Type.KEYSYM,
              GLFW.GLFW_KEY_UNKNOWN,
              "key.categories." + ApertureAPI.MODID));

  public static final Lazy<KeyMapping> PREVIEW_MODE =
      register(
          new KeyMapping(
              "key." + ApertureAPI.MODID + ".preview_mode",
              KeyConflictContext.IN_GAME,
              InputConstants.Type.KEYSYM,
              GLFW.GLFW_KEY_UNKNOWN,
              "key.categories." + ApertureAPI.MODID));

  public static final Lazy<KeyMapping> PLAY =
      register(
          new KeyMapping(
              "key." + ApertureAPI.MODID + ".play",
              KeyConflictContext.IN_GAME,
              InputConstants.Type.KEYSYM,
              GLFW.GLFW_KEY_UNKNOWN,
              "key.categories." + ApertureAPI.MODID));

  public static final Lazy<KeyMapping> RESET =
      register(
          new KeyMapping(
              "key." + ApertureAPI.MODID + ".reset",
              KeyConflictContext.IN_GAME,
              InputConstants.Type.KEYSYM,
              GLFW.GLFW_KEY_UNKNOWN,
              "key.categories." + ApertureAPI.MODID));

  public static final Lazy<KeyMapping> SET_CAMERA_TIME =
      register(
          new KeyMapping(
              "key." + ApertureAPI.MODID + ".set_camera_time",
              KeyConflictContext.IN_GAME,
              InputConstants.Type.KEYSYM,
              GLFW.GLFW_KEY_UNKNOWN,
              "key.categories." + ApertureAPI.MODID));

  public static final Lazy<KeyMapping> BACK =
      register(
          new KeyMapping(
              "key." + ApertureAPI.MODID + ".back",
              KeyConflictContext.IN_GAME,
              InputConstants.Type.KEYSYM,
              GLFW.GLFW_KEY_UNKNOWN,
              "key.categories." + ApertureAPI.MODID));

  public static final Lazy<KeyMapping> FORWARD =
      register(
          new KeyMapping(
              "key." + ApertureAPI.MODID + ".forward",
              KeyConflictContext.IN_GAME,
              InputConstants.Type.KEYSYM,
              GLFW.GLFW_KEY_UNKNOWN,
              "key.categories." + ApertureAPI.MODID));

  public static final Lazy<KeyMapping> MANAGER =
      register(
          new KeyMapping(
              "key." + ApertureAPI.MODID + ".manager",
              KeyConflictContext.IN_GAME,
              InputConstants.Type.KEYSYM,
              GLFW.GLFW_KEY_UNKNOWN,
              "key.categories." + ApertureAPI.MODID));

  public static final Lazy<KeyMapping> CLEAN =
      register(
          new KeyMapping(
              "key." + ApertureAPI.MODID + ".clean",
              KeyConflictContext.IN_GAME,
              InputConstants.Type.KEYSYM,
              GLFW.GLFW_KEY_UNKNOWN,
              "key.categories." + ApertureAPI.MODID));

  public static final Lazy<KeyMapping> NATIVE_CENTER =
      register(
          new KeyMapping(
              "key." + ApertureAPI.MODID + ".native_center",
              KeyConflictContext.IN_GAME,
              InputConstants.Type.KEYSYM,
              GLFW.GLFW_KEY_UNKNOWN,
              "key.categories." + ApertureAPI.MODID));

  public static final Lazy<KeyMapping> REMOVE_NATIVE_CENTER =
      register(
          new KeyMapping(
              "key." + ApertureAPI.MODID + ".remove_native_center",
              KeyConflictContext.IN_GAME,
              InputConstants.Type.KEYSYM,
              GLFW.GLFW_KEY_UNKNOWN,
              "key.categories." + ApertureAPI.MODID));

  // Keyframe editing keybinds
  public static final Lazy<KeyMapping> DUPLICATE_KEYFRAME =
      register(
          new KeyMapping(
              "key." + ApertureAPI.MODID + ".duplicate_keyframe",
              KeyConflictContext.IN_GAME,
              InputConstants.Type.KEYSYM,
              GLFW.GLFW_KEY_D,
              "key.categories." + ApertureAPI.MODID));

  public static final Lazy<KeyMapping> UPDATE_KEYFRAME_FROM_CAMERA =
      register(
          new KeyMapping(
              "key." + ApertureAPI.MODID + ".update_keyframe_from_camera",
              KeyConflictContext.IN_GAME,
              InputConstants.Type.KEYSYM,
              GLFW.GLFW_KEY_U,
              "key.categories." + ApertureAPI.MODID));

  public static final Lazy<KeyMapping> NEXT_KEYFRAME =
      register(
          new KeyMapping(
              "key." + ApertureAPI.MODID + ".next_keyframe",
              KeyConflictContext.IN_GAME,
              InputConstants.Type.KEYSYM,
              GLFW.GLFW_KEY_PAGE_DOWN,
              "key.categories." + ApertureAPI.MODID));

  public static final Lazy<KeyMapping> PREVIOUS_KEYFRAME =
      register(
          new KeyMapping(
              "key." + ApertureAPI.MODID + ".previous_keyframe",
              KeyConflictContext.IN_GAME,
              InputConstants.Type.KEYSYM,
              GLFW.GLFW_KEY_PAGE_UP,
              "key.categories." + ApertureAPI.MODID));

  public static final Lazy<KeyMapping> CYCLE_INTERPOLATION =
      register(
          new KeyMapping(
              "key." + ApertureAPI.MODID + ".cycle_interpolation",
              KeyConflictContext.IN_GAME,
              InputConstants.Type.KEYSYM,
              GLFW.GLFW_KEY_I,
              "key.categories." + ApertureAPI.MODID));

  public static final Lazy<KeyMapping> DECREASE_FOV =
      register(
          new KeyMapping(
              "key." + ApertureAPI.MODID + ".decrease_fov",
              KeyConflictContext.IN_GAME,
              InputConstants.Type.KEYSYM,
              GLFW.GLFW_KEY_MINUS,
              "key.categories." + ApertureAPI.MODID));

  public static final Lazy<KeyMapping> INCREASE_FOV =
      register(
          new KeyMapping(
              "key." + ApertureAPI.MODID + ".increase_fov",
              KeyConflictContext.IN_GAME,
              InputConstants.Type.KEYSYM,
              GLFW.GLFW_KEY_EQUAL,
              "key.categories." + ApertureAPI.MODID));

  public static final Lazy<KeyMapping> INSERT_KEYFRAME_BETWEEN =
      register(
          new KeyMapping(
              "key." + ApertureAPI.MODID + ".insert_keyframe_between",
              KeyConflictContext.IN_GAME,
              InputConstants.Type.KEYSYM,
              GLFW.GLFW_KEY_INSERT,
              "key.categories." + ApertureAPI.MODID));

  private static Lazy<KeyMapping> register(KeyMapping key) {
    Lazy<KeyMapping> lazy = Lazy.of(() -> key);
    list.add(lazy);
    return lazy;
  }

  @SubscribeEvent
  public static void register(RegisterKeyMappingsEvent event) {
    for (Lazy<KeyMapping> key : list) {
      event.register(key.get());
    }

    list = null;
  }
}
