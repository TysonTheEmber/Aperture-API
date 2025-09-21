package net.tysontheember.apertureapi.common;

import net.tysontheember.apertureapi.ApertureAPI;
import net.tysontheember.apertureapi.CameraModifierManager;
import net.tysontheember.apertureapi.ICameraModifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApertureAPI.MODID, value = Dist.CLIENT)
public class TestListener {
    private static final ICameraModifier modifier = CameraModifierManager
            .createModifier("test", true);

    @SubscribeEvent
    public static void on(ViewportEvent.ComputeFov event) {
        int i = 10;
    }
}

