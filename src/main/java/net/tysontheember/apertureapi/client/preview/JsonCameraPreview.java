package net.tysontheember.apertureapi.client.preview;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.tysontheember.apertureapi.ApertureAPI;
import net.tysontheember.apertureapi.ICameraModifier;
import net.tysontheember.apertureapi.CameraModifierManager;
import net.tysontheember.apertureapi.camera.CameraPathDef;
import net.tysontheember.apertureapi.camera.CameraPathRegistry;
import net.tysontheember.apertureapi.client.CameraAnimIdeCache;
import net.tysontheember.apertureapi.client.ClientUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.List;

/**
 * Minimal client-only JSON camera path preview.
 * Supports: linear position, linear rotation (yaw/pitch) or lookAt per-keyframe, keyframed FOV.
 * Falls back gracefully if fields are missing.
 */
@Mod.EventBusSubscriber(modid = ApertureAPI.MODID, value = Dist.CLIENT)
public class JsonCameraPreview {
    private static final ICameraModifier PREVIEW_MOD = CameraModifierManager
            .createModifier(ApertureAPI.MODID + "_json_preview", true)
            .enableGlobalMode()
            .enablePos()
            .enableRotation()
            .enableFov()
            .enableLerp();

    private static CameraPathDef def;
    private static boolean playing;
    private static float t; // seconds
    private static float duration; // seconds

    // Freeze original player pose so their body remains where preview started
    private static Vector3f savedPos;
    private static Vector3f savedRot; // pitch,yaw,roll(0)
    private static boolean savedNoGravity;

    public static void start(String id) {
        CameraPathDef found = CameraPathRegistry.get(id);
        if (found == null || found.keyframes == null || found.keyframes.isEmpty()) {
            stop();
            return;
        }
        def = found;
        duration = def.keyframes.get(def.keyframes.size() - 1).time;
        t = 0f;
        playing = true;
        CameraAnimIdeCache.PREVIEW = true; // hide hand and IDE overlays
        // Freeze player at current pose
        var p = Minecraft.getInstance().player;
        if (p != null) {
            savedPos = new Vector3f((float)p.getX(), (float)p.getY(), (float)p.getZ());
            savedRot = new Vector3f(p.getXRot(), p.getYRot(), 0f);
            savedNoGravity = p.isNoGravity();
            p.setNoGravity(true);
            p.setDeltaMovement(0,0,0);
            p.setPos(savedPos.x, savedPos.y, savedPos.z);
            p.setXRot(savedRot.x);
            p.setYRot(savedRot.y);
        }
        ClientUtil.toThirdView(); // ensure player body is visible
    }

