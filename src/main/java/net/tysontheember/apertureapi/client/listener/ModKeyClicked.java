package net.tysontheember.apertureapi.client.listener;

import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tysontheember.apertureapi.ApertureAPI;
import net.tysontheember.apertureapi.client.CameraAnimIdeCache;
import net.tysontheember.apertureapi.client.ClientUtil;
import net.tysontheember.apertureapi.client.PreviewAnimator;
import net.tysontheember.apertureapi.client.gui.screen.PointSettingScreen;
import net.tysontheember.apertureapi.client.gui.screen.RemotePathSearchScreen;
import net.tysontheember.apertureapi.client.register.ModKeyMapping;
import net.tysontheember.apertureapi.common.animation.CameraKeyframe;
import net.tysontheember.apertureapi.common.animation.GlobalCameraPath;
import net.tysontheember.apertureapi.common.animation.PathInterpolator;
import org.joml.Vector3f;

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
      CameraAnimIdeCache.getPath()
          .add(
              new CameraKeyframe(
                  camera.getPosition().toVector3f(),
                  new Vector3f(xRot, yRot, 0),
                  mc.options.fov().get(),
                  PathInterpolator.LINEAR));
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

      CameraAnimIdeCache.setNative(
          ClientUtil.player().position().toVector3f(),
          new Vector3f(0, Mth.wrapDegrees(ClientUtil.playerYHeadRot()), 0));
    }

    while (ModKeyMapping.REMOVE_NATIVE_CENTER.get().consumeClick()) {
      if (!CameraAnimIdeCache.EDIT) {
        continue;
      }

      CameraAnimIdeCache.getPath().setNativeMode(false);
    }

    // Keyframe editing functionality
    while (ModKeyMapping.DUPLICATE_KEYFRAME.get().consumeClick()) {
      if (!CameraAnimIdeCache.EDIT || CameraAnimIdeCache.getSelectedPoint().getPointTime() < 0) {
        continue;
      }

      CameraAnimIdeCache.SelectedPoint selectedPoint = CameraAnimIdeCache.getSelectedPoint();
      int selectedTime = selectedPoint.getPointTime();
      GlobalCameraPath path = CameraAnimIdeCache.getPath();
      CameraKeyframe originalKeyframe = path.getPoint(selectedTime);

      if (originalKeyframe != null) {
        // Find a new time slot (increment until we find an unused time)
        int newTime = selectedTime + 1;
        while (path.getPoint(newTime) != null) {
          newTime++;
        }

        // Create a copy of the keyframe
        CameraKeyframe duplicateKeyframe = originalKeyframe.copy();
        path.add(newTime, duplicateKeyframe);

        // Select the new duplicated keyframe
        selectedPoint.setSelected(newTime);
      }
    }

    while (ModKeyMapping.UPDATE_KEYFRAME_FROM_CAMERA.get().consumeClick()) {
      if (!CameraAnimIdeCache.EDIT || CameraAnimIdeCache.getSelectedPoint().getPointTime() < 0) {
        continue;
      }

      CameraAnimIdeCache.SelectedPoint selectedPoint = CameraAnimIdeCache.getSelectedPoint();
      int selectedTime = selectedPoint.getPointTime();
      GlobalCameraPath path = CameraAnimIdeCache.getPath();
      CameraKeyframe keyframe = path.getPoint(selectedTime);

      if (keyframe != null) {
        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();

        // Update position and rotation from current camera
        keyframe.setPos(
            (float) camera.getPosition().x,
            (float) camera.getPosition().y,
            (float) camera.getPosition().z);
        keyframe
            .getRot()
            .set(
                Mth.wrapDegrees(camera.getXRot()),
                Mth.wrapDegrees(camera.getYRot()),
                0 // Roll remains unchanged
                );
        keyframe.setFov(mc.options.fov().get());
      }
    }

    while (ModKeyMapping.NEXT_KEYFRAME.get().consumeClick()) {
      if (!CameraAnimIdeCache.EDIT) {
        continue;
      }

      CameraAnimIdeCache.SelectedPoint selectedPoint = CameraAnimIdeCache.getSelectedPoint();
      GlobalCameraPath path = CameraAnimIdeCache.getPath();

      if (selectedPoint.getPointTime() < 0) {
        // No keyframe selected, select the first one
        Map.Entry<Integer, CameraKeyframe> first = path.getNextEntry(Integer.MIN_VALUE);
        if (first != null) {
          selectedPoint.setSelected(first.getKey());
        }
      } else {
        // Select next keyframe
        Map.Entry<Integer, CameraKeyframe> next = path.getNextEntry(selectedPoint.getPointTime());
        if (next != null) {
          selectedPoint.setSelected(next.getKey());
        }
      }
    }

    while (ModKeyMapping.PREVIOUS_KEYFRAME.get().consumeClick()) {
      if (!CameraAnimIdeCache.EDIT) {
        continue;
      }

      CameraAnimIdeCache.SelectedPoint selectedPoint = CameraAnimIdeCache.getSelectedPoint();
      GlobalCameraPath path = CameraAnimIdeCache.getPath();

      if (selectedPoint.getPointTime() < 0) {
        // No keyframe selected, select the last one
        Map.Entry<Integer, CameraKeyframe> last = path.getPreEntry(Integer.MAX_VALUE);
        if (last != null) {
          selectedPoint.setSelected(last.getKey());
        }
      } else {
        // Select previous keyframe
        Map.Entry<Integer, CameraKeyframe> prev = path.getPreEntry(selectedPoint.getPointTime());
        if (prev != null) {
          selectedPoint.setSelected(prev.getKey());
        }
      }
    }

    while (ModKeyMapping.CYCLE_INTERPOLATION.get().consumeClick()) {
      if (!CameraAnimIdeCache.EDIT || CameraAnimIdeCache.getSelectedPoint().getPointTime() < 0) {
        continue;
      }

      CameraAnimIdeCache.SelectedPoint selectedPoint = CameraAnimIdeCache.getSelectedPoint();
      int selectedTime = selectedPoint.getPointTime();
      GlobalCameraPath path = CameraAnimIdeCache.getPath();
      CameraKeyframe keyframe = path.getPoint(selectedTime);

      if (keyframe != null) {
        // Cycle through PathInterpolator values
        PathInterpolator[] interpolators = PathInterpolator.values();
        PathInterpolator current = keyframe.getPathInterpolator();

        int currentIndex = current.index;
        int nextIndex = (currentIndex + 1) % interpolators.length;

        PathInterpolator next = PathInterpolator.fromIndex(nextIndex);
        keyframe.setPathInterpolator(next);

        // Update bezier data for the new interpolation mode
        path.updateBezier(selectedTime);
      }
    }

    while (ModKeyMapping.DECREASE_FOV.get().consumeClick()) {
      if (!CameraAnimIdeCache.EDIT || CameraAnimIdeCache.getSelectedPoint().getPointTime() < 0) {
        continue;
      }

      CameraAnimIdeCache.SelectedPoint selectedPoint = CameraAnimIdeCache.getSelectedPoint();
      int selectedTime = selectedPoint.getPointTime();
      GlobalCameraPath path = CameraAnimIdeCache.getPath();
      CameraKeyframe keyframe = path.getPoint(selectedTime);

      if (keyframe != null) {
        float currentFov = keyframe.getFov();
        float newFov = Math.max(1.0f, currentFov - 5.0f); // Decrease by 5, minimum 1
        keyframe.setFov(newFov);
      }
    }

    while (ModKeyMapping.INCREASE_FOV.get().consumeClick()) {
      if (!CameraAnimIdeCache.EDIT || CameraAnimIdeCache.getSelectedPoint().getPointTime() < 0) {
        continue;
      }

      CameraAnimIdeCache.SelectedPoint selectedPoint = CameraAnimIdeCache.getSelectedPoint();
      int selectedTime = selectedPoint.getPointTime();
      GlobalCameraPath path = CameraAnimIdeCache.getPath();
      CameraKeyframe keyframe = path.getPoint(selectedTime);

      if (keyframe != null) {
        float currentFov = keyframe.getFov();
        float newFov = Math.min(179.0f, currentFov + 5.0f); // Increase by 5, maximum 179
        keyframe.setFov(newFov);
      }
    }

    while (ModKeyMapping.INSERT_KEYFRAME_BETWEEN.get().consumeClick()) {
      if (!CameraAnimIdeCache.EDIT || CameraAnimIdeCache.getSelectedPoint().getPointTime() < 0) {
        continue;
      }

      CameraAnimIdeCache.SelectedPoint selectedPoint = CameraAnimIdeCache.getSelectedPoint();
      int selectedTime = selectedPoint.getPointTime();
      GlobalCameraPath path = CameraAnimIdeCache.getPath();

      Map.Entry<Integer, CameraKeyframe> current = path.getEntry(selectedTime);
      Map.Entry<Integer, CameraKeyframe> next = path.getNextEntry(selectedTime);

      if (current != null && next != null) {
        int insertTime = (selectedTime + next.getKey()) / 2; // Insert at midpoint

        // Ensure we don't overwrite an existing keyframe
        while (path.getPoint(insertTime) != null) {
          insertTime++;
        }

        // Interpolate position between current and next keyframes
        Vector3f currentPos = current.getValue().getPos();
        Vector3f nextPos = next.getValue().getPos();
        Vector3f newPos =
            new Vector3f(
                (currentPos.x + nextPos.x) / 2,
                (currentPos.y + nextPos.y) / 2,
                (currentPos.z + nextPos.z) / 2);

        // Interpolate rotation
        Vector3f currentRot = current.getValue().getRot();
        Vector3f nextRot = next.getValue().getRot();
        Vector3f newRot =
            new Vector3f(
                (currentRot.x + nextRot.x) / 2,
                Mth.wrapDegrees((currentRot.y + nextRot.y) / 2), // Handle yaw wrapping
                (currentRot.z + nextRot.z) / 2);

        // Interpolate FOV
        float currentFov = current.getValue().getFov();
        float nextFov = next.getValue().getFov();
        float newFov = (currentFov + nextFov) / 2;

        // Create new keyframe with the interpolated values
        CameraKeyframe newKeyframe =
            new CameraKeyframe(newPos, newRot, newFov, PathInterpolator.LINEAR);

        path.add(insertTime, newKeyframe);

        // Select the newly inserted keyframe
        selectedPoint.setSelected(insertTime);
      }
    }
  }
}
