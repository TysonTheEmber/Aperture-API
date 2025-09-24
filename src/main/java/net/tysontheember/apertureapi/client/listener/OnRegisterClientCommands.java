package net.tysontheember.apertureapi.client.listener;

import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tysontheember.apertureapi.ApertureAPI;

/**
 * Registers client-side commands so they can access client-only state (like the current preview path)
 */
@Mod.EventBusSubscriber(modid = ApertureAPI.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class OnRegisterClientCommands {
    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        // No client-only commands registered; all commands use server roots /aperture and /camera.
    }
}
