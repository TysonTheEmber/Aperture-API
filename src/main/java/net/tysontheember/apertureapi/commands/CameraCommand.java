package net.tysontheember.apertureapi.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.tysontheember.apertureapi.commandutil.CameraPathService;
import net.tysontheember.apertureapi.commandutil.CommandUtils;
import net.tysontheember.apertureapi.common.GlobalCameraSavedData;
import net.tysontheember.apertureapi.common.animation.GlobalCameraPath;
import net.tysontheember.apertureapi.common.network.ServerPayloadSender;

public class CameraCommand {
  private static final SuggestionProvider<CommandSourceStack> PATH_SUGGESTER =
      (ctx, b) -> suggestPathIds(ctx, b);

  public static void register(CommandDispatcher<CommandSourceStack> d, String root) {
    d.register(
        Commands.literal(root)
            .then(Commands.literal("list").executes(CameraCommand::list))
            .then(
                Commands.literal("play")
                    .requires(s -> CommandUtils.permCheck(s, 2))
                    .then(
                        Commands.argument("name", StringArgumentType.word())
                            .suggests(PATH_SUGGESTER)
                            .executes(CameraCommand::play)
                            .then(
                                Commands.argument("speed", FloatArgumentType.floatArg(0.05f, 10f))
                                    .suggests(CameraCommand::suggestSpeeds)
                                    .then(
                                        Commands.argument(
                                                "loop",
                                                com.mojang.brigadier.arguments.BoolArgumentType
                                                    .bool())
                                            .executes(CameraCommand::playWithLoop)
                                            .then(
                                                Commands.argument(
                                                        "auto-reset",
                                                        com.mojang.brigadier.arguments
                                                            .BoolArgumentType.bool())
                                                    .executes(CameraCommand::playWithOptions)
                                                    .then(
                                                        Commands.argument(
                                                                "target",
                                                                net.minecraft.commands.arguments
                                                                    .EntityArgument.player())
                                                            .executes(
                                                                CameraCommand
                                                                    ::playForTargetWithOptions)))))))
            .then(
                Commands.literal("stop")
                    .executes(CameraCommand::stopSelf)
                    .then(
                        Commands.argument(
                                "target", net.minecraft.commands.arguments.EntityArgument.player())
                            .requires(s -> CommandUtils.permCheck(s, 2))
                            .executes(CameraCommand::stopTarget)))
            .then(
                Commands.literal("reset")
                    .executes(CameraCommand::resetSelf)
                    .then(
                        Commands.argument(
                                "target", net.minecraft.commands.arguments.EntityArgument.player())
                            .requires(s -> CommandUtils.permCheck(s, 2))
                            .executes(CameraCommand::resetTarget)))
            .then(
                Commands.literal("interpolation")
                    .requires(s -> CommandUtils.permCheck(s, 2))
                    .then(
                        Commands.argument("name", StringArgumentType.word())
                            .suggests(PATH_SUGGESTER)
                            .then(
                                Commands.argument("mode", StringArgumentType.word())
                                    .suggests(CameraCommand::suggestInterpolations)
                                    .executes(CameraCommand::setInterpolation))))
            .then(
                Commands.literal("export")
                    .requires(s -> CommandUtils.permCheck(s, 2))
                    .then(
                        Commands.argument("name", StringArgumentType.word())
                            .suggests(PATH_SUGGESTER)
                            .then(
                                Commands.argument("file", StringArgumentType.word())
                                    .executes(CameraCommand::exportAs)))));
  }

  private static int list(CommandContext<CommandSourceStack> ctx) {
    ServerLevel level = CommandUtils.level(ctx.getSource());
    List<String> ids = CameraPathService.list(level);
    if (ids.isEmpty()) {
      CommandUtils.msgInfo(ctx.getSource(), "No saved camera paths.");
    } else {
      CommandUtils.msgInfo(ctx.getSource(), "Saved camera paths: " + String.join(", ", ids));
    }
    return 1;
  }

