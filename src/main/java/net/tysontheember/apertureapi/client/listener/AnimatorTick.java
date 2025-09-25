package net.tysontheember.apertureapi.client.listener;

import static net.tysontheember.apertureapi.client.CameraAnimIdeCache.getPath;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tysontheember.apertureapi.ApertureAPI;
import net.tysontheember.apertureapi.client.Animator;
import net.tysontheember.apertureapi.client.CameraAnimIdeCache;
import net.tysontheember.apertureapi.client.ClientUtil;
import net.tysontheember.apertureapi.client.PreviewAnimator;

@Mod.EventBusSubscriber(modid = ApertureAPI.MODID, value = Dist.CLIENT)
public class AnimatorTick {
  @SubscribeEvent
  public static void tick(TickEvent.ClientTickEvent event) {
    if (event.phase != TickEvent.Phase.END || ClientUtil.gamePaused()) {
      return;
    }

    if (Animator.INSTANCE.isPlaying()) {
      Animator.INSTANCE.tick();
    } else if ((CameraAnimIdeCache.VIEW || CameraAnimIdeCache.EDIT)
        && !getPath().getPoints().isEmpty()
        && !ClientUtil.gamePaused()) {
      PreviewAnimator.INSTANCE.tick();
    }
  }
}
