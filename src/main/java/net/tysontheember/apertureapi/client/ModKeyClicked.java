package net.tysontheember.apertureapi.client;

import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tysontheember.apertureapi.ApertureAPI;

@Mod.EventBusSubscriber(modid = ApertureAPI.MODID, value = Dist.CLIENT)
public class ModKeyClicked {
  public static Consumer<Void> GUI_OPENER =
      (a) -> Minecraft.getInstance().setScreen(new CameraModifierScreen());

  @SubscribeEvent
  public static void keyClick(TickEvent.ClientTickEvent event) {
    if (event.phase != TickEvent.Phase.END) {
      return;
    }

    while (ModKeyMapping.CAMERA_MODIFIER_SCREEN_KEY.get().consumeClick()) {
      GUI_OPENER.accept(null);
    }
  }
}
