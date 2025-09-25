package net.tysontheember.apertureapi.client;

import static net.tysontheember.apertureapi.InterpolationMath.catmullRom;
import static net.tysontheember.apertureapi.InterpolationMath.line;
import static net.tysontheember.apertureapi.client.ClientUtil.partialTicks;

import java.util.Map;
import net.minecraft.util.Mth;
import net.tysontheember.apertureapi.common.animation.CameraKeyframe;
import net.tysontheember.apertureapi.common.animation.GlobalCameraPath;
import net.tysontheember.apertureapi.common.animation.TimeInterpolator;
import org.joml.Matrix3f;
import org.joml.Vector3f;

public class Animator {
  public static final Animator INSTANCE = new Animator();
  private GlobalCameraPath path;
  private boolean playing;
  private boolean loop = true;
  private boolean autoReset =
      true; // automatically reset camera when done playing (even for loops when stopped)
  private int time;
  private boolean exiting; // true while we are fading out before returning control

  private final Vector3f center = new Vector3f();
  private final Vector3f rotation = new Vector3f();
  private final Matrix3f rotationMatrix = new Matrix3f();

  // Constant-speed reparameterization (duration-based) and quaternion orientation flags
  private boolean constantSpeed = true;
  private boolean quaternionOrientation = true;

  // Active segment arc-length cache
  private static final class SegmentLUT {
    int preTime;
    int nextTime;
    net.tysontheember.apertureapi.common.animation.PathInterpolator mode;
    net.tysontheember.apertureapi.path.ArcLengthLUT lut;
  }

  private SegmentLUT currentLUT;

  public void tick() {
    if (!playing || path == null) {
      return;
    }

    if (exiting) {
      // Hold on the current frame while exit fade plays
      return;
    }

    if (time >= path.getLength()) {
      if (loop) {
        // Wrap seamlessly to the beginning
        time = 0;
      } else {
        // Begin exit fade if auto-reset is enabled
        if (autoReset) {
          beginExitSequence();
        } else {
          // Just stop without resetting camera
          playing = false;
        }
      }
    } else {
      time++;
    }
  }

  public void play() {
    playing = true;
  }

  public void stop() {
    // Request graceful exit; don't snap camera (only if auto-reset is enabled)
    if (autoReset) {
      beginExitSequence();
    } else {
      // Just stop playing without resetting camera
      playing = false;
      path = null;
      time = 0;
      exiting = false;
    }
  }

  public void reset() {
    // Request graceful exit; don't snap camera (only if auto-reset is enabled)
    if (autoReset) {
      beginExitSequence();
    } else {
      // Just reset time without changing camera
      time = 0;
    }
  }

  private void beginExitSequence() {
    if (!playing || path == null || exiting) return;
    exiting = true;
    // Fade to black, then at black reset camera and stop playing
    try {
      net.tysontheember.apertureapi.client.gui.overlay.CutsceneFadeOverlay.startExitSequence(
          () -> {
            // At full black: now it's safe to hand control back
            try {
              net.tysontheember.apertureapi.client.network.ClientPayloadSender.cutsceneInvul(false);
            } catch (Throwable ignored) {
            }
            ClientUtil.resetCameraType();
            // Now fully stop
            playing = false;
            path = null;
            time = 0;
            exiting = false;
          });
    } catch (Throwable ignored) {
    }
  }

  public void resetAndPlay() {
    time = 0;
    playing = true;
  }

  public Animator setLoop(boolean loop) {
    this.loop = loop;
    return this;
  }

  public boolean isLoop() {
    return loop;
  }

  public Animator setAutoReset(boolean autoReset) {
    this.autoReset = autoReset;
    return this;
  }

  public boolean isAutoReset() {
    return autoReset;
  }

  public int getTime() {
    return time;
  }

  public void setTime(int time) {
    this.time = time;
  }

  public boolean isPlaying() {
    return playing;
  }

  public void setPathAndPlay(GlobalCameraPath path) {
    this.path = path;
    this.currentLUT = null; // reset LUT cache
    resetAndPlay();
  }

  public void setPathAndPlay(GlobalCameraPath path, Vector3f center, Vector3f rotation) {
    this.path = path;
    this.center.set(center);
    this.rotation.set(rotation);
    rotationMatrix.identity().rotateY((360 - rotation.y) * Mth.DEG_TO_RAD);
    this.currentLUT = null; // reset LUT cache
    resetAndPlay();
  }

