package net.tysontheember.apertureapi.path;

import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Orientation helpers: yaw/pitch/roll conversions and quaternion slerp.
 */
public final class OrientationUtil {
    private OrientationUtil() {}

    /** Convert yaw/pitch/roll in degrees to a quaternion with YXZ order. */
    public static Quaternionf yprDegToQuat(float yawDeg, float pitchDeg, float rollDeg, Quaternionf out) {
        float yaw = -yawDeg * Mth.DEG_TO_RAD;   // match existing convention
        float pitch = pitchDeg * Mth.DEG_TO_RAD;
        float roll = rollDeg * Mth.DEG_TO_RAD;
        return out.identity().rotationYXZ(yaw, pitch, roll);
    }

    /** Convert quaternion to yaw/pitch/roll degrees with YXZ order. */
    public static Vector3f quatToYprDeg(Quaternionf q, Vector3f outDeg) {
        // Extract Euler YXZ from quaternion
        // JOML can compute Euler angles via getEulerAnglesYXZ
        Vector3f rad = new Vector3f();
        q.getEulerAnglesYXZ(rad);
        float yawDeg = -rad.y * Mth.RAD_TO_DEG; // invert to match convention
        float pitchDeg = rad.x * Mth.RAD_TO_DEG;
        float rollDeg = rad.z * Mth.RAD_TO_DEG;
        return outDeg.set(pitchDeg, yawDeg, rollDeg);
    }

    /** Spherical linear interpolation between two quaternions. */
    public static Quaternionf slerp(Quaternionf a, Quaternionf b, float t, Quaternionf out) {
        return out.set(a).slerp(b, t);
    }
}
