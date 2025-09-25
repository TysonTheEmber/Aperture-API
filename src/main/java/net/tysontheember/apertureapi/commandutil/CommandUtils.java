package net.tysontheember.apertureapi.commandutil;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public class CommandUtils {
  private static boolean DEBUG = false;

  public static boolean permCheck(CommandSourceStack s, int level) {
    // Allow command blocks; otherwise require permission
    if (s.source instanceof net.minecraft.world.level.BaseCommandBlock) return true;
    if (s.isPlayer()) {
      return s.getServer().getProfilePermissions(s.getPlayer().getGameProfile()) >= level;
    }
    return false;
  }

  public static void msgInfo(CommandSourceStack s, String msg) {
    s.sendSuccess(() -> Component.literal(msg), false);
  }

  public static void msgWarn(CommandSourceStack s, String msg) {
    s.sendSuccess(() -> Component.literal("[warn] " + msg), false);
  }

  public static void msgError(CommandSourceStack s, String msg) {
    s.sendFailure(Component.literal(msg));
  }

  public static ServerLevel level(CommandSourceStack s) {
    return s.getLevel();
  }

  public static float clamp01(float v) {
    return Math.max(0f, Math.min(1f, v));
  }

  public static boolean isDebug() {
    return DEBUG;
  }

  public static void setDebug(boolean v) {
    DEBUG = v;
  }

  // Version line helper: attempts to include mod version; falls back gracefully
  public static String versionLine() {
    String mod = "Aperture-API";
    String version = "unknown";
    try {
      var opt =
          net.minecraftforge.fml.ModList.get()
              .getModContainerById(net.tysontheember.apertureapi.ApertureAPI.MODID);
      if (opt.isPresent()) {
        version = opt.get().getModInfo().getVersion().toString();
      }
    } catch (Throwable ignored) {
    }
    return mod + " v" + version + ", MC 1.20.1";
  }
}
