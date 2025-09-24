package net.tysontheember.apertureapi.client;

import net.tysontheember.apertureapi.common.animation.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import org.joml.Math;
import net.minecraft.util.Mth;

import static net.tysontheember.apertureapi.client.ClientUtil.*;

public class CameraAnimIdeCache {
    public static final float POINT_PICK_EXPAND = 0.2f;
    public static boolean EDIT;
    public static boolean VIEW;
    public static boolean PREVIEW;
    private static Mode MODE = Mode.MOVE;
    private static final MoveModeData MOVE_DATA = new MoveModeData();
    private static GlobalCameraPath PATH = new GlobalCameraPath("new");
    private static final SelectedPoint SELECTED_POINT = new SelectedPoint();
    private static final float BEZIER_PICK_EXPAND = 0.1f;

    private static final Vector3f NATIVE_POS = new Vector3f();
    private static final Vector3f NATIVE_ROT = new Vector3f();

    /*static {
        // Test data (disabled)
        TRACK = new GlobalCameraPath("test");
        TRACK.add(new CameraKeyframe(new Vector3f(1, 56, 3), new Vector3f(), 70, PathInterpolator.LINEAR));
        TRACK.add(new CameraKeyframe(new Vector3f(3, 56, 5), new Vector3f(), 70, PathInterpolator.LINEAR));
        TRACK.add(new CameraKeyframe(new Vector3f(7, 56, 8), new Vector3f(), 70, PathInterpolator.LINEAR));
        TRACK.add(new CameraKeyframe(new Vector3f(5, 56, 0), new Vector3f(), 70, PathInterpolator.LINEAR));

        TRACK.add(new CameraKeyframe(new Vector3f(1, 58, 3), new Vector3f(), 70, PathInterpolator.SMOOTH));
        TRACK.add(new CameraKeyframe(new Vector3f(3, 58, 5), new Vector3f(), 70, PathInterpolator.SMOOTH));
        TRACK.add(new CameraKeyframe(new Vector3f(5, 58, 0), new Vector3f(), 70, PathInterpolator.SMOOTH));
        TRACK.add(new CameraKeyframe(new Vector3f(7, 58, 8), new Vector3f(), 70, PathInterpolator.SMOOTH));

        TRACK.add(new CameraKeyframe(new Vector3f(1, 59, 3), new Vector3f(), 70, PathInterpolator.STEP));
        TRACK.add(new CameraKeyframe(new Vector3f(3, 59, 5), new Vector3f(), 70, PathInterpolator.STEP));
        TRACK.add(new CameraKeyframe(new Vector3f(5, 59, 0), new Vector3f(), 70, PathInterpolator.STEP));
        TRACK.add(new CameraKeyframe(new Vector3f(7, 59, 8), new Vector3f(), 70, PathInterpolator.STEP));

        CameraKeyframe b1 = new CameraKeyframe(new Vector3f(1, 60, 3), new Vector3f(), 70, PathInterpolator.BEZIER);
        CameraKeyframe b2 = new CameraKeyframe(new Vector3f(3, 60, 5), new Vector3f(), 70, PathInterpolator.BEZIER);
        CameraKeyframe b3 = new CameraKeyframe(new Vector3f(5, 60, 0), new Vector3f(), 70, PathInterpolator.BEZIER);
        CameraKeyframe b4 = new CameraKeyframe(new Vector3f(7, 60, 8), new Vector3f(), 70, PathInterpolator.BEZIER);
        TRACK.add(b1);
        TRACK.add(b2);
        TRACK.add(b3);
        TRACK.add(b4);
        b1.getPathBezier().getRight().add(0, 1, 0);
        b2.getPathBezier().getRight().add(0, -1, 0);
        b3.getPathBezier().getRight().add(1, 0, 1);
        b4.getPathBezier().getRight().add(-1, 1, -1);
        b4.setPosTimeInterpolator(TimeInterpolator.BEZIER);
        b4.getPosBezier().easyInOut();
    }*/

