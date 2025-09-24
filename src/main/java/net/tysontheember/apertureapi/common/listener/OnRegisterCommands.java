package net.tysontheember.apertureapi.common.listener;

import net.tysontheember.apertureapi.ApertureAPI;
import net.tysontheember.apertureapi.common.GlobalCameraSavedData;
import net.tysontheember.apertureapi.common.animation.GlobalCameraPath;
import net.tysontheember.apertureapi.common.network.ServerPayloadSender;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = ApertureAPI.MODID)
public class OnRegisterCommands {
private static final Component PLAY_ANIM_FAILURE = Component.translatable("commands.apertureapi.play.failure");
    private static final Component PLAY_NATIVE_ANIM_FAILURE = Component.translatable("commands.apertureapi.play.native.failure");

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        // Existing cameraanim commands
        dispatcher.register(
                Commands.literal("cameraanim")
                        .then(Commands.literal("play")
                                .requires(source -> {
                                    if (source.source instanceof BaseCommandBlock) {
                                        return true;
                                    } else if (source.isPlayer()) {
                                        return source.getServer().getProfilePermissions(source.getPlayer().getGameProfile()) > 1;
                                    } else {
                                        return false;
                                    }
                                })
                                .then(Commands.argument("Target", EntityArgument.players())
                                        .then(Commands.argument("Anim Id", StringArgumentType.string())
                                                .executes(context -> {
                                                    EntitySelector target = context.getArgument("Target", EntitySelector.class);
                                                    List<ServerPlayer> players = target.findPlayers(context.getSource());

                                                    if (players.isEmpty()) {
                                                        return 1;
                                                    }

                                                    ServerPlayer player = players.get(0);
                                                    String animId = context.getArgument("Anim Id", String.class);
                                                    GlobalCameraSavedData data = GlobalCameraSavedData.getData((ServerLevel) player.level());
                                                    GlobalCameraPath path = data.getPath(animId);

                                                    if (path == null) {
                                                        context.getSource().sendFailure(PLAY_ANIM_FAILURE.copy().append(animId));
                                                    }

                                                    for (ServerPlayer serverPlayer : players) {
                                                        ServerPayloadSender.sendGlobalPath(path, serverPlayer, 1);
                                                    }

                                                    return 1;
                                                })
                                                .then(Commands.argument("Center", EntityArgument.entity())
                                                        .executes(context -> {
                                                            EntitySelector target = context.getArgument("Target", EntitySelector.class);
                                                            List<ServerPlayer> players = target.findPlayers(context.getSource());

                                                            if (players.isEmpty()) {
                                                                return 1;
                                                            }

                                                            ServerPlayer player = players.get(0);
                                                            String animId = context.getArgument("Anim Id", String.class);
                                                            GlobalCameraSavedData data = GlobalCameraSavedData.getData((ServerLevel) player.level());
                                                            GlobalCameraPath path = data.getPath(animId);

                                                            if (path == null) {
                                                                context.getSource().sendFailure(PLAY_ANIM_FAILURE.copy().append(animId));
                                                                return 1;
                                                            } else if (!path.isNativeMode()) {
                                                                context.getSource().sendFailure(PLAY_NATIVE_ANIM_FAILURE.copy().append(animId));
                                                                return 1;
                                                            }

                                                            EntitySelector center = context.getArgument("Center", EntitySelector.class);
                                                            Entity centerEntity = center.findSingleEntity(context.getSource());

                                                            for (ServerPlayer serverPlayer : players) {
                                                                ServerPayloadSender.sendNativePath(path, serverPlayer, centerEntity);
                                                            }

                                                            return 1;
                                                        }))
                                        ))
                        )

        );
        
        // Register smoothing/debug commands and path export under '/aperture ...' with '/camera' alias
        net.tysontheember.apertureapi.common.command.SmoothingCommand.register(dispatcher);
        net.tysontheember.apertureapi.common.command.PathExportCommand.register(dispatcher);
        net.tysontheember.apertureapi.common.command.DemoCameraCommand.register(dispatcher);
    }
}


