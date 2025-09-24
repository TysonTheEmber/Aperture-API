package net.tysontheember.apertureapi.client;

import net.tysontheember.apertureapi.common.animation.*;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.Map;

import static net.tysontheember.apertureapi.client.ClientUtil.partialTicks;
import static net.tysontheember.apertureapi.InterpolationMath.*;

public class PreviewAnimator {
    public static final PreviewAnimator INSTANCE = new PreviewAnimator();
    private boolean playing;
    private int time;

    public void tick() {
        if (!playing) {
            return;
        }

        if (time >= CameraAnimIdeCache.getPath().getLength()) {
            // Wrap seamlessly to the beginning to avoid a one-frame stall at the end
            time = 0;
        } else {
            time++;
        }
    }

    public void play() {
        playing = true;
    }

    public void stop() {
        playing = false;
    }

    public void reset() {
        time = 0;
        playing = false;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public void back() {
        if (time <= 0) {
            return;
        }

        time = Math.max(0, time - 5);
    }

    public void forward() {
        if (time >= CameraAnimIdeCache.getPath().getLength()) {
            return;
        }

        time = Math.min(CameraAnimIdeCache.getPath().getLength(), time + 5);
    }

    public boolean isPlaying() {
        return playing;
    }

    public boolean prepareCameraInfo(Vector3f posDest, Vector3f rotDest, float[] fov) {
        float partialTicks = isPlaying() ? partialTicks() : 0;
        GlobalCameraPath track = CameraAnimIdeCache.getPath();
        float currentTime = time + partialTicks;
        
        // Use consistent logic: always find the keyframe segment we're in
        Map.Entry<Integer, CameraKeyframe> preEntry = track.getPreEntry(time + 1); // +1 to handle exact matches correctly
        Map.Entry<Integer, CameraKeyframe> nextEntry = track.getNextEntry(time);
        
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
            t = net.tysontheember.apertureapi.common.animation.JitterPrevention.calculateSmoothT(currentTime, preEntry.getKey(), nextEntry.getKey());
        }

        CameraKeyframe pre = preEntry.getValue();
        CameraKeyframe next = nextEntry.getValue();

        float t1;
        // Position interpolation
        if (next.getPosTimeInterpolator() == TimeInterpolator.BEZIER) {
            t1 = next.getPosBezier().interpolate(t);
        } else {
            t1 = t;
        }

        switch (next.getPathInterpolator()) {
            case LINEAR -> line(t1, pre.getPos(), next.getPos(), posDest);
            case SMOOTH -> {
                Vector3f p0, p3;
                Map.Entry<Integer, CameraKeyframe> prePre = track.getPreEntry(preEntry.getKey());

                if (prePre == null) {
                    p0 = pre.getPos();
                } else {
                    p0 = prePre.getValue().getPos();
                }

                Map.Entry<Integer, CameraKeyframe> nextNext = track.getNextEntry(nextEntry.getKey());

                if (nextNext == null) {
                    p3 = next.getPos();
                } else {
                    p3 = nextNext.getValue().getPos();
                }

                catmullRom(t1, p0, pre.getPos(), next.getPos(), p3, posDest);
            }
            case BEZIER -> next.getPathBezier().interpolate(t1, pre.getPos(), next.getPos(), posDest);
            case STEP -> posDest.set(pre.getPos());
        }

        // Rotation interpolation
        if (next.getRotTimeInterpolator() == TimeInterpolator.BEZIER) {
            t1 = next.getRotBezier().interpolate(t);
        } else {
            t1 = t;
        }

        Vector3f preRot = pre.getRot();
        Vector3f nextRot = next.getRot();
        // Use smooth rotation interpolation to prevent angle wrapping issues
        net.tysontheember.apertureapi.common.animation.JitterPrevention.smoothRotationLerp(t1, preRot, nextRot, rotDest);

        // FOV interpolation
        if (next.getFovTimeInterpolator() == TimeInterpolator.BEZIER) {
            t1 = next.getFovBezier().interpolate(t);
        } else {
            t1 = t;
        }

        fov[0] = net.tysontheember.apertureapi.common.animation.JitterPrevention.smoothFovLerp(t1, pre.getFov(), next.getFov());

        return true;
    }
}