  public boolean prepareCameraInfo(Vector3f posDest, Vector3f rotDest, float[] fov) {
    if (path == null) {
      return false;
    }

    float partialTicks = isPlaying() ? partialTicks() : 0;
    float currentTime = time + partialTicks;

    // Use consistent logic: always find the keyframe segment we're in
    Map.Entry<Integer, CameraKeyframe> preEntry =
        path.getPreEntry(time + 1); // +1 to handle exact matches correctly
    Map.Entry<Integer, CameraKeyframe> nextEntry = path.getNextEntry(time);

    // If we're before the first keyframe or after the last
    if (preEntry == null) {
      if (nextEntry == null) return false;
      posDest.set(nextEntry.getValue().getPos());
      rotDest.set(nextEntry.getValue().getRot());
      fov[0] = nextEntry.getValue().getFov();
      return true;
    }

    if (nextEntry == null) {
      posDest.set(preEntry.getValue().getPos());
      rotDest.set(preEntry.getValue().getRot());
      fov[0] = preEntry.getValue().getFov();
      return true;
    }

    // Calculate interpolation parameter with proper boundary handling
    float timeDelta = nextEntry.getKey() - preEntry.getKey();
    float t;

    if (timeDelta <= 0.001f) {
      // Handle very close or identical keyframe times
      t = 0.0f;
    } else {
      t =
          net.tysontheember.apertureapi.common.animation.JitterPrevention.calculateSmoothT(
              currentTime, preEntry.getKey(), nextEntry.getKey());
    }

    CameraKeyframe pre = preEntry.getValue();
    CameraKeyframe next = nextEntry.getValue();

    float t1;
    // Position interpolation (time easing)
    if (next.getPosTimeInterpolator() == TimeInterpolator.BEZIER) {
      t1 = next.getPosBezier().interpolate(t);
    } else {
      t1 = t;
    }

    // If constantSpeed is on, remap t1 by arc-length LUT for this segment
    float tPos = t1;
    if (constantSpeed
        && next.getPathInterpolator()
            != net.tysontheember.apertureapi.common.animation.PathInterpolator.STEP) {
      ensureSegmentLUT(path, preEntry, nextEntry, next.getPathInterpolator());
      if (currentLUT != null && currentLUT.lut != null && currentLUT.lut.totalLength() > 1e-6f) {
        float desired = currentLUT.lut.totalLength() * t1;
        tPos = currentLUT.lut.tForDistance(desired);
      }
    }

    switch (next.getPathInterpolator()) {
      case LINEAR -> line(tPos, pre.getPos(), next.getPos(), posDest);
      case COSINE -> {
        float tCos = (1.0f - (float) Math.cos(Math.PI * tPos)) * 0.5f;
        line(tCos, pre.getPos(), next.getPos(), posDest);
      }
      case SMOOTH -> {
        Vector3f p0, p3;
        Map.Entry<Integer, CameraKeyframe> prePre = path.getPreEntry(preEntry.getKey());

        if (prePre == null) {
          p0 = pre.getPos();
        } else {
          p0 = prePre.getValue().getPos();
        }

        Map.Entry<Integer, CameraKeyframe> nextNext = path.getNextEntry(nextEntry.getKey());

        if (nextNext == null) {
          p3 = next.getPos();
        } else {
          p3 = nextNext.getValue().getPos();
        }

        catmullRom(tPos, p0, pre.getPos(), next.getPos(), p3, posDest);
      }
      case CATMULL_UNIFORM, CATMULL_CENTRIPETAL, CATMULL_CHORDAL -> {
        Vector3f p0, p3;
        Map.Entry<Integer, CameraKeyframe> prePre = path.getPreEntry(preEntry.getKey());
        if (prePre == null) p0 = pre.getPos();
        else p0 = prePre.getValue().getPos();
        Map.Entry<Integer, CameraKeyframe> nextNext = path.getNextEntry(nextEntry.getKey());
        if (nextNext == null) p3 = next.getPos();
        else p3 = nextNext.getValue().getPos();
        float alpha =
            switch (next.getPathInterpolator()) {
              case CATMULL_UNIFORM -> 0.0f;
              case CATMULL_CHORDAL -> 1.0f;
              default -> 0.5f; // CATMULL_CENTRIPETAL default for stability
            };
        net.tysontheember.apertureapi.path.CatmullRom.eval(
            tPos, p0, pre.getPos(), next.getPos(), p3, alpha, posDest);
      }
      case BEZIER -> next.getPathBezier().interpolate(tPos, pre.getPos(), next.getPos(), posDest);
      case STEP -> posDest.set(pre.getPos());
    }

    // Rotation interpolation (time easing for rotation)
    if (next.getRotTimeInterpolator() == TimeInterpolator.BEZIER) {
      t1 = next.getRotBezier().interpolate(t);
    } else {
      t1 = t;
    }

    Vector3f preRot = pre.getRot();
    Vector3f nextRot = next.getRot();
    if (quaternionOrientation) {
      // Quaternion slerp orientation blending
      org.joml.Quaternionf qa =
          net.tysontheember.apertureapi.path.OrientationUtil.yprDegToQuat(
              preRot.y, preRot.x, preRot.z, new org.joml.Quaternionf());
      org.joml.Quaternionf qb =
          net.tysontheember.apertureapi.path.OrientationUtil.yprDegToQuat(
              nextRot.y, nextRot.x, nextRot.z, new org.joml.Quaternionf());
      org.joml.Quaternionf qOut = new org.joml.Quaternionf();
      net.tysontheember.apertureapi.path.OrientationUtil.slerp(qa, qb, t1, qOut);
      net.tysontheember.apertureapi.path.OrientationUtil.quatToYprDeg(qOut, rotDest);
    } else {
      // Fallback to angle-aware lerp
      net.tysontheember.apertureapi.common.animation.JitterPrevention.smoothRotationLerp(
          t1, preRot, nextRot, rotDest);
    }

    // FOV interpolation
    if (next.getFovTimeInterpolator() == TimeInterpolator.BEZIER) {
      t1 = next.getFovBezier().interpolate(t);
    } else {
      t1 = t;
    }

    fov[0] =
        net.tysontheember.apertureapi.common.animation.JitterPrevention.smoothFovLerp(
            t1, pre.getFov(), next.getFov());

    if (path.isNativeMode()) {
      rotationMatrix.transform(posDest).add(center);
      rotDest.add(rotation);
    }

    return true;
  }

