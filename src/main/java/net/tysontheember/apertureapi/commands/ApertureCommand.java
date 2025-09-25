package net.tysontheember.apertureapi.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.tysontheember.apertureapi.commandutil.CommandUtils;

public class ApertureCommand {
  public static void register(CommandDispatcher<CommandSourceStack> d, String root) {
    d.register(
        Commands.literal(root)
            .requires(s -> CommandUtils.permCheck(s, 2))
            .then(
                Commands.literal("help")
                    .executes(
                        ctx -> {
                          CommandUtils.msgInfo(
                              ctx.getSource(),
                              "Usage: /aperture (help|version|api)  |  /camera (list|play|stop|export)");
                          return 1;
                        }))
            .then(
                Commands.literal("version")
                    .executes(
                        ctx -> {
                          CommandUtils.msgInfo(ctx.getSource(), CommandUtils.versionLine());
                          return 1;
                        }))
            .then(
                Commands.literal("api")
                    .executes(
                        ctx -> {
                          CommandUtils.msgInfo(
                              ctx.getSource(),
                              "API level: 1.0.0; protocol: 1; features: JSON_EXPORT, PATH_PLAYER_BINDING");
                          return 1;
                        })));
  }
}