    // Per-tick update
    public static void tick() {
        if (MODE == Mode.MOVE || MODE == Mode.ROTATE) {
            MOVE_DATA.move();
        }
    }

    public static void leftPick(Vector3f origin, Vector3f direction, float length) {
        if ((MODE == Mode.MOVE || MODE == Mode.ROTATE) && MOVE_DATA.moveType != MoveType.NONE) {
            return;
        }

        // First try to pick rotation ring in ROTATE mode
        if (MODE == Mode.ROTATE && MOVE_DATA.pickRotateModule(origin, direction)) {
            return;
        }

        length += 0.1f;
        length *= length;

        if (MODE == Mode.MOVE && MOVE_DATA.pickMoveModule(origin, direction, true)) return;
        length = pickBezier(length, origin, direction);
        pickPoint(length, origin, direction);
    }

    public static void rightPick(Vector3f origin, Vector3f direction, float length) {
        if (MODE == Mode.MOVE && MOVE_DATA.moveType != MoveType.NONE) {
            return;
        }

        if (SELECTED_POINT.pointTime < 0) {
            return;
        }

        // Right-click is reserved for move/drag only
        if (MODE == Mode.MOVE) {
            MOVE_DATA.pickMoveModule(origin, direction, false);
        }
    }

    private static float pickBezier(float length, Vector3f origin, Vector3f direction) {
        int selectedTime = SELECTED_POINT.getPointTime();

        if (selectedTime <= 0) {
            return length;
        }
        // Check if we are hitting a Bezier control point
        CameraKeyframe point = PATH.getPoint(selectedTime);

        if (point == null || point.getPathInterpolator() != PathInterpolator.BEZIER) {
            return length;
        }

        Vec3BezierController controller = point.getPathBezier();
        Vector3f right = controller.getRight();
        float rightL = right.distanceSquared(origin);

        if (rightL <= length && Intersectionf.testRayAab(origin, direction, new Vector3f(right).sub(BEZIER_PICK_EXPAND, BEZIER_PICK_EXPAND, BEZIER_PICK_EXPAND), new Vector3f(right).add(BEZIER_PICK_EXPAND, BEZIER_PICK_EXPAND, BEZIER_PICK_EXPAND))) {
            length = rightL;
            SELECTED_POINT.setControl(ControlType.RIGHT);
        }

        CameraKeyframe pre = PATH.getPrePoint(selectedTime);

        if (pre == null) {
            return length;
        }

        Vector3f left = controller.getLeft();
        float leftL = left.distanceSquared(origin);

        if (leftL <= length && Intersectionf.testRayAab(origin, direction, new Vector3f(left).sub(BEZIER_PICK_EXPAND, BEZIER_PICK_EXPAND, BEZIER_PICK_EXPAND), new Vector3f(left).add(BEZIER_PICK_EXPAND, BEZIER_PICK_EXPAND, BEZIER_PICK_EXPAND))) {
            length = leftL;
            SELECTED_POINT.setControl(ControlType.LEFT);
        }

        return length;
    }

    private static void pickPoint(float length, Vector3f origin, Vector3f direction) {
        int time = -1;

        for (Int2ObjectMap.Entry<CameraKeyframe> entry : PATH.getEntries()) {
            Vector3f position = entry.getValue().getPos();
            float d = position.distanceSquared(origin);

            if (d > length) {
                continue;
            }

            if (Intersectionf.testRayAab(origin, direction, new Vector3f(position).sub(POINT_PICK_EXPAND, POINT_PICK_EXPAND, POINT_PICK_EXPAND), new Vector3f(position).add(POINT_PICK_EXPAND, POINT_PICK_EXPAND, POINT_PICK_EXPAND))) {
                if (!(d < length)) {
                    continue;
                }

                length = d;
                time = entry.getIntKey();
            }
        }

        if (time >= 0) {
            SELECTED_POINT.setSelected(time);
        }
    }

    public static GlobalCameraPath getPath() {
        return PATH;
    }

