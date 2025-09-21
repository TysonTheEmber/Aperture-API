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

        if (time > CameraAnimIdeCache.getPath().getLength()) {
            reset();
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
        Map.Entry<Integer, CameraKeyframe> current = track.getEntry(time);

        Map.Entry<Integer, CameraKeyframe> preEntry = current == null ? track.getPreEntry(time) : current;
        Map.Entry<Integer, CameraKeyframe> nextEntry = track.getNextEntry(time);
        float t;

        if (preEntry == null) {
            if (nextEntry == null) return false;
            posDest.set(nextEntry.getValue().getPos());
            rotDest.set(nextEntry.getValue().getRot());
            return true;
        } else {
            if (nextEntry == null) {
                posDest.set(preEntry.getValue().getPos());
                rotDest.set(preEntry.getValue().getRot());
                return true;
            } else {
                t = (partialTicks + time - preEntry.getKey()) / (nextEntry.getKey() - preEntry.getKey());
            }
        }

        CameraKeyframe pre = preEntry.getValue();
        CameraKeyframe next = nextEntry.getValue();

        float t1;
        // 坐标插值
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

        // 旋转插值
        if (next.getPosTimeInterpolator() == TimeInterpolator.BEZIER) {
            t1 = next.getRotBezier().interpolate(t);
        } else {
            t1 = t;
        }

        Vector3f preRot = pre.getRot();
        Vector3f nextRot = next.getRot();
        line(t1, preRot, nextRot, rotDest);

        // fov插值
        if (next.getPosTimeInterpolator() == TimeInterpolator.BEZIER) {
            t1 = next.getRotBezier().interpolate(t);
        } else {
            t1 = t;
        }

        fov[0] = Mth.lerp(t1, pre.getFov(), next.getFov());

        return true;
    }
}

