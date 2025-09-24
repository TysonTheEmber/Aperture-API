package net.tysontheember.apertureapi.client.listener;

import net.tysontheember.apertureapi.ApertureAPI;
import net.tysontheember.apertureapi.InterpolationMath;
import net.tysontheember.apertureapi.client.Animator;
import net.tysontheember.apertureapi.client.PreviewAnimator;
import net.tysontheember.apertureapi.client.ClientUtil;
import net.tysontheember.apertureapi.client.CameraAnimIdeCache;
import net.tysontheember.apertureapi.common.animation.CameraKeyframe;
import net.tysontheember.apertureapi.common.animation.GlobalCameraPath;
import net.tysontheember.apertureapi.common.animation.PathInterpolator;
import net.tysontheember.apertureapi.common.animation.Vec3BezierController;
import net.tysontheember.apertureapi.CameraModifierManager;
import net.tysontheember.apertureapi.ICameraModifier;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;

import static net.tysontheember.apertureapi.client.CameraAnimIdeCache.*;

@Mod.EventBusSubscriber(modid = ApertureAPI.MODID, value = Dist.CLIENT)
public class OnLevelRender {
    private static final int
            X_COLOR = 0xffff1242,
            Y_COLOR = 0xff26ec45,
            Z_COLOR = 0xff0894ed,
            X_COLOR_TRANSPARENT = 0x7fff1242,
            Y_COLOR_TRANSPARENT = 0x7f26ec45,
            Z_COLOR_TRANSPARENT = 0x7f0894ed,
            SELECTED_COLOR = 0xff3e90ff,
            SELECTED_COLOR_TRANSPARENT = 0x7f3e90ff;
    private static final Vector3f
            V_CACHE_1 = new Vector3f(),
            V_CACHE_2 = new Vector3f(),
            V_CACHE_3 = new Vector3f(),
            V_CACHE_4 = new Vector3f(),
            V_CACHE_5 = new Vector3f(),
            V_CACHE_6 = new Vector3f(),
            V_CACHE_7 = new Vector3f(),
            CAMERA_CACHE = new Vector3f();
    private static final Quaternionf Q_CACHE = new Quaternionf();

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            return;
        }

        setupPlayerCamera();
        renderCameraAnimIde(event);
    }

    private static void renderCameraAnimIde(RenderLevelStageEvent event) {
        if (Animator.INSTANCE.isPlaying()) {
            return;
        }

        if ((CameraAnimIdeCache.VIEW || CameraAnimIdeCache.EDIT) && !getPath().getPoints().isEmpty()) {
            if (!ClientUtil.gamePaused()) {
                setupCameraAnimIdeCamera();
            }

            // Hide all keyframes/lines while in preview mode
            if (!ClientUtil.hideGui() && !CameraAnimIdeCache.PREVIEW) {
                renderCameraAnimIdePath(event);
            }
        }
    }

    private static void renderCameraAnimIdePath(RenderLevelStageEvent event) {
        CameraAnimIdeCache.tick();
        PoseStack poseStack = event.getPoseStack();
        PoseStack.Pose last = poseStack.last();
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        RenderSystem.enableDepthTest();

        // Get current camera position
        Vec3 p = event.getCamera().getPosition();
        CAMERA_CACHE.set(p.x, p.y, p.z);
        SelectedPoint selected = getSelectedPoint();

        // Optional debug path overlay using advanced sampler (velocity color/ticks)
        if (net.tysontheember.apertureapi.client.render.PathDebugRenderer.showVelocityColor ||
            net.tysontheember.apertureapi.client.render.PathDebugRenderer.showDirectionTicks) {
            net.tysontheember.apertureapi.client.render.PathDebugRenderer.render(getPath(), last, bufferSource, p);
        }

        // Lines pass
        renderLines(selected, bufferSource, last);
        // Filled triangles pass
        renderFilledBox(selected, bufferSource, last);
        // Quads pass
        renderQuads(selected, bufferSource, last);
        RenderSystem.disableDepthTest();
    }

private static final ICameraModifier IDE_MODIFIER = CameraModifierManager
            .createModifier(ApertureAPI.MODID + "_ide", false)
            .enableFov()
            .enablePos()
            .enableRotation()
            .enableGlobalMode();

    private static final Vector3f IDE_POS = new Vector3f();
    private static final Vector3f IDE_ROT = new Vector3f();
    private static final float[] IDE_FOV = new float[1];

    private static void setupCameraAnimIdeCamera() {
        PreviewAnimator animator = PreviewAnimator.INSTANCE;

        if (!PREVIEW) {
            IDE_MODIFIER.disable();
            return;
        }

        if (!animator.prepareCameraInfo(IDE_POS, IDE_ROT, IDE_FOV)) {
            return;
        }

        IDE_MODIFIER.enable()
                .setPos(IDE_POS.x, IDE_POS.y, IDE_POS.z)
                .setRotationYXZ(IDE_ROT)
                .setFov(IDE_FOV[0]);
    }