    public static void setPath(GlobalCameraPath path) {
        PATH = path;
        SELECTED_POINT.reset();
    }

    public static SelectedPoint getSelectedPoint() {
        return SELECTED_POINT;
    }

    public static Mode getMode() {
        return MODE;
    }

    public static MoveModeData getMoveMode() {
        return MOVE_DATA;
    }

    public static void setNative(Vector3f pos, Vector3f rot) {
        NATIVE_POS.set(pos);
        NATIVE_ROT.set(rot);
        PATH.setNativeMode(true);
    }

    public static Vector3f getNativePos() {
        return NATIVE_POS;
    }

    public static Vector3f getNativeRot() {
        return NATIVE_ROT;
    }

    public static class SelectedPoint {
        private int pointTime = -1;
        private ControlType control = ControlType.NONE;

        public void setSelected(int time) {
            this.pointTime = time;
            control = ControlType.NONE;
        }

        private void setControl(ControlType control) {
            this.control = control;
        }

        public int getPointTime() {
            return pointTime;
        }

        public ControlType getControl() {
            return control;
        }

        @Nullable
        public Vector3f getPosition() {
            Vector3f pos = null;

            switch (control) {
                case LEFT -> {
                    CameraKeyframe point = PATH.getPoint(pointTime);
                    if (point == null) break;
                    pos = point.getPathBezier().getLeft();
                }
                case RIGHT -> {
                    CameraKeyframe point = PATH.getPoint(pointTime);
                    if (point == null) break;
                    pos = point.getPathBezier().getRight();
                }
                case NONE -> {
                    CameraKeyframe point = PATH.getPoint(pointTime);
                    if (point == null) break;
                    pos = point.getPos();
                }
            }

            return pos;
        }

        public void reset() {
            pointTime = -1;
            control = ControlType.NONE;
        }
    }

    public enum ControlType {
        LEFT,
        RIGHT,
        NONE
    }

    public enum Mode {
        NONE,
        MOVE,
        ROTATE
    }

    public static class MoveModeData {
        private MoveType moveType = MoveType.NONE;
        private final Vector3f delta = new Vector3f();
        private float startAngle;
        private float rotStartX;
        private float rotStartY;
        private float rotStartZ;
        private float currentDeltaDeg;
        private char currentAxis = '\0';

        private MoveModeData() {
        }

        public MoveType getMoveType() {
            return moveType;
        }

        public void reset() {
            moveType = MoveType.NONE;
        }

