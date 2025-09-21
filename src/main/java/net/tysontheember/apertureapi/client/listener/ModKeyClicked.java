package net.tysontheember.apertureapi.client.listener;

import net.tysontheember.apertureapi.ApertureAPI;
import net.tysontheember.apertureapi.client.CameraAnimIdeCache;
import net.tysontheember.apertureapi.client.PreviewAnimator;
import net.tysontheember.apertureapi.client.ClientUtil;
import net.tysontheember.apertureapi.client.register.ModKeyMapping;
import net.tysontheember.apertureapi.client.gui.screen.RemotePathSearchScreen;
import net.tysontheember.apertureapi.client.gui.screen.PointSettingScreen;
import net.tysontheember.apertureapi.common.animation.CameraKeyframe;
import net.tysontheember.apertureapi.common.animation.GlobalCameraPath;
import net.tysontheember.apertureapi.common.animation.PathInterpolator;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.Map;

@Mod.EventBusSubscriber(modid = ApertureAPI.MODID, value = Dist.CLIENT)
public class ModKeyClicked {
    @SubscribeEvent
    public static void keyClick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        while (ModKeyMapping.ADD_GLOBAL_CAMERA_POINT.get().consumeClick()) {
            if (!CameraAnimIdeCache.EDIT) {
                continue;
            }

            Minecraft mc = Minecraft.getInstance();
            Camera camera = mc.gameRenderer.getMainCamera();
            float yRot = Mth.wrapDegrees(camera.getYRot());
            float xRot = Mth.wrapDegrees(camera.getXRot());
            CameraAnimIdeCache.getPath().add(new CameraKeyframe(camera.getPosition().toVector3f(), new Vector3f(xRot, yRot, 0), mc.options.fov().get(), PathInterpolator.LINEAR));
        }

        while (ModKeyMapping.DELETE_GLOBAL_CAMERA_POINT.get().consumeClick()) {
            if (!CameraAnimIdeCache.EDIT) {
                continue;
            }

            CameraAnimIdeCache.SelectedPoint selectedPoint = CameraAnimIdeCache.getSelectedPoint();
            int time = selectedPoint.getPointTime();
            GlobalCameraPath track = CameraAnimIdeCache.getPath();
            track.remove(time);
            Map.Entry<Integer, CameraKeyframe> pre = track.getPreEntry(time);

            if (pre == null) {
                selectedPoint.reset();
            } else {
                selectedPoint.setSelected(pre.getKey());
            }
        }

        while (ModKeyMapping.EDIT_MODE.get().consumeClick()) {
            if (ClientUtil.player().isCreative()) {
                CameraAnimIdeCache.EDIT = !CameraAnimIdeCache.EDIT;
            }
        }

        while (ModKeyMapping.VIEW_MODE.get().consumeClick()) {
            if (ClientUtil.player().isCreative()) {
                CameraAnimIdeCache.VIEW = !CameraAnimIdeCache.VIEW;
            }
        }

        while (ModKeyMapping.ROTATE_MODE.get().consumeClick()) {
            if (!CameraAnimIdeCache.EDIT) {
                continue;
            }
            CameraAnimIdeCache.toggleMode();
        }

        while (ModKeyMapping.POINT_SETTING.get().consumeClick()) {
            if (!CameraAnimIdeCache.EDIT || CameraAnimIdeCache.getSelectedPoint().getPointTime() < 0) {
                continue;
            }

            Minecraft.getInstance().setScreen(new PointSettingScreen());
        }

        while (ModKeyMapping.PREVIEW_MODE.get().consumeClick()) {
            if (!CameraAnimIdeCache.EDIT) {
                continue;
            }

            CameraAnimIdeCache.PREVIEW = !CameraAnimIdeCache.PREVIEW;
        }

        while (ModKeyMapping.PLAY.get().consumeClick()) {
            if (!CameraAnimIdeCache.EDIT) {
                continue;
            }

            if (PreviewAnimator.INSTANCE.isPlaying()) {
                PreviewAnimator.INSTANCE.stop();
            } else {
                PreviewAnimator.INSTANCE.play();
            }
        }

        while (ModKeyMapping.RESET.get().consumeClick()) {
            if (!CameraAnimIdeCache.EDIT) {
                continue;
            }

            PreviewAnimator.INSTANCE.reset();
        }

        while (ModKeyMapping.SET_CAMERA_TIME.get().consumeClick()) {
            if (!CameraAnimIdeCache.EDIT || CameraAnimIdeCache.getSelectedPoint().getPointTime() < 0) {
                continue;
            }

            PreviewAnimator.INSTANCE.setTime(CameraAnimIdeCache.getSelectedPoint().getPointTime());
        }

        if (ModKeyMapping.BACK.get().isDown() && CameraAnimIdeCache.EDIT) {
            PreviewAnimator.INSTANCE.back();
        }

        if (ModKeyMapping.FORWARD.get().isDown() && CameraAnimIdeCache.EDIT) {
            PreviewAnimator.INSTANCE.forward();
        }

        while (ModKeyMapping.MANAGER.get().consumeClick()) {
            if (!ClientUtil.player().isCreative()) {
                return;
            }

            Minecraft.getInstance().setScreen(new RemotePathSearchScreen());
        }

        while (ModKeyMapping.CLEAN.get().consumeClick()) {
            if (!CameraAnimIdeCache.EDIT) {
                return;
            }

            CameraAnimIdeCache.setPath(new GlobalCameraPath("id"));
        }

        while (ModKeyMapping.NATIVE_CENTER.get().consumeClick()) {
            if (!CameraAnimIdeCache.EDIT) {
                continue;
            }

            CameraAnimIdeCache.setNative(ClientUtil.player().position().toVector3f(), new Vector3f(0, Mth.wrapDegrees(ClientUtil.playerYHeadRot()), 0));
        }

        while (ModKeyMapping.REMOVE_NATIVE_CENTER.get().consumeClick()) {
            if (!CameraAnimIdeCache.EDIT) {
                continue;
            }

            CameraAnimIdeCache.getPath().setNativeMode(false);
        }
    }
}

