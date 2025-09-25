package net.tysontheember.apertureapi.client.listener;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tysontheember.apertureapi.ApertureAPI;
import net.tysontheember.apertureapi.client.CameraAnimIdeCache;

@Mod.EventBusSubscriber(modid = ApertureAPI.MODID, value = Dist.CLIENT)
public class HideHandOnPreview {
  @SubscribeEvent
  public static void onRenderHand(RenderHandEvent event) {
    if (CameraAnimIdeCache.PREVIEW) {
      event.setCanceled(true);
    }
  }
}