        private boolean pickMoveModule(Vector3f origin, Vector3f direction, boolean leftClick) {
            int selectedTime = SELECTED_POINT.getPointTime();

            if (selectedTime < 0) {
                return false;
            }

            if (leftClick) {
                Vector3f pos = SELECTED_POINT.getPosition();

                if (pos == null) return false;

                float deadZone = 0.1f,
                        l = 0.9f + 0.35f,
                        w = 0.1f;

                // 快速检测是否在范围内
                if (!Intersectionf.testRayAab(origin, direction, new Vector3f(pos).sub(w, w, 0), new Vector3f(pos).add(deadZone + l, deadZone + l, deadZone + l))) {
                    return false;
                }

                float min = Float.MAX_VALUE;
                Vector2f resultPos = new Vector2f();
                boolean result = false;

                // x
                if (Intersectionf.intersectRayAab(origin, direction, new Vector3f(pos).add(deadZone, -w, -w), new Vector3f(pos).add(deadZone + l, w, w), resultPos)) {
                    min = resultPos.x;
                    result = true;
                    MOVE_DATA.moveType = MoveType.X;
                }

                // y
                if (Intersectionf.intersectRayAab(origin, direction, new Vector3f(pos).add(-w, deadZone, -w), new Vector3f(pos).add(w, deadZone + l, w), resultPos) && min > resultPos.x) {
                    min = resultPos.x;
                    result = true;
                    MOVE_DATA.moveType = MoveType.Y;
                }

                // z
                if (Intersectionf.intersectRayAab(origin, direction, new Vector3f(pos).add(-w, -w, deadZone), new Vector3f(pos).add(w, w, deadZone + l), resultPos) && min > resultPos.x) {
                    min = resultPos.x;
                    result = true;
                    MOVE_DATA.moveType = MoveType.Z;
                }

                float spacing = 0.2f,
                        size = 0.3f;
                // xy
                if (Intersectionf.intersectRayAab(origin, direction, new Vector3f(pos).add(spacing, spacing, -w), new Vector3f(pos).add(size + spacing, size + spacing, w), resultPos) && min > resultPos.x) {
                    min = resultPos.x;
                    result = true;
                    MOVE_DATA.moveType = MoveType.XY;
                }

                // xz
                if (Intersectionf.intersectRayAab(origin, direction, new Vector3f(pos).add(spacing, -w, spacing), new Vector3f(pos).add(size + spacing, w, size + spacing), resultPos) && min > resultPos.x) {
                    min = resultPos.x;
                    result = true;
                    MOVE_DATA.moveType = MoveType.XZ;
                }

                // yz
                if (Intersectionf.intersectRayAab(origin, direction, new Vector3f(pos).add(-w, spacing, spacing), new Vector3f(pos).add(w, size + spacing, size + spacing), resultPos) && min > resultPos.x) {
                    min = resultPos.x;
                    result = true;
                    MOVE_DATA.moveType = MoveType.YZ;
                }


                MOVE_DATA.delta.set(direction).mul(min).add(origin).sub(pos).mul(-1);
                return result;
            } else {
                Vector3f pos, min, max;

                switch (SELECTED_POINT.control) {
                    case LEFT -> {
                        CameraKeyframe point = PATH.getPoint(SELECTED_POINT.pointTime);
                        if (point == null) return false;
                        pos = point.getPathBezier().getLeft();
                        min = new Vector3f(pos).sub(BEZIER_PICK_EXPAND, BEZIER_PICK_EXPAND, BEZIER_PICK_EXPAND);
                        max = new Vector3f(pos).add(BEZIER_PICK_EXPAND, BEZIER_PICK_EXPAND, BEZIER_PICK_EXPAND);
                    }
                    case RIGHT -> {
                        CameraKeyframe point = PATH.getPoint(SELECTED_POINT.pointTime);
                        if (point == null) return false;
                        pos = point.getPathBezier().getRight();
                        min = new Vector3f(pos).sub(BEZIER_PICK_EXPAND, BEZIER_PICK_EXPAND, BEZIER_PICK_EXPAND);
                        max = new Vector3f(pos).add(BEZIER_PICK_EXPAND, BEZIER_PICK_EXPAND, BEZIER_PICK_EXPAND);
                    }
                    case NONE -> {
                        CameraKeyframe point = PATH.getPoint(SELECTED_POINT.pointTime);
                        if (point == null) return false;
                        pos = point.getPos();
                        min = new Vector3f(pos).sub(POINT_PICK_EXPAND, POINT_PICK_EXPAND, POINT_PICK_EXPAND);
                        max = new Vector3f(pos).add(POINT_PICK_EXPAND, POINT_PICK_EXPAND, POINT_PICK_EXPAND);
                    }
                    default -> {
                        return false;
                    }
                }

                Vector2f resultPos = new Vector2f();

                if (Intersectionf.intersectRayAab(origin, direction, min, max, resultPos)) {
                    MOVE_DATA.delta.set(resultPos.x);
                    MOVE_DATA.moveType = MoveType.XYZ;
                    return true;
                } else {
                    return false;
                }
            }
        }