  private static int play(CommandContext<CommandSourceStack> ctx) {
    String name = StringArgumentType.getString(ctx, "name");
    float speed =
        ctx.getInput().contains(" ") && ctx.getInput().split(" ").length >= 4
            ? FloatArgumentType.getFloat(ctx, "speed")
            : 1.0f;
    boolean loop =
        ctx.getInput().contains(" ") && ctx.getInput().split(" ").length >= 5
            ? com.mojang.brigadier.arguments.BoolArgumentType.getBool(ctx, "loop")
            : false;
    ServerPlayer player;
    try {
      player = ctx.getSource().getPlayerOrException();
    } catch (Exception e) {
      CommandUtils.msgError(ctx.getSource(), "Player-only command");
      return 0;
    }
    ServerLevel level = CommandUtils.level(ctx.getSource());
    GlobalCameraSavedData data = GlobalCameraSavedData.getData(level);
    GlobalCameraPath path = data.getPath(name);
    if (path == null) {
      CommandUtils.msgError(ctx.getSource(), "Unknown path: '" + name + "'");
      return 0;
    }
    // speed/loop not yet stored on path; client respects loop based on receiver
    int receiver = loop ? 1 : 2; // 1=loop, 2=one-shot
    ServerPayloadSender.sendGlobalPath(path, player, receiver);
    CommandUtils.msgInfo(
        ctx.getSource(), "Playing '" + name + "' (speed=" + speed + ", loop=" + loop + ")");
    return 1;
  }

  private static int playWithLoop(CommandContext<CommandSourceStack> ctx) {
    String name = StringArgumentType.getString(ctx, "name");
    float speed = FloatArgumentType.getFloat(ctx, "speed");
    boolean loop = com.mojang.brigadier.arguments.BoolArgumentType.getBool(ctx, "loop");
    boolean autoReset = true; // Default auto-reset behavior

    ServerPlayer player;
    try {
      player = ctx.getSource().getPlayerOrException();
    } catch (Exception e) {
      CommandUtils.msgError(ctx.getSource(), "Player-only command");
      return 0;
    }
    ServerLevel level = CommandUtils.level(ctx.getSource());
    GlobalCameraSavedData data = GlobalCameraSavedData.getData(level);
    GlobalCameraPath path = data.getPath(name);
    if (path == null) {
      CommandUtils.msgError(ctx.getSource(), "Unknown path: '" + name + "'");
      return 0;
    }

    int receiver = calculateReceiver(loop, autoReset);
    ServerPayloadSender.sendGlobalPath(path, player, receiver);
    CommandUtils.msgInfo(
        ctx.getSource(),
        "Playing '"
            + name
            + "' (speed="
            + speed
            + ", loop="
            + loop
            + ", auto-reset="
            + autoReset
            + ")");
    return 1;
  }

  private static int playWithOptions(CommandContext<CommandSourceStack> ctx) {
    String name = StringArgumentType.getString(ctx, "name");
    float speed = FloatArgumentType.getFloat(ctx, "speed");
    boolean loop = com.mojang.brigadier.arguments.BoolArgumentType.getBool(ctx, "loop");
    boolean autoReset = com.mojang.brigadier.arguments.BoolArgumentType.getBool(ctx, "auto-reset");

    ServerPlayer player;
    try {
      player = ctx.getSource().getPlayerOrException();
    } catch (Exception e) {
      CommandUtils.msgError(ctx.getSource(), "Player-only command");
      return 0;
    }
    ServerLevel level = CommandUtils.level(ctx.getSource());
    GlobalCameraSavedData data = GlobalCameraSavedData.getData(level);
    GlobalCameraPath path = data.getPath(name);
    if (path == null) {
      CommandUtils.msgError(ctx.getSource(), "Unknown path: '" + name + "'");
      return 0;
    }

    int receiver = calculateReceiver(loop, autoReset);
    ServerPayloadSender.sendGlobalPath(path, player, receiver);
    CommandUtils.msgInfo(
        ctx.getSource(),
        "Playing '"
            + name
            + "' (speed="
            + speed
            + ", loop="
            + loop
            + ", auto-reset="
            + autoReset
            + ")");
    return 1;
  }

  private static int playForTarget(CommandContext<CommandSourceStack> ctx)
      throws com.mojang.brigadier.exceptions.CommandSyntaxException {
    String name = StringArgumentType.getString(ctx, "name");
    float speed = FloatArgumentType.getFloat(ctx, "speed");
    boolean loop = com.mojang.brigadier.arguments.BoolArgumentType.getBool(ctx, "loop");
    boolean autoReset = true; // Default auto-reset behavior
    var target = net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "target");
    ServerLevel level = CommandUtils.level(ctx.getSource());
    GlobalCameraSavedData data = GlobalCameraSavedData.getData(level);
    GlobalCameraPath path = data.getPath(name);
    if (path == null) {
      CommandUtils.msgError(ctx.getSource(), "Unknown path: '" + name + "'");
      return 0;
    }

