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
        // Central command registry: only '/aperture' and '/camera' roots
        net.tysontheember.apertureapi.commands.CommandRegistry.register(dispatcher);

        // Note: legacy roots like '/cameraanim' and '/apertureapi' are no longer registered here.
    }
}


