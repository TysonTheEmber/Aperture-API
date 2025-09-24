package net.tysontheember.apertureapi.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.tysontheember.apertureapi.common.GlobalCameraSavedData;
import net.tysontheember.apertureapi.common.animation.*;
import net.tysontheember.apertureapi.common.network.ServerPayloadSender;
import org.joml.Vector3f;

import java.util.UUID;

public class DemoCameraCommand {
    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("DemoCameraCommand");
    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("aperture")
            .then(Commands.literal("demo").requires(src -> src.isPlayer())
                // Consolidated zoom commands under a single group
                .then(Commands.literal("zoom")
                    .then(Commands.literal("face_player")
                        .executes(ctx -> zoomFacePlayer(ctx, 20f, 6))
                        .then(Commands.argument("distance", FloatArgumentType.floatArg(2f, 200f))
                            .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 120))
                                .executes(ctx -> zoomFacePlayer(ctx,
                                    FloatArgumentType.getFloat(ctx, "distance"),
                                    IntegerArgumentType.getInteger(ctx, "seconds"))))))
                    .then(Commands.literal("face_forward")
                        .executes(ctx -> zoomFaceForward(ctx, 20f, 6))
                        .then(Commands.argument("distance", FloatArgumentType.floatArg(2f, 200f))
                            .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 120))
                                .executes(ctx -> zoomFaceForward(ctx,
                                    FloatArgumentType.getFloat(ctx, "distance"),
                                    IntegerArgumentType.getInteger(ctx, "seconds")))))))
                // Consolidated orbit command (ultra-smooth by default)
                .then(Commands.literal("orbit")
                    .executes(ctx -> smoothOrbit360(ctx, 10f, 16, 0f))
                    .then(Commands.argument("radius", FloatArgumentType.floatArg(2f, 200f))
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(2, 300))
                            .then(Commands.argument("yOffset", FloatArgumentType.floatArg(-50f, 50f))
                                .executes(ctx -> smoothOrbit360(ctx,
                                    FloatArgumentType.getFloat(ctx, "radius"),
                                    IntegerArgumentType.getInteger(ctx, "seconds"),
                                    FloatArgumentType.getFloat(ctx, "yOffset"))))))
            )
        ));
        // Alias: /camera mirrors /aperture
        d.register(Commands.literal("camera")
            .then(Commands.literal("demo").requires(src -> src.isPlayer())
                .then(Commands.literal("zoom")
                    .then(Commands.literal("face_player")
                        .executes(ctx -> zoomFacePlayer(ctx, 20f, 6))
                        .then(Commands.argument("distance", FloatArgumentType.floatArg(2f, 200f))
                            .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 120))
                                .executes(ctx -> zoomFacePlayer(ctx,
                                    FloatArgumentType.getFloat(ctx, "distance"),
                                    IntegerArgumentType.getInteger(ctx, "seconds"))))))
                    .then(Commands.literal("face_forward")
                        .executes(ctx -> zoomFaceForward(ctx, 20f, 6))
                        .then(Commands.argument("distance", FloatArgumentType.floatArg(2f, 200f))
                            .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 120))
                                .executes(ctx -> zoomFaceForward(ctx,
                                    FloatArgumentType.getFloat(ctx, "distance"),
                                    IntegerArgumentType.getInteger(ctx, "seconds")))))))
                .then(Commands.literal("orbit")
                    .executes(ctx -> smoothOrbit360(ctx, 10f, 16, 0f))
                    .then(Commands.argument("radius", FloatArgumentType.floatArg(2f, 200f))
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(2, 300))
                            .then(Commands.argument("yOffset", FloatArgumentType.floatArg(-50f, 50f))
                                .executes(ctx -> smoothOrbit360(ctx,
                                    FloatArgumentType.getFloat(ctx, "radius"),
                                    IntegerArgumentType.getInteger(ctx, "seconds"),
                                    FloatArgumentType.getFloat(ctx, "yOffset"))))))
            )
        ));
    }

    private static int zoomFacePlayer(CommandContext<CommandSourceStack> ctx, float distance, int seconds) throws CommandSyntaxException {
        try {
        ctx.getSource().sendSuccess(() -> Component.literal("[demo] building zoom_face_player..."), false);
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        ServerLevel level = ctx.getSource().getLevel();
        Vector3f eye = new Vector3f((float)p.getX(), (float)(p.getY() + p.getEyeHeight()), (float)p.getZ());
        Vector3f view = playerView(p.getYRot(), p.getXRot()); // unit vector

        float start = 2f;
        float end = distance;
        int ticks = seconds * 20;

        GlobalCameraPath path = new GlobalCameraPath("demo_zoom_face_player_" + System.currentTimeMillis(), p);
        ctx.getSource().sendSuccess(() -> Component.literal("[demo] id=" + path.getId()), false);

        // Start keyframe (close to player, in front of player looking at player)
        Vector3f pos0 = new Vector3f(eye).add(new Vector3f(view).mul(start));
        Vector3f rot0 = lookAtRotDeg(pos0, eye);
        CameraKeyframe k0 = kf(pos0, rot0, 70f);

        // End keyframe (farther away, still in front looking at player)
        Vector3f pos1 = new Vector3f(eye).add(new Vector3f(view).mul(end));
        Vector3f rot1 = lookAtRotDeg(pos1, eye);
        CameraKeyframe k1 = kf(pos1, rot1, 70f);

        ctx.getSource().sendSuccess(() -> Component.literal("[demo] add k0"), false);
        path.add(0, k0);
        ctx.getSource().sendSuccess(() -> Component.literal("[demo] add k1 @" + ticks + "t"), false);
        path.add(ticks, k1);

        saveAndPreview(ctx, level, p, path);
        return 1;
        } catch (Exception e) {
            LOGGER.error("zoom_face_player failed", e);
            ctx.getSource().sendFailure(Component.literal("Demo failed: " + e.getClass().getSimpleName() + ": " + String.valueOf(e.getMessage())));
            return 0;
        }
    }

    private static int zoomFaceForward(CommandContext<CommandSourceStack> ctx, float distance, int seconds) throws CommandSyntaxException {
        try {
        ctx.getSource().sendSuccess(() -> Component.literal("[demo] building zoom_face_forward..."), false);
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        ServerLevel level = ctx.getSource().getLevel();
        Vector3f eye = new Vector3f((float)p.getX(), (float)(p.getY() + p.getEyeHeight()), (float)p.getZ());
        Vector3f view = playerView(p.getYRot(), p.getXRot());

        float start = 2f;
        float end = distance;
        int ticks = seconds * 20;

        GlobalCameraPath path = new GlobalCameraPath("demo_zoom_face_forward_" + System.currentTimeMillis(), p);
        ctx.getSource().sendSuccess(() -> Component.literal("[demo] id=" + path.getId()), false);

        // Start: behind player, looking forward with player's rot
        Vector3f pos0 = new Vector3f(eye).sub(new Vector3f(view).mul(start));
        Vector3f rot0 = new Vector3f(p.getXRot(), wrapYaw(p.getYRot()), 0);
        CameraKeyframe k0 = kf(pos0, rot0, 70f);

        // End: further behind, same forward rotation
        Vector3f pos1 = new Vector3f(eye).sub(new Vector3f(view).mul(end));
        Vector3f rot1 = new Vector3f(rot0);
        CameraKeyframe k1 = kf(pos1, rot1, 70f);

        ctx.getSource().sendSuccess(() -> Component.literal("[demo] add k0"), false);
        path.add(0, k0);
        ctx.getSource().sendSuccess(() -> Component.literal("[demo] add k1 @" + ticks + "t"), false);
        path.add(ticks, k1);

        saveAndPreview(ctx, level, p, path);
        return 1;
        } catch (Exception e) {
            LOGGER.error("zoom_face_forward failed", e);
            ctx.getSource().sendFailure(Component.literal("Demo failed: " + e.getClass().getSimpleName() + ": " + String.valueOf(e.getMessage())));
            return 0;
        }
    }

