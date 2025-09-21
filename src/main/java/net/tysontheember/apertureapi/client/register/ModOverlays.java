package net.tysontheember.apertureapi.client.register;

import net.tysontheember.apertureapi.ApertureAPI;
import net.tysontheember.apertureapi.client.gui.overlay.ModifyModeOverlay;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApertureAPI.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModOverlays {
    @SubscribeEvent
    public static void register(RegisterGuiOverlaysEvent event) {
        event.registerBelow(VanillaGuiOverlay.HOTBAR.id(), "modify_mode_overlay", new ModifyModeOverlay());
    }
}