    int receiver = calculateReceiver(loop, autoReset);
    ServerPayloadSender.sendGlobalPath(path, target, receiver);
    CommandUtils.msgInfo(
        ctx.getSource(),
        "Playing '"
            + name
            + "' for "
            + target.getGameProfile().getName()
            + " at speed="
            + speed
            + " loop="
            + loop
            + " auto-reset="
            + autoReset);
    return 1;
  }

  private static int playForTargetWithOptions(CommandContext<CommandSourceStack> ctx)
      throws com.mojang.brigadier.exceptions.CommandSyntaxException {
    String name = StringArgumentType.getString(ctx, "name");
    float speed = FloatArgumentType.getFloat(ctx, "speed");
    boolean loop = com.mojang.brigadier.arguments.BoolArgumentType.getBool(ctx, "loop");
    boolean autoReset = com.mojang.brigadier.arguments.BoolArgumentType.getBool(ctx, "auto-reset");
    var target = net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "target");
    ServerLevel level = CommandUtils.level(ctx.getSource());
    GlobalCameraSavedData data = GlobalCameraSavedData.getData(level);
    GlobalCameraPath path = data.getPath(name);
    if (path == null) {
      CommandUtils.msgError(ctx.getSource(), "Unknown path: '" + name + "'");
      return 0;
    }

    int receiver = calculateReceiver(loop, autoReset);
    ServerPayloadSender.sendGlobalPath(path, target, receiver);
    CommandUtils.msgInfo(
        ctx.getSource(),
        "Playing '"
            + name
            + "' for "
            + target.getGameProfile().getName()
            + " at speed="
            + speed
            + " loop="
            + loop
            + " auto-reset="
            + autoReset);
    return 1;
  }

  private static int calculateReceiver(boolean loop, boolean autoReset) {
    // receiver codes:
    // 1=loop with auto-reset, 2=one-shot with auto-reset
    // 3=loop without auto-reset, 4=one-shot without auto-reset
    if (autoReset) {
      return loop ? 1 : 2;
    } else {
      return loop ? 3 : 4;
    }
  }

  private static java.util.concurrent.CompletableFuture<Suggestions> suggestSpeeds(
      CommandContext<CommandSourceStack> ctx, SuggestionsBuilder b) {
    for (String s : new String[] {"0.5", "1.0", "2.0"}) {
      if (s.startsWith(b.getRemaining())) b.suggest(s);
    }
    return b.buildFuture();
  }

  private static CompletableFuture<Suggestions> suggestInterpolations(
      CommandContext<CommandSourceStack> ctx, SuggestionsBuilder b) {
    String[] modes =
        new String[] {
          "linear",
          "smooth",
          "bezier",
          "step",
          "cosine",
          "catmull:uniform",
          "catmull:centripetal",
          "catmull:chordal"
        };
    for (String s : modes) if (s.startsWith(b.getRemaining())) b.suggest(s);
    return b.buildFuture();
  }

  private static int stopSelf(CommandContext<CommandSourceStack> ctx) {
    try {
      var player = ctx.getSource().getPlayerOrException();
      net.tysontheember.apertureapi.common.network.ServerPayloadSender.send(
          "stopCamera", new net.minecraft.nbt.CompoundTag(), player);
      CommandUtils.msgInfo(
          ctx.getSource(), "Stopped camera for " + player.getGameProfile().getName());
      return 1;
    } catch (Exception e) {
      CommandUtils.msgError(ctx.getSource(), "Player-only command");
      return 0;
    }
  }

  private static int stopTarget(CommandContext<CommandSourceStack> ctx) {
    try {
      var target = net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "target");
      net.tysontheember.apertureapi.common.network.ServerPayloadSender.send(
          "stopCamera", new net.minecraft.nbt.CompoundTag(), target);
      CommandUtils.msgInfo(
          ctx.getSource(), "Stopped camera for " + target.getGameProfile().getName());
      return 1;
    } catch (Exception e) {
      CommandUtils.msgError(ctx.getSource(), "Failed to stop for target: " + e.getMessage());
      return 0;
    }
  }

  private static int resetSelf(CommandContext<CommandSourceStack> ctx) {
    try {
      var player = ctx.getSource().getPlayerOrException();
      net.minecraft.nbt.CompoundTag resetTag = new net.minecraft.nbt.CompoundTag();
      resetTag.putBoolean("force_reset", true);
      net.tysontheember.apertureapi.common.network.ServerPayloadSender.send(
          "resetCamera", resetTag, player);
      CommandUtils.msgInfo(
          ctx.getSource(), "Reset camera for " + player.getGameProfile().getName());
      return 1;
    } catch (Exception e) {
      CommandUtils.msgError(ctx.getSource(), "Player-only command");
      return 0;
    }
  }

  private static int resetTarget(CommandContext<CommandSourceStack> ctx) {
    try {
      var target = net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "target");
      net.minecraft.nbt.CompoundTag resetTag = new net.minecraft.nbt.CompoundTag();
      resetTag.putBoolean("force_reset", true);
      net.tysontheember.apertureapi.common.network.ServerPayloadSender.send(
          "resetCamera", resetTag, target);
      CommandUtils.msgInfo(
          ctx.getSource(), "Reset camera for " + target.getGameProfile().getName());
      return 1;
    } catch (Exception e) {
      CommandUtils.msgError(ctx.getSource(), "Failed to reset for target: " + e.getMessage());
      return 0;
    }
  }

  private static int setInterpolation(CommandContext<CommandSourceStack> ctx) {
    String name = StringArgumentType.getString(ctx, "name");
    String mode = StringArgumentType.getString(ctx, "mode");
    ServerLevel level = CommandUtils.level(ctx.getSource());
    GlobalCameraSavedData data = GlobalCameraSavedData.getData(level);
    GlobalCameraPath path = data.getPath(name);
    if (path == null) {
      CommandUtils.msgError(ctx.getSource(), "Unknown path: '" + name + "'");
      return 0;
    }
    net.tysontheember.apertureapi.common.animation.PathInterpolator interp =
        switch (mode.toLowerCase()) {
          case "linear" -> net.tysontheember.apertureapi.common.animation.PathInterpolator.LINEAR;
          case "smooth" -> net.tysontheember.apertureapi.common.animation.PathInterpolator.SMOOTH;
          case "bezier" -> net.tysontheember.apertureapi.common.animation.PathInterpolator.BEZIER;
          case "step" -> net.tysontheember.apertureapi.common.animation.PathInterpolator.STEP;
          case "cosine" -> net.tysontheember.apertureapi.common.animation.PathInterpolator.COSINE;
          case "catmull:uniform" -> net.tysontheember.apertureapi.common.animation.PathInterpolator
              .CATMULL_UNIFORM;
          case "catmull:centripetal" -> net.tysontheember.apertureapi.common.animation
              .PathInterpolator.CATMULL_CENTRIPETAL;
          case "catmull:chordal" -> net.tysontheember.apertureapi.common.animation.PathInterpolator
              .CATMULL_CHORDAL;
          default -> null;
        };
    if (interp == null) {
      CommandUtils.msgError(ctx.getSource(), "Unknown interpolation mode: '" + mode + "'");
      return 0;
    }
    for (var e : path.getEntries()) {
      e.getValue().setPathInterpolator(interp);
      path.updateBezier(e.getIntKey());
    }
    CommandUtils.msgInfo(ctx.getSource(), "Set interpolation for '" + name + "' to " + mode);
    return 1;
  }

  private static int exportAs(CommandContext<CommandSourceStack> ctx) {
    String name = StringArgumentType.getString(ctx, "name");
    String file = StringArgumentType.getString(ctx, "file");
    ServerLevel level = CommandUtils.level(ctx.getSource());
    var res = CameraPathService.exportAs(level, name, file);
    if (res.success()) {
      CommandUtils.msgInfo(ctx.getSource(), "Exported '" + name + "' → " + res.path());
      return 1;
    } else {
      CommandUtils.msgError(ctx.getSource(), res.message());
      return 0;
    }
  }

  private static CompletableFuture<Suggestions> suggestPathIds(
      CommandContext<CommandSourceStack> ctx, SuggestionsBuilder b) {
    ServerLevel level = CommandUtils.level(ctx.getSource());
    for (String id : CameraPathService.list(level)) {
      if (id.startsWith(b.getRemaining())) b.suggest(id);
    }
    return b.buildFuture();
  }
}