/**
     * Ultra-smooth 360 degree orbit with maximum smoothness settings
     */
    private static int smoothOrbit360(CommandContext<CommandSourceStack> ctx, float radius, int seconds, float yOffset) throws CommandSyntaxException {
        try {
            ctx.getSource().sendSuccess(() -> Component.literal("[demo] building ultra-smooth orbit360..."), false);
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            ServerLevel level = ctx.getSource().getLevel();
            Vector3f center = new Vector3f((float) p.getX(), (float) (p.getY() + p.getEyeHeight() + yOffset), (float) p.getZ());

            int ticks = Math.max(2, seconds * 20);
            // Ultra-high density keyframes for maximum smoothness
            int segments = Math.max(360, Math.min(1440, seconds * 90)); // ~4.5 samples per tick for ultra-smooth motion

            // Generate positions and tangents
            Vector3f[] pos = new Vector3f[segments + 1];
            Vector3f[] tan = new Vector3f[segments + 1];
            for (int i = 0; i <= segments; i++) {
                float t = (float) i / segments;
                double ang = 2 * Math.PI * t;
                float sx = (float) (Math.sin(ang));
                float cz = (float) (Math.cos(ang));
                pos[i] = new Vector3f(center.x + sx * radius, center.y, center.z + cz * radius);
                Vector3f tg = new Vector3f(cz, 0, -sx).normalize();
                tan[i] = tg;
            }

            GlobalCameraPath path = new GlobalCameraPath("demo_smooth_orbit360_" + System.currentTimeMillis(), p);
            
            // Optimized control distance for ultra-smooth circular motion
            float d = calculateOptimalControlDistance(radius, segments) * 0.75f; // Even tighter for ultra-smoothness

            for (int i = 0; i <= segments; i++) {
                int tTick = (i == segments) ? ticks : Math.max(0, Math.min(ticks - 1, Math.round((i / (float) segments) * ticks)));
                Vector3f pCurr = pos[i];
                Vector3f rot = lookAtRotDeg(pCurr, center);
                
                // Create ultra-smooth keyframe with optimized settings
                CameraKeyframe k = new CameraKeyframe(new Vector3f(pCurr), new Vector3f(rot), 70f);
                
                // Use SMOOTH path interpolation as fallback, but prefer BEZIER for control points
                k.setPathInterpolator(PathInterpolator.BEZIER);
                
                // Apply enhanced time interpolation with custom smooth curves
                k.setPosTimeInterpolator(TimeInterpolator.BEZIER);
                k.getPosBezier().setLeft(0.25f, 0.1f);  // Custom smooth curve
                k.getPosBezier().setRight(0.75f, 0.9f);
                
                k.setRotTimeInterpolator(TimeInterpolator.BEZIER);
                k.getRotBezier().easyInOut(); // Keep rotation smooth but standard
                
                k.setFovTimeInterpolator(TimeInterpolator.BEZIER);
                k.getFovBezier().easyInOut();

                // Set bezier control handles for ultra-smooth circular path
                if (i > 0) {
                    Vector3f pPrev = pos[i - 1];
                    Vector3f tPrev = tan[i - 1];
                    Vector3f tCurr = tan[i];
                    k.getPathBezier().setLeft(pPrev.x + tPrev.x * d, pPrev.y + tPrev.y * d, pPrev.z + tPrev.z * d);
                    k.getPathBezier().setRight(pCurr.x - tCurr.x * d, pCurr.y - tCurr.y * d, pCurr.z - tCurr.z * d);
                }
                path.add(tTick, k);
            }

            ctx.getSource().sendSuccess(() -> Component.literal("[demo] created ultra-smooth orbit with " + segments + " keyframes"), false);
            saveAndPreview(ctx, level, p, path);
            return 1;
        } catch (Exception e) {
            LOGGER.error("smooth_orbit360 failed", e);
            ctx.getSource().sendFailure(Component.literal("Ultra-smooth orbit demo failed: " + e.getClass().getSimpleName() + ": " + String.valueOf(e.getMessage())));
            return 0;
        }
    }

    private static void saveAndPreview(CommandContext<CommandSourceStack> ctx, ServerLevel level, ServerPlayer p, GlobalCameraPath path) {
        try {
            GlobalCameraSavedData data = GlobalCameraSavedData.getData(level);
            ctx.getSource().sendSuccess(() -> Component.literal("[demo] saving..."), false);
            data.addPath(path);
            ctx.getSource().sendSuccess(() -> Component.literal("[demo] sending to client..."), false);
            // send to client for immediate preview (receiver=1 matches existing handler)
            // Use receiver=2 for one-shot playback (no loop)
            ServerPayloadSender.sendGlobalPath(path, p, 2);
            ctx.getSource().sendSuccess(() -> Component.literal("Created path '" + path.getId() + "' and started preview."), false);
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Demo save/preview failed: " + e.getClass().getSimpleName() + ": " + String.valueOf(e.getMessage())));
        }
    }

    private static CameraKeyframe kf(Vector3f pos, Vector3f rotDegYXZ, float fov) {
        CameraKeyframe k = new CameraKeyframe(new Vector3f(pos), new Vector3f(rotDegYXZ), fov);
        // Use BEZIER time easing for smooth start/stop
        k.setPosTimeInterpolator(TimeInterpolator.BEZIER);
        k.getPosBezier().easyInOut();
        k.setRotTimeInterpolator(TimeInterpolator.BEZIER);
        k.getRotBezier().easyInOut();
        k.setFovTimeInterpolator(TimeInterpolator.BEZIER);
        k.getFovBezier().easyInOut();
        // Default path type linear unless overridden
        return k;
    }

    private static Vector3f playerView(float yawDeg, float pitchDeg) {
        // Match ClientUtil.playerView math
        float f = pitchDeg * Mth.DEG_TO_RAD;
        float f1 = -yawDeg * Mth.DEG_TO_RAD;
        float f2 = Mth.cos(f1);
        float f3 = Mth.sin(f1);
        float f4 = Mth.cos(f);
        float f5 = Mth.sin(f);
        return new Vector3f(f3 * f4, -f5, f2 * f4).normalize();
    }

    private static Vector3f lookAtRotDeg(Vector3f from, Vector3f to) {
        Vector3f aim = new Vector3f(to).sub(from);
        float len = aim.length();
        if (len < 1e-6f) return new Vector3f(0, 0, 0);
        float horiz = Mth.sqrt(aim.x * aim.x + aim.z * aim.z);
        float pitch = (float) Math.acos(horiz / len) * Mth.RAD_TO_DEG * (aim.y < 0 ? 1 : -1);
        float yaw = (float) -(Mth.atan2(aim.x, aim.z) * Mth.RAD_TO_DEG);
        return new Vector3f(pitch, wrapYaw(yaw), 0);
    }

    private static float wrapYaw(float yaw) {
        // normalize to [-180, 180]
        yaw = yaw % 360f;
        if (yaw > 180f) yaw -= 360f;
        if (yaw < -180f) yaw += 360f;
        return yaw;
    }
    
    /**
     * Create a keyframe with enhanced smoothness settings for circular motion
     */
    private static CameraKeyframe smoothOrbitKeyframe(Vector3f pos, Vector3f rot, float fov) {
        CameraKeyframe k = new CameraKeyframe(new Vector3f(pos), new Vector3f(rot), fov);
        
        // Use BEZIER path interpolation for smooth curves
        k.setPathInterpolator(PathInterpolator.BEZIER);
        
        // Apply enhanced BEZIER time interpolation with custom curves optimized for circular motion
        k.setPosTimeInterpolator(TimeInterpolator.BEZIER);
        k.getPosBezier().easyInOut();
        
        k.setRotTimeInterpolator(TimeInterpolator.BEZIER);
        k.getRotBezier().easyInOut();
        
        k.setFovTimeInterpolator(TimeInterpolator.BEZIER);
        k.getFovBezier().easyInOut();
        
        return k;
    }
    
    /**
     * Calculate optimal bezier control distance for circular motion
     */
    private static float calculateOptimalControlDistance(float radius, int segments) {
        float theta = (float) (2 * Math.PI / segments);
        // Enhanced control distance calculation for smoother circles
        return (float) ((4.0 / 3.0) * radius * Math.tan(theta / 4.0) * 0.85f); // 0.85f factor for tighter curves
    }
}