    public static void stop() {
        playing = false;
        def = null;
        PREVIEW_MOD.disable();
        CameraAnimIdeCache.PREVIEW = false;
        // Restore player physics
        var p = Minecraft.getInstance().player;
        if (p != null) {
            p.setNoGravity(savedNoGravity);
        }
        ClientUtil.resetCameraType();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (!playing || def == null) return;
        if (Minecraft.getInstance().isPaused()) return;
        // Keep player frozen at saved pose while previewing
        var p = Minecraft.getInstance().player;
        if (p != null && savedPos != null && savedRot != null) {
            p.setDeltaMovement(0,0,0);
            p.setPos(savedPos.x, savedPos.y, savedPos.z);
            p.setXRot(savedRot.x);
            p.setYRot(savedRot.y);
        }

        float dt = 1.0f / 20.0f; // tick seconds
        t += dt * Math.max(0.001f, def.speed <= 0 ? 1f : def.speed);
        if (t > duration) {
            if (def.loop) {
                t = t % Math.max(0.0001f, duration);
            } else {
                stop();
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent event) {
        if (!playing || def == null) return;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
        float frame = Minecraft.getInstance().getFrameTime(); // partial ticks [0..1)
        float timeSec = t + frame * Math.max(0.001f, def.speed <= 0 ? 1f : def.speed) / 20.0f;
        if (timeSec > duration) {
            timeSec = def.loop ? (timeSec % Math.max(0.0001f, duration)) : duration;
        }
        sampleAndApply(timeSec);
    }

    private static void sampleAndApply(float timeSec) {
        List<CameraPathDef.KeyframeDef> kfs = def.keyframes;
        int preIdx = 0;
        int nextIdx = 0;
        for (int i = 0; i < kfs.size(); i++) {
            if (kfs.get(i).time <= timeSec) preIdx = i;
            if (kfs.get(i).time >= timeSec) { nextIdx = i; break; }
        }
        CameraPathDef.KeyframeDef pre = kfs.get(preIdx);
        CameraPathDef.KeyframeDef next = kfs.get(nextIdx);
        float span = Math.max(1e-6f, next.time - pre.time);
        float localT = Mth.clamp((timeSec - pre.time) / span, 0f, 1f);

        // Position: linear fallback
        Vector3f p0 = new Vector3f((float)pre.pos.x, (float)pre.pos.y, (float)pre.pos.z);
        Vector3f p1 = new Vector3f((float)next.pos.x, (float)next.pos.y, (float)next.pos.z);
        Vector3f pos = new Vector3f(
                Mth.lerp(localT, p0.x, p1.x),
                Mth.lerp(localT, p0.y, p1.y),
                Mth.lerp(localT, p0.z, p1.z)
        );

        // Rotation: slerp between forward direction vectors, then convert to yaw/pitch
        Vector3f f0 = resolveForward(pre);
        Vector3f f1 = resolveForward(next);
        Vector3f f = slerpDir(f0, f1, localT);
        float yaw = (float)(Math.toDegrees(Math.atan2(-f.x, f.z)));
        float pitch = (float)(-Math.toDegrees(Math.atan2(f.y, Math.sqrt(f.x*f.x + f.z*f.z))));
        Vector3f rot = new Vector3f(pitch, yaw, 0f);

        // FOV: keyframed linear if available
        float fov = 70f;
        if (def.fov != null) {
            if ("fixed".equalsIgnoreCase(def.fov.mode) || "inherit".equalsIgnoreCase(def.fov.mode)) {
                fov = def.fov._default;
            } else if ("keyframed".equalsIgnoreCase(def.fov.mode)) {
                float fov0 = pre.fov != null ? pre.fov : def.fov._default;
                float fov1 = next.fov != null ? next.fov : def.fov._default;
                fov = Mth.lerp(localT, fov0, fov1);
            }
        }

        PREVIEW_MOD.enable()
                .setPos(pos.x, pos.y, pos.z)
                .setRotationYXZ(rot.x, rot.y, rot.z)
                .setFov(fov);
    }

    private static Vector3f resolveForward(CameraPathDef.KeyframeDef kf) {
        if (kf.lookAt != null && kf.pos != null) {
            double dx = kf.lookAt.x - kf.pos.x;
            double dy = kf.lookAt.y - kf.pos.y;
            double dz = kf.lookAt.z - kf.pos.z;
            Vector3f v = new Vector3f((float)dx, (float)dy, (float)dz);
            if (v.lengthSquared() < 1e-6f) return new Vector3f(0, 0, 1);
            v.normalize();
            return v;
        }
        if (kf.rot != null) {
            float yawRad = (float)Math.toRadians(kf.rot.yaw);
            float pitchRad = (float)Math.toRadians(kf.rot.pitch);
            float cp = Mth.cos(pitchRad);
            float sp = Mth.sin(pitchRad);
            float cy = Mth.cos(yawRad);
            float sy = Mth.sin(yawRad);
            // Forward vector consistent with yaw/pitch conversions above
            float x = -cp * sy;
            float y = -sp;
            float z = cp * cy;
            return new Vector3f(x, y, z).normalize();
        }
        return new Vector3f(0, 0, 1);
    }

    private static Vector3f slerpDir(Vector3f a, Vector3f b, float t) {
        Vector3f v0 = new Vector3f(a).normalize();
        Vector3f v1 = new Vector3f(b).normalize();
        float dot = Mth.clamp(v0.dot(v1), -1f, 1f);
        if (dot > 0.9995f) {
            // Nearly the same; fall back to normalized lerp
            return new Vector3f(
                    Mth.lerp(t, v0.x, v1.x),
                    Mth.lerp(t, v0.y, v1.y),
                    Mth.lerp(t, v0.z, v1.z)
            ).normalize();
        }
        double theta0 = Math.acos(dot);
        double sin0 = Math.sin(theta0);
        double theta = theta0 * t;
        double sin1 = Math.sin(theta);
        double sin2 = Math.sin(theta0 - theta);
        float s0 = (float)(sin2 / sin0);
        float s1 = (float)(sin1 / sin0);
        return new Vector3f(
                v0.x * s0 + v1.x * s1,
                v0.y * s0 + v1.y * s1,
                v0.z * s0 + v1.z * s1
        );
    }
}