        private boolean pickRotateModule(Vector3f origin, Vector3f direction) {
            int selectedTime = SELECTED_POINT.getPointTime();
            if (selectedTime < 0) return false;
            CameraKeyframe point = PATH.getPoint(selectedTime);
            if (point == null) return false;
            Vector3f center = point.getPos();
            float r = 1.2f;
            // Match clickable thickness to rendered band (halfWidth ~0.06). Picking feels better slightly larger than render.
            float ringHalfWidth = 0.06f;
            float pickHalfWidth = ringHalfWidth * 2.5f + 0.02f; // widen pick to roughly align with perceived band thickness
            float inner = r - pickHalfWidth;
            float outer = r + pickHalfWidth;
            float bestT = Float.MAX_VALUE;
            MoveType best = MoveType.NONE;
            float angleAtPick = 0f;

            // Helper to test ring in plane with normal (1,0,0) etc. (accept both sides of plane)
            java.util.function.BiFunction<Vector3f, Character, Float> testAxis = (c, axis) -> {
                float nx = axis == 'x' ? 1f : 0f;
                float ny = axis == 'y' ? 1f : 0f;
                float nz = axis == 'z' ? 1f : 0f;
                float t = Intersectionf.intersectRayPlane(origin.x, origin.y, origin.z, direction.x, direction.y, direction.z,
                        center.x, center.y, center.z, nx, ny, nz, 1e-6f);
                if (Float.isNaN(t) || Float.isInfinite(t)) return Float.NEGATIVE_INFINITY;
                Vector3f hit = new Vector3f(direction).mul(t).add(origin).sub(center);
                // distance within plane
                float d = (axis == 'x') ? (float) Math.sqrt(hit.y * hit.y + hit.z * hit.z)
                        : (axis == 'y') ? (float) Math.sqrt(hit.x * hit.x + hit.z * hit.z)
                        : (float) Math.sqrt(hit.x * hit.x + hit.y * hit.y);
                // accept within band [inner, outer]
                if (d >= inner && d <= outer) {
                    return t; // return signed t; use abs for ordering below
                }
                return Float.NEGATIVE_INFINITY;
            };

            Float tx = testAxis.apply(center, 'x');
            Float ty = testAxis.apply(center, 'y');
            Float tz = testAxis.apply(center, 'z');

            // Prefer the axis with the smallest radial deviation from r; tie-breaker by |t|
            class Candidate { float t; float dev; MoveType type; Candidate(float t, float dev, MoveType type){this.t=t; this.dev=dev; this.type=type;} }
            java.util.ArrayList<Candidate> cands = new java.util.ArrayList<>(3);
            if (tx != Float.NEGATIVE_INFINITY) {
                Vector3f hit = new Vector3f(direction).mul(tx).add(origin).sub(center);
                float d = (float) Math.sqrt(hit.y * hit.y + hit.z * hit.z);
                cands.add(new Candidate(tx, Math.abs(d - r), MoveType.RX));
            }
            if (ty != Float.NEGATIVE_INFINITY) {
                Vector3f hit = new Vector3f(direction).mul(ty).add(origin).sub(center);
                float d = (float) Math.sqrt(hit.x * hit.x + hit.z * hit.z);
                cands.add(new Candidate(ty, Math.abs(d - r), MoveType.RY));
            }
            if (tz != Float.NEGATIVE_INFINITY) {
                Vector3f hit = new Vector3f(direction).mul(tz).add(origin).sub(center);
                float d = (float) Math.sqrt(hit.x * hit.x + hit.y * hit.y);
                cands.add(new Candidate(tz, Math.abs(d - r), MoveType.RZ));
            }
            Candidate bestCand = null;
            for (Candidate c : cands) {
                if (bestCand == null || c.dev < bestCand.dev || (c.dev == bestCand.dev && Math.abs(c.t) < Math.abs(bestCand.t))) {
                    bestCand = c;
                }
            }
            if (bestCand == null) return false;
            bestT = bestCand.t;
            best = bestCand.type;

            if (best == MoveType.NONE) return false;

            // compute start angle and cache current rot
            Vector3f hit = new Vector3f(direction).mul(bestT).add(origin).sub(center);
            switch (best) {
                case RX -> { startAngle = (float) Math.toDegrees(Math.atan2(hit.z, hit.y)); currentAxis = 'X'; }
                case RY -> { startAngle = (float) Math.toDegrees(Math.atan2(hit.x, hit.z)); currentAxis = 'Y'; }
                case RZ -> { startAngle = (float) Math.toDegrees(Math.atan2(hit.y, hit.x)); currentAxis = 'Z'; }
            }
            currentDeltaDeg = 0f;
            rotStartX = point.getRot().x;
            rotStartY = point.getRot().y;
            rotStartZ = point.getRot().z;
            moveType = best;
            return true;
        }