private static final ICameraModifier PLAYER_MODIFIER = CameraModifierManager
            .createModifier(ApertureAPI.MODID + "_player", true)
            .enableFov()
            .enablePos()
            .enableRotation()
            .enableGlobalMode();

    private static final Vector3f PLAYER_POS = new Vector3f();
    private static final Vector3f PLAYER_ROT = new Vector3f();
    private static final float[] PLAYER_FOV = new float[1];

    private static void setupPlayerCamera() {
        Animator animator = Animator.INSTANCE;

        if (!animator.isPlaying()) {
            PLAYER_MODIFIER.disable();
            return;
        }

        if (!animator.prepareCameraInfo(PLAYER_POS, PLAYER_ROT, PLAYER_FOV)) {
            return;
        }

        PLAYER_MODIFIER.enable()
                .setPos(PLAYER_POS.x, PLAYER_POS.y, PLAYER_POS.z)
                .setRotationYXZ(PLAYER_ROT)
                .setFov(PLAYER_FOV[0]);
    }

    private static void renderLines(SelectedPoint selected, MultiBufferSource.BufferSource bufferSource, PoseStack.Pose pose) {
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.LINES);
        // Path polyline
        renderTrackLine(selected, buffer, pose);
        // Bezier control link lines
        renderBezierLine(selected, buffer, pose);

        switch (getMode()) {
            case MOVE -> // Move axis lines
                    renderMoveLine(selected, buffer, pose);
            case ROTATE -> // Rotation rings
                    renderRotateGizmo(selected, buffer, pose);
        }

        // If a move plane is selected (XY/XZ/YZ), draw a bold outline for better visibility
        if (getMode() == CameraAnimIdeCache.Mode.MOVE) {
            renderMovePlaneOutline(selected, buffer, pose);
        }

        // When actively adjusting a keyframe (moving/rotating), draw a camera cone for visual FOV
        renderCameraConeDuringAdjust(selected, buffer, pose);

        renderCamera(pose, buffer);

        renderNativeCenter(pose, buffer);

        bufferSource.endBatch(RenderType.LINES);
    }

    private static void renderNativeCenter(PoseStack.Pose pose, VertexConsumer buffer) {
        if (getPath().isNativeMode()) {
            addLine(buffer, pose, getNativePos().add(0.5f, 0.1f, 0.5f, V_CACHE_3).sub(CAMERA_CACHE), getNativePos().add(-0.5f, 0.1f, 0.5f, V_CACHE_2).sub(CAMERA_CACHE), 0xffe76767);
            addLine(buffer, pose, getNativePos().add(-0.5f, 0.1f, 0.5f, V_CACHE_3).sub(CAMERA_CACHE), getNativePos().add(-0.5f, 0.1f, -0.5f, V_CACHE_2).sub(CAMERA_CACHE), 0xffe76767);
            addLine(buffer, pose, getNativePos().add(-0.5f, 0.1f, -0.5f, V_CACHE_3).sub(CAMERA_CACHE), getNativePos().add(0.5f, 0.1f, -0.5f, V_CACHE_2).sub(CAMERA_CACHE), 0xffe76767);
            addLine(buffer, pose, getNativePos().add(0.5f, 0.1f, -0.5f, V_CACHE_3).sub(CAMERA_CACHE), getNativePos().add(0.5f, 0.1f, 0.5f, V_CACHE_2).sub(CAMERA_CACHE), 0xffe76767);
            addLine(buffer, pose, getNativePos().add(0.5f, 0.1f, 0.5f, V_CACHE_3).sub(CAMERA_CACHE), getNativePos().add(-0.5f, 0.1f, -0.5f, V_CACHE_2).sub(CAMERA_CACHE), 0xffe76767);
            addLine(buffer, pose, getNativePos().add(0.5f, 0.1f, -0.5f, V_CACHE_3).sub(CAMERA_CACHE), getNativePos().add(-0.5f, 0.1f, 0.5f, V_CACHE_2).sub(CAMERA_CACHE), 0xffe76767);
            Vector3f rot = V_CACHE_4.set(0, 0.1f, -2).rotateY((180 - getNativeRot().y) * Mth.DEG_TO_RAD);
            addLine(buffer, pose, getNativePos().add(0, 0.1f, 0, V_CACHE_3).sub(CAMERA_CACHE), getNativePos().add(rot, V_CACHE_2).sub(CAMERA_CACHE), 0xff00ff00);
        }
    }

    private static void renderCamera(PoseStack.Pose pose, VertexConsumer buffer) {
        PreviewAnimator animator = PreviewAnimator.INSTANCE;
        Q_CACHE.set(0, 0, 0, 1).rotationYXZ(IDE_ROT.y, IDE_ROT.x, IDE_ROT.z);

        if (!PREVIEW) {
            if (animator.prepareCameraInfo(IDE_POS, IDE_ROT, IDE_FOV)) {
                IDE_ROT.y *= -1;
                IDE_ROT.mul(Mth.DEG_TO_RAD);

                addLine(buffer, pose, V_CACHE_1.set(-0.15f, -0.1f, -0.1f).rotate(Q_CACHE).add(IDE_POS).sub(CAMERA_CACHE), V_CACHE_2.set(0.15f, -0.1f, -0.1f).rotate(Q_CACHE).add(IDE_POS).sub(CAMERA_CACHE), 0xff000000);
                addLine(buffer, pose, V_CACHE_1.set(-0.15f, -0.1f, 0f).rotate(Q_CACHE).add(IDE_POS).sub(CAMERA_CACHE), V_CACHE_2.set(0.15f, -0.1f, 0f).rotate(Q_CACHE).add(IDE_POS).sub(CAMERA_CACHE), 0xff000000);
                addLine(buffer, pose, V_CACHE_1.set(-0.15f, -0.1f, -0.1f).rotate(Q_CACHE).add(IDE_POS).sub(CAMERA_CACHE), V_CACHE_2.set(-0.15f, -0.1f, 0f).rotate(Q_CACHE).add(IDE_POS).sub(CAMERA_CACHE), 0xff000000);
                addLine(buffer, pose, V_CACHE_1.set(0.15f, -0.1f, -0.1f).rotate(Q_CACHE).add(IDE_POS).sub(CAMERA_CACHE), V_CACHE_2.set(0.15f, -0.1f, 0f).rotate(Q_CACHE).add(IDE_POS).sub(CAMERA_CACHE), 0xff000000);

                addLine(buffer, pose, V_CACHE_1.set(-0.15f, 0.1f, -0.1f).rotate(Q_CACHE).add(IDE_POS).sub(CAMERA_CACHE), V_CACHE_2.set(0.15f, 0.1f, -0.1f).rotate(Q_CACHE).add(IDE_POS).sub(CAMERA_CACHE), 0xff000000);
                addLine(buffer, pose, V_CACHE_1.set(-0.15f, 0.1f, 0f).rotate(Q_CACHE).add(IDE_POS).sub(CAMERA_CACHE), V_CACHE_2.set(0.15f, 0.1f, 0f).rotate(Q_CACHE).add(IDE_POS).sub(CAMERA_CACHE), 0xff000000);
                addLine(buffer, pose, V_CACHE_1.set(-0.15f, 0.1f, -0.1f).rotate(Q_CACHE).add(IDE_POS).sub(CAMERA_CACHE), V_CACHE_2.set(-0.15f, 0.1f, 0f).rotate(Q_CACHE).add(IDE_POS).sub(CAMERA_CACHE), 0xff000000);
                addLine(buffer, pose, V_CACHE_1.set(0.15f, 0.1f, -0.1f).rotate(Q_CACHE).add(IDE_POS).sub(CAMERA_CACHE), V_CACHE_2.set(0.15f, 0.1f, 0f).rotate(Q_CACHE).add(IDE_POS).sub(CAMERA_CACHE), 0xff000000);

                addLine(buffer, pose, V_CACHE_1.set(0.15f, -0.1f, -0.1f).rotate(Q_CACHE).add(IDE_POS).sub(CAMERA_CACHE), V_CACHE_2.set(0.15f, 0.1f, -0.1f).rotate(Q_CACHE).add(IDE_POS).sub(CAMERA_CACHE), 0xff000000);
                addLine(buffer, pose, V_CACHE_1.set(-0.15f, -0.1f, -0.1f).rotate(Q_CACHE).add(IDE_POS).sub(CAMERA_CACHE), V_CACHE_2.set(-0.15f, 0.1f, -0.1f).rotate(Q_CACHE).add(IDE_POS).sub(CAMERA_CACHE), 0xff000000);
                addLine(buffer, pose, V_CACHE_1.set(-0.15f, -0.1f, 0f).rotate(Q_CACHE).add(IDE_POS).sub(CAMERA_CACHE), V_CACHE_2.set(-0.15f, 0.1f, 0f).rotate(Q_CACHE).add(IDE_POS).sub(CAMERA_CACHE), 0xff000000);
                addLine(buffer, pose, V_CACHE_1.set(0.15f, -0.1f, 0f).rotate(Q_CACHE).add(IDE_POS).sub(CAMERA_CACHE), V_CACHE_2.set(0.15f, 0.1f, 0f).rotate(Q_CACHE).add(IDE_POS).sub(CAMERA_CACHE), 0xff000000);

                addLine(buffer, pose, V_CACHE_1.set(0.5f, 0.28125f, 0.3f).rotate(Q_CACHE).add(IDE_POS).sub(CAMERA_CACHE), V_CACHE_2.set(-0.5f, 0.28125f, 0.3f).rotate(Q_CACHE).add(IDE_POS).sub(CAMERA_CACHE), 0xff000000);
                addLine(buffer, pose, V_CACHE_1.set(-0.5f, -0.28125f, 0.3f).rotate(Q_CACHE).add(IDE_POS).sub(CAMERA_CACHE), V_CACHE_2.set(-0.5f, 0.28125f, 0.3f).rotate(Q_CACHE).add(IDE_POS).sub(CAMERA_CACHE), 0xff000000);
                addLine(buffer, pose, V_CACHE_1.set(0.5f, -0.28125f, 0.3f).rotate(Q_CACHE).add(IDE_POS).sub(CAMERA_CACHE), V_CACHE_2.set(0.5f, 0.28125f, 0.3f).rotate(Q_CACHE).add(IDE_POS).sub(CAMERA_CACHE), 0xff000000);
                addLine(buffer, pose, V_CACHE_1.set(0.5f, -0.28125f, 0.3f).rotate(Q_CACHE).add(IDE_POS).sub(CAMERA_CACHE), V_CACHE_2.set(-0.5f, -0.28125f, 0.3f).rotate(Q_CACHE).add(IDE_POS).sub(CAMERA_CACHE), 0xff000000);

                addLine(buffer, pose, V_CACHE_1.set(0.5f, 0.28125f, 0.3f).rotate(Q_CACHE).add(IDE_POS).sub(CAMERA_CACHE), V_CACHE_2.set(0.15f, 0.1f, 0f).rotate(Q_CACHE).add(IDE_POS).sub(CAMERA_CACHE), 0xff000000);
                addLine(buffer, pose, V_CACHE_1.set(0.5f, -0.28125f, 0.3f).rotate(Q_CACHE).add(IDE_POS).sub(CAMERA_CACHE), V_CACHE_2.set(0.15f, -0.1f, 0f).rotate(Q_CACHE).add(IDE_POS).sub(CAMERA_CACHE), 0xff000000);
                addLine(buffer, pose, V_CACHE_1.set(-0.5f, 0.28125f, 0.3f).rotate(Q_CACHE).add(IDE_POS).sub(CAMERA_CACHE), V_CACHE_2.set(-0.15f, 0.1f, 0f).rotate(Q_CACHE).add(IDE_POS).sub(CAMERA_CACHE), 0xff000000);
                addLine(buffer, pose, V_CACHE_1.set(-0.5f, -0.28125f, 0.3f).rotate(Q_CACHE).add(IDE_POS).sub(CAMERA_CACHE), V_CACHE_2.set(-0.15f, -0.1f, 0f).rotate(Q_CACHE).add(IDE_POS).sub(CAMERA_CACHE), 0xff000000);
            }
        }
    }

    // Uses V_CACHE_1, V_CACHE_2, V_CACHE_3, V_CACHE_4
    private static void renderTrackLine(SelectedPoint selected, VertexConsumer buffer, PoseStack.Pose last) {
        GlobalCameraPath track = getPath();
        ArrayList<CameraKeyframe> points = track.getPoints();

        if (points.size() < 2) {
            return;
        }

        for (int i = 1, c = points.size(); i < c; i++) {
            CameraKeyframe p1 = points.get(i - 1);
            CameraKeyframe p2 = points.get(i);
            final Vector3f v1 = V_CACHE_1.set(p1.getPos()).sub(CAMERA_CACHE);
            final Vector3f v2 = V_CACHE_2.set(p2.getPos()).sub(CAMERA_CACHE);

            switch (p2.getPathInterpolator()) {
                case LINEAR -> addLine(buffer, last, v1, v2, 0xffffffff);
                case COSINE -> {
                    // cosine easing along the straight segment
                    for (int t = 1; t <= 20; t++) {
                        float raw = 0.05f * t;
                        float f = (1.0f - (float) Math.cos(Math.PI * raw)) * 0.5f;
                        V_CACHE_3.set(v1).lerp(v2, f);
                        addLine(buffer, last, (t == 1 ? v1 : V_CACHE_4), V_CACHE_3, 0xffffffff);
                        V_CACHE_4.set(V_CACHE_3);
                    }
                    addLine(buffer, last, V_CACHE_4, v2, 0xffffffff);
                }
                case SMOOTH -> {
                    Vector3f v0;
                    Vector3f v3;

                    if (i > 1) {
                        CameraKeyframe p = points.get(i - 2);
                        v0 = V_CACHE_3.set(p.getPos()).sub(CAMERA_CACHE);
                    } else {
                        v0 = v1;
                    }

                    if (i < c - 1) {
                        CameraKeyframe p = points.get(i + 1);
                        v3 = V_CACHE_4.set(p.getPos()).sub(CAMERA_CACHE);
                    } else {
                        v3 = v2;
                    }

                    addSmoothLine(buffer, last, v0, v1, v2, v3, 0xffffffff);
                }
                case CATMULL_UNIFORM, CATMULL_CENTRIPETAL, CATMULL_CHORDAL -> {
                    Vector3f v0;
                    Vector3f v3;
                    if (i > 1) {
                        CameraKeyframe p = points.get(i - 2);
                        v0 = V_CACHE_3.set(p.getPos()).sub(CAMERA_CACHE);
                    } else {
                        v0 = v1;
                    }
                    if (i < c - 1) {
                        CameraKeyframe p = points.get(i + 1);
                        v3 = V_CACHE_4.set(p.getPos()).sub(CAMERA_CACHE);
                    } else {
                        v3 = v2;
                    }
                    float alpha = switch (p2.getPathInterpolator()) {
                        case CATMULL_UNIFORM -> 0.0f;
                        case CATMULL_CHORDAL -> 1.0f;
                        default -> 0.5f;
                    };
                    addCatmullAlphaLine(buffer, last, v0, v1, v2, v3, alpha, 0xffffffff);
                }
                case BEZIER -> {
                    Vec3BezierController controller = p2.getPathBezier();
                    addBezierLine(buffer, last, v1, V_CACHE_3.set(controller.getLeft()).sub(CAMERA_CACHE), V_CACHE_4.set(controller.getRight()).sub(CAMERA_CACHE), v2, 0xffffffff);
                }
                case STEP -> addLine(buffer, last, v1, v2, 0xff7f7f7f);
            }
        }
    }

    // Uses V_CACHE_1 and V_CACHE_2
    private static void renderMoveLine(SelectedPoint selected, VertexConsumer buffer, PoseStack.Pose last) {
        int selectedTime = selected.getPointTime();

        if (selectedTime < 0) {
            return;
        }

        // When a point is selected, draw the move-axis helper lines
        Vector3f p = selected.getPosition();

        if (p == null) return;

        Vector3f pos = V_CACHE_1.set(p);

        pos.sub(CAMERA_CACHE);
        int xColor = X_COLOR,
                yColor = Y_COLOR,
                zColor = Z_COLOR;
        float lenX = 1.0f, lenY = 1.0f, lenZ = 1.0f;

        // 选中仅变粗/变长（不改颜色）
        switch (getMoveMode().getMoveType()) {
            case X -> { lenX = 1.6f; }
            case Y -> { lenY = 1.6f; }
            case Z -> { lenZ = 1.6f; }
        }

        // x轴
        Vector3f axis = V_CACHE_2.set(pos).add(lenX, 0, 0);
        addLine(buffer, last, pos, axis, xColor);
        // y轴
        axis.set(pos).add(0, lenY, 0);
        addLine(buffer, last, pos, axis, yColor);
        // z轴
        axis.set(pos).add(0, 0, lenZ);
        addLine(buffer, last, pos, axis, zColor);
    }

    // 使用vCache1、vCache2
    private static void renderBezierLine(SelectedPoint selected, VertexConsumer buffer, PoseStack.Pose last) {
        int selectedTime = selected.getPointTime();
        GlobalCameraPath track = getPath();

        if (selectedTime <= 0) {
            return;
        }

        // Render extra lines for the selected point (Bezier control links)
        CameraKeyframe selectedPoint = track.getPoint(selectedTime);

        if (selectedPoint == null || selectedPoint.getPathInterpolator() != PathInterpolator.BEZIER) {
            return;
        }

        Vec3BezierController controller = selectedPoint.getPathBezier();
        Vector3f selectedPos = V_CACHE_1.set(selectedPoint.getPos()).sub(CAMERA_CACHE);
        Vector3f right = V_CACHE_2.set(controller.getRight()).sub(CAMERA_CACHE);
        addLine(buffer, last, selectedPos, right, 0x7f98FB98);
        CameraKeyframe pre = track.getPrePoint(selectedTime);

        if (pre == null) return;

        Vector3f prePos = V_CACHE_1.set(pre.getPos()).sub(CAMERA_CACHE);
        Vector3f left = V_CACHE_2.set(controller.getLeft()).sub(CAMERA_CACHE);
        addLine(buffer, last, prePos, left, 0x7f98FB98);
    }

    private static void renderFilledBox(SelectedPoint selected, MultiBufferSource.BufferSource bufferSource, PoseStack.Pose last) {
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.debugFilledBox());
        // Camera keyframe points
        renderPoint(buffer, last);
        // Bezier control points
        renderBezierPoint(selected, buffer, last);
        // Selected keyframe
        renderSelectedPoint(selected, buffer, last);

        switch (getMode()) {
            case MOVE -> // Move arrowheads
                    renderArrowhead(selected, buffer, last);
        }

        bufferSource.endBatch(RenderType.debugFilledBox());
    }

    // Uses V_CACHE_1
    private static void renderPoint(VertexConsumer buffer, PoseStack.Pose last) {
        GlobalCameraPath track = getPath();

        for (CameraKeyframe point : track.getPoints()) {
            addPoint(buffer, last, V_CACHE_1.set(point.getPos()).sub(CAMERA_CACHE), 0.1f, 0xff000000);
        }
    }

    // Uses V_CACHE_1
    private static void renderBezierPoint(SelectedPoint selected, VertexConsumer buffer, PoseStack.Pose last) {
        int selectedTime = selected.getPointTime();

        if (selectedTime < 1) return;

        GlobalCameraPath track = getPath();
        CameraKeyframe selectedPoint = track.getPoint(selectedTime);

        if (selectedPoint == null || selectedPoint.getPathInterpolator() != PathInterpolator.BEZIER) return;

        CameraKeyframe prePoint = track.getPrePoint(selectedTime);

        if (prePoint == null) return;

        // Draw Bezier control handle markers
        Vec3BezierController controller = selectedPoint.getPathBezier();
        addPoint(buffer, last, V_CACHE_1.set(controller.getLeft()).sub(CAMERA_CACHE), 0.05f, 0x7f98FB98);
        addPoint(buffer, last, V_CACHE_1.set(controller.getRight()).sub(CAMERA_CACHE), 0.05f, 0x7f98FB98);
    }

    // 使用vCache1
    private static void renderSelectedPoint(SelectedPoint selected, VertexConsumer buffer, PoseStack.Pose last) {
        Vector3f pos = selected.getPosition();

        if (pos == null) return;

        switch (selected.getControl()) {
            case LEFT, RIGHT ->
                    addPoint(buffer, last, V_CACHE_1.set(pos).sub(CAMERA_CACHE), 0.07f, SELECTED_COLOR_TRANSPARENT);
            case NONE ->
                    addPoint(buffer, last, V_CACHE_1.set(pos).sub(CAMERA_CACHE), 0.12f, SELECTED_COLOR_TRANSPARENT);
        }
    }

    // Uses V_CACHE_1
    private static void renderArrowhead(SelectedPoint selected, VertexConsumer buffer, PoseStack.Pose last) {
        Vector3f pos = selected.getPosition();

        if (pos == null) return;

        pos = V_CACHE_1.set(pos).sub(CAMERA_CACHE);
        int xColor = X_COLOR,
                yColor = Y_COLOR,
                zColor = Z_COLOR;
        float scaleX = 1.0f, scaleY = 1.0f, scaleZ = 1.0f;

        // 选中仅放大箭头（不改颜色）
        switch (getMoveMode().getMoveType()) {
            case X -> { scaleX = 1.6f; }
            case Y -> { scaleY = 1.6f; }
            case Z -> { scaleZ = 1.6f; }
        }

        addArrowhead(buffer, last, pos, xColor, yColor, zColor, scaleX, scaleY, scaleZ);
    }

    private static void renderQuads(SelectedPoint selected, MultiBufferSource.BufferSource bufferSource, PoseStack.Pose last) {
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.debugQuads());

        switch (getMode()) {
            case NONE -> {
            }
            case MOVE -> // Translucent move planes
                    renderMoveSlice(selected, buffer, last);
            case ROTATE -> // Thick rotation band
                    renderRotateBand(selected, buffer, last);
        }

        bufferSource.endBatch(RenderType.debugQuads());
    }

    // Uses V_CACHE_1
    private static void renderMoveSlice(SelectedPoint selected, VertexConsumer buffer, PoseStack.Pose last) {
        Vector3f pos = selected.getPosition();

        if (pos == null) return;

        pos = V_CACHE_1.set(pos).sub(CAMERA_CACHE);
        int xyColor = Z_COLOR_TRANSPARENT,
                yzColor = X_COLOR_TRANSPARENT,
                xzColor = Y_COLOR_TRANSPARENT;

        // Keep default axis colors (do not switch to blue)
        switch (getMoveMode().getMoveType()) {
            case XY -> { /* keep xyColor as Z_COLOR_TRANSPARENT */ }
            case YZ -> { /* keep yzColor as X_COLOR_TRANSPARENT */ }
            case XZ -> { /* keep xzColor as Y_COLOR_TRANSPARENT */ }
        }

        addSlice(buffer, last, pos, xyColor, yzColor, xzColor);
    }

    // Uses V_CACHE_5
    private static void addLine(VertexConsumer buffer, PoseStack.Pose pose, Vector3f pos1, Vector3f pos2, int color) {
        Vector3f normalize = V_CACHE_5.set(pos2).sub(pos1).normalize();
        Matrix3f matrix3f = pose.normal();
        Matrix4f matrix4f = pose.pose();
        buffer.vertex(matrix4f, pos1.x, pos1.y, pos1.z).color(color).normal(matrix3f, normalize.x, normalize.y, normalize.z).endVertex();
        buffer.vertex(matrix4f, pos2.x, pos2.y, pos2.z).color(color).normal(matrix3f, normalize.x, normalize.y, normalize.z).endVertex();
    }

    // Uses V_CACHE_6 and V_CACHE_7
    private static void addSmoothLine(VertexConsumer buffer, PoseStack.Pose pose, Vector3f pre, Vector3f p1, Vector3f p2, Vector3f after, int color) {
        Vector3f pos1 = V_CACHE_6.set(p1);
        Vector3f pos2 = V_CACHE_7.zero();

        for (float i = 1; i <= 20; i++) {
            float f = 0.05f * i;
            InterpolationMath.catmullRom(f, pre, p1, p2, after, pos2);
            addLine(buffer, pose, pos1, pos2, color);
            pos1.set(pos2);
        }

        addLine(buffer, pose, pos2, p2, color);
    }

    // Uses V_CACHE_6 and V_CACHE_7
    private static void addCatmullAlphaLine(VertexConsumer buffer, PoseStack.Pose pose, Vector3f pre, Vector3f p1, Vector3f p2, Vector3f after, float alpha, int color) {
        Vector3f pos1 = V_CACHE_6.set(p1);
        Vector3f pos2 = V_CACHE_7.zero();
        for (int i = 1; i <= 20; i++) {
            float f = 0.05f * i;
            net.tysontheember.apertureapi.path.CatmullRom.eval(f, pre, p1, p2, after, alpha, pos2);
            addLine(buffer, pose, pos1, pos2, color);
            pos1.set(pos2);
        }
        addLine(buffer, pose, pos2, p2, color);
    }

    // Uses V_CACHE_6 and V_CACHE_7
    private static void addBezierLine(VertexConsumer buffer, PoseStack.Pose pose, Vector3f p1, Vector3f c1, Vector3f c2, Vector3f p2, int color) {
        Vector3f pos1 = V_CACHE_6.set(p1);
        Vector3f pos2 = V_CACHE_7.zero();

        for (int i = 0; i < 20; i++) {
            float t = 0.05f * i;
            InterpolationMath.bezier(t, p1, c1, c2, p2, pos2);
            addLine(buffer, pose, pos1, pos2, color);
            pos1.set(pos2);
        }

        addLine(buffer, pose, pos2, p2, color);
    }

    // Uses V_CACHE_5
    private static void addPoint(VertexConsumer buffer, PoseStack.Pose pose, Vector3f pos, float size, int color) {
        Vector3f vec = V_CACHE_5;
        Matrix4f matrix4f = pose.pose();

        vec.set(pos).add(size, -size, -size);
        buffer.vertex(matrix4f, vec.x, vec.y, vec.z).color(color).endVertex();//1
        vec.set(pos).add(size, -size, -size);
        buffer.vertex(matrix4f, vec.x, vec.y, vec.z).color(color).endVertex();//1
        vec.set(pos).add(size, -size, -size);
        buffer.vertex(matrix4f, vec.x, vec.y, vec.z).color(color).endVertex();//2
        vec.set(pos).add(-size, size, -size);
        buffer.vertex(matrix4f, vec.x, vec.y, vec.z).color(color).endVertex();//3
        vec.set(pos).add(size, size, -size);
        buffer.vertex(matrix4f, vec.x, vec.y, vec.z).color(color).endVertex();//4
        vec.set(pos).add(size, size, size);
        buffer.vertex(matrix4f, vec.x, vec.y, vec.z).color(color).endVertex();//5
        vec.set(pos).add(size, -size, -size);
        buffer.vertex(matrix4f, vec.x, vec.y, vec.z).color(color).endVertex();//6
        vec.set(pos).add(size, -size, size);
        buffer.vertex(matrix4f, vec.x, vec.y, vec.z).color(color).endVertex();//7
        vec.set(pos).add(-size, -size, size);
        buffer.vertex(matrix4f, vec.x, vec.y, vec.z).color(color).endVertex();//8
        vec.set(pos).add(size, size, size);
        buffer.vertex(matrix4f, vec.x, vec.y, vec.z).color(color).endVertex();//9
        vec.set(pos).add(-size, size, size);
        buffer.vertex(matrix4f, vec.x, vec.y, vec.z).color(color).endVertex();//10
        vec.set(pos).add(-size, size, -size);
        buffer.vertex(matrix4f, vec.x, vec.y, vec.z).color(color).endVertex();//11
        vec.set(pos).add(-size, -size, size);
        buffer.vertex(matrix4f, vec.x, vec.y, vec.z).color(color).endVertex();//12
        vec.set(pos).add(-size, -size, -size);
        buffer.vertex(matrix4f, vec.x, vec.y, vec.z).color(color).endVertex();//13
        vec.set(pos).add(size, -size, -size);
        buffer.vertex(matrix4f, vec.x, vec.y, vec.z).color(color).endVertex();//14
        vec.set(pos).add(-size, size, -size);
        buffer.vertex(matrix4f, vec.x, vec.y, vec.z).color(color).endVertex();//15
        vec.set(pos).add(-size, size, -size);
        buffer.vertex(matrix4f, vec.x, vec.y, vec.z).color(color).endVertex();//16
        vec.set(pos).add(-size, size, -size);
        buffer.vertex(matrix4f, vec.x, vec.y, vec.z).color(color).endVertex();//16
    }

    // Uses V_CACHE_5
    private static void addSlice(VertexConsumer buffer, PoseStack.Pose pose, Vector3f pos, int xyColor, int yzColor, int xzColor) {
        float spacing = 0.2f;
        float half = 0.3f + spacing;
        Vector3f vec = V_CACHE_5;
        Matrix4f matrix4f = pose.pose();
        // yz
        vec.set(pos).add(0, spacing, spacing);
        buffer.vertex(matrix4f, vec.x, vec.y, vec.z).color(yzColor).endVertex();
        vec.set(pos).add(0, spacing, half);
        buffer.vertex(matrix4f, vec.x, vec.y, vec.z).color(yzColor).endVertex();
        vec.set(pos).add(0, half, half);
        buffer.vertex(matrix4f, vec.x, vec.y, vec.z).color(yzColor).endVertex();
        vec.set(pos).add(0, half, spacing);
        buffer.vertex(matrix4f, vec.x, vec.y, vec.z).color(yzColor).endVertex();

        //xy
        vec.set(pos).add(half, half, 0);
        buffer.vertex(matrix4f, vec.x, vec.y, vec.z).color(xyColor).endVertex();
        vec.set(pos).add(half, spacing, 0);
        buffer.vertex(matrix4f, vec.x, vec.y, vec.z).color(xyColor).endVertex();
        vec.set(pos).add(spacing, spacing, 0);
        buffer.vertex(matrix4f, vec.x, vec.y, vec.z).color(xyColor).endVertex();
        vec.set(pos).add(spacing, half, 0);
        buffer.vertex(matrix4f, vec.x, vec.y, vec.z).color(xyColor).endVertex();

        //xz
        vec.set(pos).add(spacing, 0, spacing);
        buffer.vertex(matrix4f, vec.x, vec.y, vec.z).color(xzColor).endVertex();
        vec.set(pos).add(spacing, 0, half);
        buffer.vertex(matrix4f, vec.x, vec.y, vec.z).color(xzColor).endVertex();
        vec.set(pos).add(half, 0, half);
        buffer.vertex(matrix4f, vec.x, vec.y, vec.z).color(xzColor).endVertex();
        vec.set(pos).add(half, 0, spacing);
        buffer.vertex(matrix4f, vec.x, vec.y, vec.z).color(xzColor).endVertex();
    }

    // 使用vCache5
    private static void addArrowhead(VertexConsumer buffer, PoseStack.Pose pose, Vector3f pos, int xColor, int yColor, int zColor) {
        addArrowhead(buffer, pose, pos, xColor, yColor, zColor, 1.0f, 1.0f, 1.0f);
    }

    // Overload with per-axis scale to emphasize selected movement
    private static void addArrowhead(VertexConsumer buffer, PoseStack.Pose pose, Vector3f pos, int xColor, int yColor, int zColor,
                                     float scaleX, float scaleY, float scaleZ) {
        Vector3f vec = V_CACHE_5.set(pos);
        float baseSize = 0.1f;
        float baseHeight = 0.35f;
        float spacing = 1f;
        Matrix4f matrix4f = pose.pose();
        // y (apply scaleY)
        float sizeY = baseSize * scaleY;
        float heightY = baseHeight * scaleY;
        vec.add(0, spacing, 0);
        buffer.vertex(matrix4f, vec.x - sizeY, vec.y, vec.z - sizeY).color(yColor).endVertex();
        buffer.vertex(matrix4f, vec.x - sizeY, vec.y, vec.z - sizeY).color(yColor).endVertex();
        buffer.vertex(matrix4f, vec.x - sizeY, vec.y, vec.z - sizeY).color(yColor).endVertex();
        buffer.vertex(matrix4f, vec.x + sizeY, vec.y, vec.z - sizeY).color(yColor).endVertex();
        buffer.vertex(matrix4f, vec.x - sizeY, vec.y, vec.z + sizeY).color(yColor).endVertex();
        buffer.vertex(matrix4f, vec.x + sizeY, vec.y, vec.z + sizeY).color(yColor).endVertex();
        buffer.vertex(matrix4f, vec.x, vec.y + heightY, vec.z).color(yColor).endVertex();
        buffer.vertex(matrix4f, vec.x + sizeY, vec.y, vec.z - sizeY).color(yColor).endVertex();
        buffer.vertex(matrix4f, vec.x - sizeY, vec.y, vec.z - sizeY).color(yColor).endVertex();
        buffer.vertex(matrix4f, vec.x - sizeY, vec.y, vec.z + sizeY).color(yColor).endVertex();
        buffer.vertex(matrix4f, vec.x, vec.y + heightY, vec.z).color(yColor).endVertex();
        buffer.vertex(matrix4f, vec.x, vec.y + heightY, vec.z).color(yColor).endVertex();
        buffer.vertex(matrix4f, vec.x, vec.y + heightY, vec.z).color(yColor).endVertex();

        // x (apply scaleX)
        float sizeX = baseSize * scaleX;
        float heightX = baseHeight * scaleX;
        vec.add(spacing, -spacing, 0);
        buffer.vertex(matrix4f, vec.x, vec.y - sizeX, vec.z - sizeX).color(xColor).endVertex();
        buffer.vertex(matrix4f, vec.x, vec.y - sizeX, vec.z - sizeX).color(xColor).endVertex();
        buffer.vertex(matrix4f, vec.x, vec.y - sizeX, vec.z - sizeX).color(xColor).endVertex();
        buffer.vertex(matrix4f, vec.x, vec.y + sizeX, vec.z - sizeX).color(xColor).endVertex();
        buffer.vertex(matrix4f, vec.x, vec.y - sizeX, vec.z + sizeX).color(xColor).endVertex();
        buffer.vertex(matrix4f, vec.x, vec.y + sizeX, vec.z + sizeX).color(xColor).endVertex();
        buffer.vertex(matrix4f, vec.x + heightX, vec.y, vec.z).color(xColor).endVertex();
        buffer.vertex(matrix4f, vec.x, vec.y + sizeX, vec.z - sizeX).color(xColor).endVertex();
        buffer.vertex(matrix4f, vec.x, vec.y - sizeX, vec.z - sizeX).color(xColor).endVertex();
        buffer.vertex(matrix4f, vec.x, vec.y - sizeX, vec.z + sizeX).color(xColor).endVertex();
        buffer.vertex(matrix4f, vec.x + heightX, vec.y, vec.z).color(xColor).endVertex();
        buffer.vertex(matrix4f, vec.x + heightX, vec.y, vec.z).color(xColor).endVertex();
        buffer.vertex(matrix4f, vec.x + heightX, vec.y, vec.z).color(xColor).endVertex();

        // z (apply scaleZ)
        float sizeZ = baseSize * scaleZ;
        float heightZ = baseHeight * scaleZ;
        vec.add(-spacing, 0, spacing);
        buffer.vertex(matrix4f, vec.x, vec.y, vec.z + heightZ).color(zColor).endVertex();
        buffer.vertex(matrix4f, vec.x, vec.y, vec.z + heightZ).color(zColor).endVertex();
        buffer.vertex(matrix4f, vec.x, vec.y, vec.z + heightZ).color(zColor).endVertex();
        buffer.vertex(matrix4f, vec.x - sizeZ, vec.y + sizeZ, vec.z).color(zColor).endVertex();
        buffer.vertex(matrix4f, vec.x - sizeZ, vec.y - sizeZ, vec.z).color(zColor).endVertex();
        buffer.vertex(matrix4f, vec.x + sizeZ, vec.y - sizeZ, vec.z).color(zColor).endVertex();
        buffer.vertex(matrix4f, vec.x, vec.y, vec.z + heightZ).color(zColor).endVertex();
        buffer.vertex(matrix4f, vec.x + sizeZ, vec.y + sizeZ, vec.z).color(zColor).endVertex();
        buffer.vertex(matrix4f, vec.x - sizeZ, vec.y + sizeZ, vec.z).color(zColor).endVertex();
        buffer.vertex(matrix4f, vec.x + sizeZ, vec.y - sizeZ, vec.z).color(zColor).endVertex();
        buffer.vertex(matrix4f, vec.x - sizeZ, vec.y - sizeZ, vec.z).color(zColor).endVertex();
        buffer.vertex(matrix4f, vec.x - sizeZ, vec.y - sizeZ, vec.z).color(zColor).endVertex();
        buffer.vertex(matrix4f, vec.x - sizeZ, vec.y - sizeZ, vec.z).color(zColor).endVertex();
    }

    private static void renderRotateGizmo(SelectedPoint selected, VertexConsumer buffer, PoseStack.Pose last) {
        int selectedTime = selected.getPointTime();
        if (selectedTime < 0) return;
        Vector3f p = selected.getPosition();
        if (p == null) return;
        Vector3f center = V_CACHE_1.set(p).sub(CAMERA_CACHE);
        float r = 1.2f;
        int xColor = X_COLOR;
        int yColor = Y_COLOR;
        int zColor = Z_COLOR;
        addCircle(buffer, last, center, r, 'x', xColor);
        addCircle(buffer, last, center, r, 'y', yColor);
        addCircle(buffer, last, center, r, 'z', zColor);
        // Strong highlight: draw multiple concentric opaque rings on the picked axis (use axis color, not blue)
        switch (getMoveMode().getMoveType()) {
            case RX -> {
                addCircle(buffer, last, center, r - 0.05f, 'x', xColor);
                addCircle(buffer, last, center, r,          'x', xColor);
                addCircle(buffer, last, center, r + 0.05f, 'x', xColor);
            }
            case RY -> {
                addCircle(buffer, last, center, r - 0.05f, 'y', yColor);
                addCircle(buffer, last, center, r,          'y', yColor);
                addCircle(buffer, last, center, r + 0.05f, 'y', yColor);
            }
            case RZ -> {
                addCircle(buffer, last, center, r - 0.05f, 'z', zColor);
                addCircle(buffer, last, center, r,          'z', zColor);
                addCircle(buffer, last, center, r + 0.05f, 'z', zColor);
            }
        }
    }

    // Render a thick ring band for the actively used rotation axis using quads (appears bolder/thicker)
    private static void renderRotateBand(SelectedPoint selected, VertexConsumer buffer, PoseStack.Pose last) {
        switch (getMoveMode().getMoveType()) {
            case RX, RY, RZ -> {
                int selectedTime = selected.getPointTime();
                if (selectedTime < 0) return;
                Vector3f p = selected.getPosition();
                if (p == null) return;
                Vector3f center = V_CACHE_1.set(p).sub(CAMERA_CACHE);
                float r = 1.2f;
                float halfWidth = 0.06f; // visual thickness (band half-width)
                char axis;
                int color;
                switch (getMoveMode().getMoveType()) {
                    case RX -> { axis = 'x'; color = X_COLOR; }
                    case RY -> { axis = 'y'; color = Y_COLOR; }
                    default -> { axis = 'z'; color = Z_COLOR; }
                }
                addRingBand(buffer, last, center, r, halfWidth, axis, color);
            }
            default -> {}
        }
    }

    private static void addRingBand(VertexConsumer buffer, PoseStack.Pose pose, Vector3f center, float radius, float halfWidth, char axis, int color) {
        int segments = 64;
        Matrix4f m = pose.pose();
        for (int i = 0; i < segments; i++) {
            float t0 = (float) (2 * Math.PI * i / segments);
            float t1 = (float) (2 * Math.PI * (i + 1) / segments);
            // inner/outer radii points per segment
            float c0 = (float) Math.cos(t0), s0 = (float) Math.sin(t0);
            float c1 = (float) Math.cos(t1), s1 = (float) Math.sin(t1);
            // four corners of the quad (inner0, outer0, outer1, inner1)
            Vector3f i0 = new Vector3f();
            Vector3f o0 = new Vector3f();
            Vector3f o1 = new Vector3f();
            Vector3f i1 = new Vector3f();
            switch (axis) {
                case 'x' -> {
                    i0.set(0f, c0 * (radius - halfWidth), s0 * (radius - halfWidth)).add(center);
                    o0.set(0f, c0 * (radius + halfWidth), s0 * (radius + halfWidth)).add(center);
                    o1.set(0f, c1 * (radius + halfWidth), s1 * (radius + halfWidth)).add(center);
                    i1.set(0f, c1 * (radius - halfWidth), s1 * (radius - halfWidth)).add(center);
                }
                case 'y' -> {
                    i0.set(c0 * (radius - halfWidth), 0f, s0 * (radius - halfWidth)).add(center);
                    o0.set(c0 * (radius + halfWidth), 0f, s0 * (radius + halfWidth)).add(center);
                    o1.set(c1 * (radius + halfWidth), 0f, s1 * (radius + halfWidth)).add(center);
                    i1.set(c1 * (radius - halfWidth), 0f, s1 * (radius - halfWidth)).add(center);
                }
                default -> { // 'z'
                    i0.set(c0 * (radius - halfWidth), s0 * (radius - halfWidth), 0f).add(center);
                    o0.set(c0 * (radius + halfWidth), s0 * (radius + halfWidth), 0f).add(center);
                    o1.set(c1 * (radius + halfWidth), s1 * (radius + halfWidth), 0f).add(center);
                    i1.set(c1 * (radius - halfWidth), s1 * (radius - halfWidth), 0f).add(center);
                }
            }
            // emit quad
            buffer.vertex(m, i0.x, i0.y, i0.z).color(color).endVertex();
            buffer.vertex(m, o0.x, o0.y, o0.z).color(color).endVertex();
            buffer.vertex(m, o1.x, o1.y, o1.z).color(color).endVertex();
            buffer.vertex(m, i1.x, i1.y, i1.z).color(color).endVertex();
        }
    }

    private static void addCircle(VertexConsumer buffer, PoseStack.Pose last, Vector3f center, float radius, char axis, int color) {
        int segments = 48;
        Vector3f prev = new Vector3f();
        Vector3f curr = new Vector3f();
        for (int i = 0; i <= segments; i++) {
            float t = (float) (2 * Math.PI * i / segments);
            switch (axis) {
                case 'x' -> curr.set(0, (float) (Math.cos(t) * radius), (float) (Math.sin(t) * radius));
                case 'y' -> curr.set((float) (Math.cos(t) * radius), 0, (float) (Math.sin(t) * radius));
                default -> curr.set((float) (Math.cos(t) * radius), (float) (Math.sin(t) * radius), 0);
            }
            curr.add(center);
            if (i > 0) {
                addLine(buffer, last, prev, curr, color);
            }
            prev.set(curr);
        }
    }

    // Outline the selected move plane with lines for a bolder visual cue
    private static void renderMovePlaneOutline(SelectedPoint selected, VertexConsumer buffer, PoseStack.Pose last) {
        if (getMoveMode().getMoveType() != CameraAnimIdeCache.MoveType.XY &&
            getMoveMode().getMoveType() != CameraAnimIdeCache.MoveType.XZ &&
            getMoveMode().getMoveType() != CameraAnimIdeCache.MoveType.YZ) {
            return;
        }
        Vector3f pos = selected.getPosition();
        if (pos == null) return;
        Vector3f base = V_CACHE_5.set(pos).sub(CAMERA_CACHE);
        float spacing = 0.2f;
        float half = 0.3f + spacing;
        // Build rectangle corners for the selected plane in the plane's axis color (not blue)
        switch (getMoveMode().getMoveType()) {
            case XY -> {
                int color = Z_COLOR;
                Vector3f a = new Vector3f(base).add(spacing, spacing, 0);
                Vector3f b = new Vector3f(base).add(half,    spacing, 0);
                Vector3f c = new Vector3f(base).add(half,    half,    0);
                Vector3f d = new Vector3f(base).add(spacing, half,    0);
                addLine(buffer, last, a, b, color);
                addLine(buffer, last, b, c, color);
                addLine(buffer, last, c, d, color);
                addLine(buffer, last, d, a, color);
            }
            case XZ -> {
                int color = Y_COLOR;
                Vector3f a = new Vector3f(base).add(spacing, 0, spacing);
                Vector3f b = new Vector3f(base).add(half,    0, spacing);
                Vector3f c = new Vector3f(base).add(half,    0, half);
                Vector3f d = new Vector3f(base).add(spacing, 0, half);
                addLine(buffer, last, a, b, color);
                addLine(buffer, last, b, c, color);
                addLine(buffer, last, c, d, color);
                addLine(buffer, last, d, a, color);
            }
            case YZ -> {
                int color = X_COLOR;
                Vector3f a = new Vector3f(base).add(0, spacing, spacing);
                Vector3f b = new Vector3f(base).add(0, spacing, half);
                Vector3f c = new Vector3f(base).add(0, half,    half);
                Vector3f d = new Vector3f(base).add(0, half,    spacing);
                addLine(buffer, last, a, b, color);
                addLine(buffer, last, b, c, color);
                addLine(buffer, last, c, d, color);
                addLine(buffer, last, d, a, color);
            }
        }
    }
    // Draw a simple camera frustum (cone/pyramid) for the selected keyframe while actively adjusting it
    private static void renderCameraConeDuringAdjust(SelectedPoint selected, VertexConsumer buffer, PoseStack.Pose pose) {
        int selectedTime = selected.getPointTime();
        if (selectedTime < 0) return;

        // Only when actively adjusting (dragging move/rotate), and only for the main keyframe (not Bezier handles)
        switch (getMoveMode().getMoveType()) {
            case NONE -> { return; }
            default -> {}
        }
        switch (selected.getControl()) {
            case NONE -> {}
            default -> { return; }
        }

        CameraKeyframe kf = getPath().getPoint(selectedTime);
        if (kf == null) return;

        // Orientation from keyframe rotation (degrees -> radians), match existing YXZ order and yaw inversion
        Vector3f rotDeg = kf.getRot();
        float yaw = -rotDeg.y * Mth.DEG_TO_RAD;
        float pitch = rotDeg.x * Mth.DEG_TO_RAD;
        float roll = rotDeg.z * Mth.DEG_TO_RAD;
        Q_CACHE.set(0, 0, 0, 1).rotationYXZ(yaw, pitch, roll);

        // Frustum geometry in local camera space: apex at origin, square base at +Z
        float length = 2.0f;
        float r = (float) java.lang.Math.tan((kf.getFov() * 0.5f) * Mth.DEG_TO_RAD) * length;

        Vector3f apex = V_CACHE_1.set(0f, 0f, 0f).rotate(Q_CACHE).add(kf.getPos()).sub(CAMERA_CACHE);
        Vector3f c1 = V_CACHE_2.set(-r, -r, length).rotate(Q_CACHE).add(kf.getPos()).sub(CAMERA_CACHE);
        Vector3f c2 = V_CACHE_3.set( r, -r, length).rotate(Q_CACHE).add(kf.getPos()).sub(CAMERA_CACHE);
        Vector3f c3 = V_CACHE_4.set( r,  r, length).rotate(Q_CACHE).add(kf.getPos()).sub(CAMERA_CACHE);
        Vector3f c4 = new Vector3f(-r,  r, length).rotate(Q_CACHE).add(kf.getPos()).sub(CAMERA_CACHE);

        int color = 0xff000000; // neutral (black) highlight while adjusting (not blue)
        // Edges from apex
        addLine(buffer, pose, apex, c1, color);
        addLine(buffer, pose, apex, c2, color);
        addLine(buffer, pose, apex, c3, color);
        addLine(buffer, pose, apex, c4, color);
        // Base perimeter
        addLine(buffer, pose, c1, c2, color);
        addLine(buffer, pose, c2, c3, color);
        addLine(buffer, pose, c3, c4, color);
        addLine(buffer, pose, c4, c1, color);
    }
}

