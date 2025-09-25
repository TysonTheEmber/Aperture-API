package net.tysontheember.apertureapi.camera;

import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tysontheember.apertureapi.ApertureAPI;

/** Hooks server datapack reload to register the camera path loader. */
@Mod.EventBusSubscriber(modid = ApertureAPI.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DataReloadHandlers {
  @SubscribeEvent
  public static void onAddReloadListeners(AddReloadListenerEvent event) {
    event.addListener(new CameraPathRegistry());
  }
}