        private void move() {
            if (SELECTED_POINT.pointTime < 0 || moveType == MoveType.NONE) {
                return;
            }

            Vector3f pos = SELECTED_POINT.getPosition();

            if (pos == null) return;

            Vector3f view = playerView();
            Vector3f origin = playerEyePos();
            float yRot = playerYHeadRot();
            float xRot = playerXRot();

            /// Movement algorithm overview:
            /// - Cast a ray from the camera view
            /// - Intersect with the corresponding constraint plane(s) for the selected axis/plane
            /// - Use the intersection point plus the cached delta to compute the new target position
            switch (moveType) {
                case X -> {
                    float a = Intersectionf.intersectRayPlane(
                            origin.x, origin.y, origin.z,
                            view.x, view.y, view.z,
                            pos.x, pos.y, pos.z,
                            0, xRot < 0 ? -1 : 1, 0,
                            1e-6f
                    );

                    float b = Intersectionf.intersectRayPlane(
                            origin.x, origin.y, origin.z,
                            view.x, view.y, view.z,
                            pos.x, pos.y, pos.z,
                            0, 0, Math.abs(yRot) >= 90 ? 1 : -1,
                            1e-6f
                    );

                    float t = (a == 0) ? b : (b == 0) ? a : Math.min(a, b);

                    if (t < 0) {
                        return;
                    }

                    t = Math.clamp(0, 100, t);
                    pos.x = view.x * t + origin.x + delta.x;
                }
                case Y -> {
                    float t = Intersectionf.intersectRayPlane(
                            origin.x, origin.y, origin.z,
                            view.x, view.y, view.z,
                            pos.x, pos.y, pos.z,
                            yRot < 0 ? -1 : 1, 0, 0,
                            1e-6f
                    );

                    if (t < 0) {
                        return;
                    }

                    t = Math.clamp(0, 100, t);
                    pos.y = view.y * t + origin.y + delta.y;
                }
                case Z -> {
                    float a = Intersectionf.intersectRayPlane(
                            origin.x, origin.y, origin.z,
                            view.x, view.y, view.z,
                            pos.x, pos.y, pos.z,
                            0, xRot < 0 ? -1 : 1, 0,
                            1e-6f
                    );

                    float b = Intersectionf.intersectRayPlane(
                            origin.x, origin.y, origin.z,
                            view.x, view.y, view.z,
                            pos.x, pos.y, pos.z,
                            yRot >= 0 ? 1 : -1, 0, 0,
                            1e-6f
                    );

                    float t = (a == 0) ? b : (b == 0) ? a : Math.min(a, b);

                    if (t < 0) {
                        return;
                    }

                    t = Math.clamp(0, 100, t);
                    pos.z = view.z * t + origin.z + delta.z;
                }
                case XY -> {
                    float t = Intersectionf.intersectRayPlane(
                            origin.x, origin.y, origin.z,
                            view.x, view.y, view.z,
                            pos.x, pos.y, pos.z,
                            0, 0, Math.abs(yRot) < 90 ? -1 : 1,
                            1e-6f
                    );

                    if (t < 0) {
                        return;
                    }

                    t = Math.clamp(0, 100, t);
                    pos.x = view.x * t + origin.x + delta.x;
                    pos.y = view.y * t + origin.y + delta.y;
                }
                case XZ -> {
                    float t = Intersectionf.intersectRayPlane(
                            origin.x, origin.y, origin.z,
                            view.x, view.y, view.z,
                            pos.x, pos.y, pos.z,
                            0, xRot < 0 ? -1 : 1, 0,
                            1e-6f
                    );

                    if (t < 0) {
                        return;
                    }

                    t = Math.clamp(0, 100, t);
                    pos.x = view.x * t + origin.x + delta.x;
                    pos.z = view.z * t + origin.z + delta.z;
                }
                case YZ -> {
                    float t = Intersectionf.intersectRayPlane(
                            origin.x, origin.y, origin.z,
                            view.x, view.y, view.z,
                            pos.x, pos.y, pos.z,
                            yRot < 0 ? -1 : 1, 0, 0,
                            1e-6f
                    );

                    if (t < 0) {
                        return;
                    }

                    t = Math.clamp(0, 100, t);
                    pos.z = view.z * t + origin.z + delta.z;
                    pos.y = view.y * t + origin.y + delta.y;
                }
                case XYZ -> pos.set(view).mul(delta.x).add(origin);
                case RX, RY, RZ -> {
                    // Compute angle on plane and set rotation component accordingly
                    CameraKeyframe point = PATH.getPoint(SELECTED_POINT.getPointTime());
                    if (point == null) return;
                    Vector3f center = point.getPos();
                    // Intersect with plane corresponding to axis
                    float nx = (moveType == MoveType.RX) ? 1f : 0f;
                    float ny = (moveType == MoveType.RY) ? 1f : 0f;
                    float nz = (moveType == MoveType.RZ) ? 1f : 0f;
                    float t = Intersectionf.intersectRayPlane(
                            origin.x, origin.y, origin.z,
                            view.x, view.y, view.z,
                            center.x, center.y, center.z,
                            nx, ny, nz,
                            1e-6f
                    );
                    if (Float.isNaN(t) || Float.isInfinite(t)) return;
                    Vector3f hit = new Vector3f(view).mul(t).add(origin).sub(center);
                    float currentAngleDeg;
                    switch (moveType) {
                        case RX -> currentAngleDeg = (float) Math.toDegrees(Math.atan2(hit.z, hit.y));
                        case RY -> currentAngleDeg = (float) Math.toDegrees(Math.atan2(hit.x, hit.z));
                        default -> currentAngleDeg = (float) Math.toDegrees(Math.atan2(hit.y, hit.x));
                    }
                    float deltaAng = Mth.wrapDegrees(currentAngleDeg - startAngle);
                    // Snapping: Shift=5°, Alt=1°, Shift+Alt=0.5°
                    float snap = 0f;
                    boolean sh = net.tysontheember.apertureapi.client.ClientUtil.shiftDown();
                    boolean al = net.tysontheember.apertureapi.client.ClientUtil.altDown();
                    if (sh && al) snap = 0.5f; else if (sh) snap = 5f; else if (al) snap = 1f;
                    if (snap > 0f) {
                        deltaAng = Math.round(deltaAng / snap) * snap;
                    }
                    currentDeltaDeg = deltaAng;
                    Vector3f rot = point.getRot();
                    switch (moveType) {
                        case RX -> rot.x = Mth.wrapDegrees(rotStartX + deltaAng);
                        case RY -> rot.y = Mth.wrapDegrees(rotStartY + deltaAng);
                        case RZ -> rot.z = Mth.wrapDegrees(rotStartZ + deltaAng);
                    }
                }
            }
        }
    }

    public static void toggleMode() {
        MODE = (MODE == Mode.ROTATE) ? Mode.MOVE : Mode.ROTATE;
    }

    public static boolean isRotating() {
        return MODE == Mode.ROTATE && MOVE_DATA.moveType == MoveType.RX || MOVE_DATA.moveType == MoveType.RY || MOVE_DATA.moveType == MoveType.RZ;
    }

    public static float getCurrentRotateDelta() {
        return MOVE_DATA.currentDeltaDeg;
    }

    public static char getCurrentRotateAxis() {
        return MOVE_DATA.currentAxis;
    }

    public enum MoveType {
        X,
        Y,
        Z,
        XY,
        XZ,
        YZ,
        XYZ,
        RX,
        RY,
        RZ,
        NONE
    }
}