  // Build or reuse LUT for the active segment, matching segment endpoints and interpolation mode
  private void ensureSegmentLUT(
      GlobalCameraPath path,
      Map.Entry<Integer, CameraKeyframe> preEntry,
      Map.Entry<Integer, CameraKeyframe> nextEntry,
      net.tysontheember.apertureapi.common.animation.PathInterpolator mode) {
    int preTime = preEntry.getKey();
    int nextTime = nextEntry.getKey();
    if (currentLUT != null
        && currentLUT.preTime == preTime
        && currentLUT.nextTime == nextTime
        && currentLUT.mode == mode) {
      return;
    }
    currentLUT = new SegmentLUT();
    currentLUT.preTime = preTime;
    currentLUT.nextTime = nextTime;
    currentLUT.mode = mode;

    final CameraKeyframe pre = preEntry.getValue();
    final CameraKeyframe next = nextEntry.getValue();
    // Neighbors for catmull variants
    Vector3f p0, p3;
    Map.Entry<Integer, CameraKeyframe> prePre = path.getPreEntry(preEntry.getKey());
    p0 = (prePre == null) ? pre.getPos() : prePre.getValue().getPos();
    Map.Entry<Integer, CameraKeyframe> nextNext = path.getNextEntry(nextEntry.getKey());
    p3 = (nextNext == null) ? next.getPos() : nextNext.getValue().getPos();

    net.tysontheember.apertureapi.path.ArcLengthLUT.Evaluator f =
        (tt, out) -> {
          switch (mode) {
            case LINEAR -> net.tysontheember.apertureapi.InterpolationMath.line(
                tt, pre.getPos(), next.getPos(), out);
            case COSINE -> {
              float tCos = (1.0f - (float) Math.cos(Math.PI * tt)) * 0.5f;
              net.tysontheember.apertureapi.InterpolationMath.line(
                  tCos, pre.getPos(), next.getPos(), out);
            }
            case SMOOTH -> net.tysontheember.apertureapi.InterpolationMath.catmullRom(
                tt, p0, pre.getPos(), next.getPos(), p3, out);
            case CATMULL_UNIFORM -> net.tysontheember.apertureapi.path.CatmullRom.eval(
                tt, p0, pre.getPos(), next.getPos(), p3, 0.0f, out);
            case CATMULL_CENTRIPETAL -> net.tysontheember.apertureapi.path.CatmullRom.eval(
                tt, p0, pre.getPos(), next.getPos(), p3, 0.5f, out);
            case CATMULL_CHORDAL -> net.tysontheember.apertureapi.path.CatmullRom.eval(
                tt, p0, pre.getPos(), next.getPos(), p3, 1.0f, out);
            case BEZIER -> pre.getPathBezier().interpolate(tt, pre.getPos(), next.getPos(), out);
            case STEP -> out.set(pre.getPos());
          }
          return out;
        };
    currentLUT.lut = new net.tysontheember.apertureapi.path.ArcLengthLUT(f, 64);
  }

  // Optional toggles
  public Animator setConstantSpeed(boolean enabled) {
    this.constantSpeed = enabled;
    return this;
  }

  public Animator setQuaternionOrientation(boolean enabled) {
    this.quaternionOrientation = enabled;
    return this;
  }
}
