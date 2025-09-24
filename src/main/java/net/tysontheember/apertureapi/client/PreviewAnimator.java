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
    private boolean loop = false; // Default to no loop
    private int time;

    private boolean constantSpeed = true;
    private boolean quaternionOrientation = true;

    // Speed-based playback state (blocks/sec)
    public enum SpeedMode { DURATION, SPEED }
    private SpeedMode speedMode = SpeedMode.DURATION;
    private float speedBlocksPerSec = 6.0f;
    private float segDistance = 0f; // distance progressed into current segment
    private Integer segPreTime = null;
    private Integer segNextTime = null;

    // Follow-target orientation state
    private boolean followTargetEnabled = false;
    private final org.joml.Vector3f followTarget = new org.joml.Vector3f();
    private float followLag = 0.2f; // 0..1 per-frame smoothing factor
    private final org.joml.Vector3f lastAimDir = new org.joml.Vector3f();
    private boolean hasLastAimDir = false;

    private static final class SegmentLUT {
        int preTime;
        int nextTime;
        net.tysontheember.apertureapi.common.animation.PathInterpolator mode;
        net.tysontheember.apertureapi.path.ArcLengthLUT lut;
    }
    private SegmentLUT currentLUT;

    public void tick() {
        if (!playing) {
            return;
        }

        if (speedMode == SpeedMode.SPEED) {
            // Advance by distance per tick; segment wrap handled in prepareCameraInfo
            float dt = 1.0f / 20.0f;
            segDistance += Math.max(0f, speedBlocksPerSec) * dt;
        } else {
            if (time >= CameraAnimIdeCache.getPath().getLength()) {
                if (loop) {
                    // Wrap seamlessly to the beginning to avoid a one-frame stall at the end
                    time = 0;
                } else {
                    // Stop playing when reaching the end
                    playing = false;
                }
            } else {
                time++;
            }
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
        currentLUT = null;
        segDistance = 0f;
        segPreTime = null;
        segNextTime = null;
        hasLastAimDir = false;
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
        
        Map.Entry<Integer, CameraKeyframe> preEntry;
        Map.Entry<Integer, CameraKeyframe> nextEntry;
        if (speedMode == SpeedMode.SPEED) {
            // Initialize segment if needed: start from first two keyframes
            if (segPreTime == null || segNextTime == null) {
                Map.Entry<Integer, CameraKeyframe> first = track.getNextEntry(Integer.MIN_VALUE);
                if (first == null) return false;
                Map.Entry<Integer, CameraKeyframe> second = track.getNextEntry(first.getKey());
                if (second == null) return false;
                segPreTime = first.getKey();
                segNextTime = second.getKey();
                segDistance = 0f;
                currentLUT = null;
            }
            preEntry = track.getEntry(segPreTime);
            if (preEntry == null || preEntry.getKey() != segPreTime) preEntry = track.getPreEntry(segPreTime + 1);
            nextEntry = track.getEntry(segNextTime);
            if (nextEntry == null || nextEntry.getKey() != segNextTime) nextEntry = track.getNextEntry(segPreTime);
        } else {
            // Use consistent logic: always find the keyframe segment we're in
            preEntry = track.getPreEntry(time + 1); // +1 to handle exact matches correctly
            nextEntry = track.getNextEntry(time);
        }
        
        // If we're before the first keyframe or after the last
        if (preEntry == null) {
            if (nextEntry == null) return false;
            posDest.set(nextEntry.getValue().getPos());
            rotDest.set(nextEntry.getValue().getRot());
            fov[0] = nextEntry.getValue().getFov();
            return true;
        }
        
        if (nextEntry == null) {
            if (speedMode == SpeedMode.SPEED && loop && track.getPoints().size() >= 2) {
                // Loop to start in SPEED mode (only if loop is enabled)
                Map.Entry<Integer, CameraKeyframe> first = track.getNextEntry(Integer.MIN_VALUE);
                Map.Entry<Integer, CameraKeyframe> second = first == null ? null : track.getNextEntry(first.getKey());
                if (first != null && second != null) {
                    segPreTime = first.getKey();
                    segNextTime = second.getKey();
                    segDistance = 0f;
                    currentLUT = null;
                    preEntry = first;
                    nextEntry = second;
                } else {
                    // Stop at the end if not looping or can't loop
                    if (playing) playing = false;
                    posDest.set(preEntry.getValue().getPos());
                    rotDest.set(preEntry.getValue().getRot());
                    fov[0] = preEntry.getValue().getFov();
                    return true;
                }
            } else {
                // Stop at the end if not looping
                if (playing) playing = false;
                posDest.set(preEntry.getValue().getPos());
                rotDest.set(preEntry.getValue().getRot());
                fov[0] = preEntry.getValue().getFov();
                return true;
            }
        }
        
        // Calculate interpolation parameter
        float t;
        if (speedMode == SpeedMode.SPEED) {
            ensureSegmentLUT(track, preEntry, nextEntry, nextEntry.getValue().getPathInterpolator());
            float segLen = (currentLUT != null && currentLUT.lut != null) ? currentLUT.lut.totalLength() : 0f;
            // Advance across segments as needed
            while (segLen > 1e-6f && segDistance > segLen) {
                segDistance -= segLen;
                // advance to next segment (loop if needed)
                Map.Entry<Integer, CameraKeyframe> newPre = nextEntry;
                Map.Entry<Integer, CameraKeyframe> newNext = track.getNextEntry(newPre.getKey());
                if (newNext == null) {
                    if (loop) {
                        // loop (only if loop is enabled)
                        Map.Entry<Integer, CameraKeyframe> first = track.getNextEntry(Integer.MIN_VALUE);
                        if (first == null) break;
                        newPre = first;
                        newNext = track.getNextEntry(newPre.getKey());
                        if (newNext == null) break;
                    } else {
                        // Stop at the end if not looping
                        playing = false;
                        break;
                    }
                }
                preEntry = newPre;
                nextEntry = newNext;
                ensureSegmentLUT(track, preEntry, nextEntry, nextEntry.getValue().getPathInterpolator());
                segLen = (currentLUT != null && currentLUT.lut != null) ? currentLUT.lut.totalLength() : 0f;
            }
            if (currentLUT != null && currentLUT.lut != null && currentLUT.lut.totalLength() > 1e-6f) {
                t = currentLUT.lut.tForDistance(Math.max(0f, Math.min(segDistance, currentLUT.lut.totalLength())));
            } else {
                t = 0f;
            }
        } else {
            float timeDelta = nextEntry.getKey() - preEntry.getKey();
            if (timeDelta <= 0.001f) {
                t = 0.0f;
            } else {
                t = net.tysontheember.apertureapi.common.animation.JitterPrevention.calculateSmoothT(currentTime, preEntry.getKey(), nextEntry.getKey());
            }
        }

        CameraKeyframe pre = preEntry.getValue();
        CameraKeyframe next = nextEntry.getValue();

        float t1;
        // Position time easing
        if (next.getPosTimeInterpolator() == TimeInterpolator.BEZIER) {
            t1 = next.getPosBezier().interpolate(t);
        } else {
            t1 = t;
        }

        float tPos = t1;
        if (constantSpeed && next.getPathInterpolator() != net.tysontheember.apertureapi.common.animation.PathInterpolator.STEP) {
            ensureSegmentLUT(track, preEntry, nextEntry, next.getPathInterpolator());
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

                catmullRom(tPos, p0, pre.getPos(), next.getPos(), p3, posDest);
            }
            case CATMULL_UNIFORM, CATMULL_CENTRIPETAL, CATMULL_CHORDAL -> {
                Vector3f p0, p3;
                Map.Entry<Integer, CameraKeyframe> prePre = track.getPreEntry(preEntry.getKey());
                if (prePre == null) p0 = pre.getPos(); else p0 = prePre.getValue().getPos();
                Map.Entry<Integer, CameraKeyframe> nextNext = track.getNextEntry(nextEntry.getKey());
                if (nextNext == null) p3 = next.getPos(); else p3 = nextNext.getValue().getPos();
                float alpha = switch (next.getPathInterpolator()) {
                    case CATMULL_UNIFORM -> 0.0f;
                    case CATMULL_CHORDAL -> 1.0f;
                    default -> 0.5f;
                };
                net.tysontheember.apertureapi.path.CatmullRom.eval(tPos, p0, pre.getPos(), next.getPos(), p3, alpha, posDest);
            }
            case BEZIER -> next.getPathBezier().interpolate(tPos, pre.getPos(), next.getPos(), posDest);
            case STEP -> posDest.set(pre.getPos());
        }

        // Rotation time easing
        if (next.getRotTimeInterpolator() == TimeInterpolator.BEZIER) {
            t1 = next.getRotBezier().interpolate(t);
        } else {
            t1 = t;
        }

        // Follow-target orientation overrides angle blending if enabled
        if (followTargetEnabled) {
            // World-space look direction: target - current position (posDest currently in world space in PreviewAnimator)
            Vector3f desired = new Vector3f(followTarget).sub(posDest).normalize();
            if (!hasLastAimDir) {
                lastAimDir.set(desired);
                hasLastAimDir = true;
            }
            // Lag smoothing
            lastAimDir.lerp(desired, Mth.clamp(followLag, 0f, 1f));
            // Convert to yaw/pitch
            float yaw = (float)(Math.toDegrees(Math.atan2(-lastAimDir.x, lastAimDir.z)));
            float pitch = (float)(-Math.toDegrees(Math.atan2(lastAimDir.y, Math.sqrt(lastAimDir.x*lastAimDir.x + lastAimDir.z*lastAimDir.z))));
            rotDest.set(pitch, yaw, 0f);
        } else {
            Vector3f preRot = pre.getRot();
            Vector3f nextRot = next.getRot();
            if (quaternionOrientation) {
                org.joml.Quaternionf qa = net.tysontheember.apertureapi.path.OrientationUtil.yprDegToQuat(preRot.y, preRot.x, preRot.z, new org.joml.Quaternionf());
                org.joml.Quaternionf qb = net.tysontheember.apertureapi.path.OrientationUtil.yprDegToQuat(nextRot.y, nextRot.x, nextRot.z, new org.joml.Quaternionf());
                org.joml.Quaternionf qOut = new org.joml.Quaternionf();
                net.tysontheember.apertureapi.path.OrientationUtil.slerp(qa, qb, t1, qOut);
                net.tysontheember.apertureapi.path.OrientationUtil.quatToYprDeg(qOut, rotDest);
            } else {
                net.tysontheember.apertureapi.common.animation.JitterPrevention.smoothRotationLerp(t1, preRot, nextRot, rotDest);
            }
        }

        // FOV interpolation
        if (next.getFovTimeInterpolator() == TimeInterpolator.BEZIER) {
            t1 = next.getFovBezier().interpolate(t);
        } else {
            t1 = t;
        }

        fov[0] = net.tysontheember.apertureapi.common.animation.JitterPrevention.smoothFovLerp(t1, pre.getFov(), next.getFov());

        return true;
    }

    private void ensureSegmentLUT(GlobalCameraPath track, Map.Entry<Integer, CameraKeyframe> preEntry, Map.Entry<Integer, CameraKeyframe> nextEntry, net.tysontheember.apertureapi.common.animation.PathInterpolator mode) {
        int preTime = preEntry.getKey();
        int nextTime = nextEntry.getKey();
        if (currentLUT != null && currentLUT.preTime == preTime && currentLUT.nextTime == nextTime && currentLUT.mode == mode) {
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
        Map.Entry<Integer, CameraKeyframe> prePre = track.getPreEntry(preEntry.getKey());
        p0 = (prePre == null) ? pre.getPos() : prePre.getValue().getPos();
        Map.Entry<Integer, CameraKeyframe> nextNext = track.getNextEntry(nextEntry.getKey());
        p3 = (nextNext == null) ? next.getPos() : nextNext.getValue().getPos();

        net.tysontheember.apertureapi.path.ArcLengthLUT.Evaluator f = (tt, out) -> {
            switch (mode) {
                case LINEAR -> net.tysontheember.apertureapi.InterpolationMath.line(tt, pre.getPos(), next.getPos(), out);
                case COSINE -> {
                    float tCos = (1.0f - (float) Math.cos(Math.PI * tt)) * 0.5f;
                    net.tysontheember.apertureapi.InterpolationMath.line(tCos, pre.getPos(), next.getPos(), out);
                }
                case SMOOTH -> net.tysontheember.apertureapi.InterpolationMath.catmullRom(tt, p0, pre.getPos(), next.getPos(), p3, out);
                case CATMULL_UNIFORM -> net.tysontheember.apertureapi.path.CatmullRom.eval(tt, p0, pre.getPos(), next.getPos(), p3, 0.0f, out);
                case CATMULL_CENTRIPETAL -> net.tysontheember.apertureapi.path.CatmullRom.eval(tt, p0, pre.getPos(), next.getPos(), p3, 0.5f, out);
                case CATMULL_CHORDAL -> net.tysontheember.apertureapi.path.CatmullRom.eval(tt, p0, pre.getPos(), next.getPos(), p3, 1.0f, out);
                case BEZIER -> pre.getPathBezier().interpolate(tt, pre.getPos(), next.getPos(), out);
                case STEP -> out.set(pre.getPos());
            }
            return out;
        };
        currentLUT.lut = new net.tysontheember.apertureapi.path.ArcLengthLUT(f, 64);
    }

    public PreviewAnimator setLoop(boolean loop) { this.loop = loop; return this; }
    public boolean isLoop() { return loop; }
    public PreviewAnimator setConstantSpeed(boolean enabled) { this.constantSpeed = enabled; return this; }
    public PreviewAnimator setQuaternionOrientation(boolean enabled) { this.quaternionOrientation = enabled; return this; }
    public PreviewAnimator setSpeedMode(SpeedMode mode) { this.speedMode = mode == null ? SpeedMode.DURATION : mode; return this; }
    public PreviewAnimator setSpeedBlocksPerSec(float bps) { this.speedBlocksPerSec = Math.max(0f, bps); return this; }
    public PreviewAnimator enableFollowTarget(boolean enabled) { this.followTargetEnabled = enabled; if (!enabled) hasLastAimDir = false; return this; }
    public PreviewAnimator setFollowTarget(float x, float y, float z) { this.followTarget.set(x,y,z); return this; }
    public PreviewAnimator setFollowLag(float lag) { this.followLag = lag; return this; }
}

