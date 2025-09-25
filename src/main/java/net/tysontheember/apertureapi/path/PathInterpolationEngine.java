package net.tysontheember.apertureapi.path;

import java.util.List;
import net.minecraft.util.Mth;
import net.tysontheember.apertureapi.path.interpolation.EasingType;
import net.tysontheember.apertureapi.path.interpolation.InterpolationType;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Core interpolation engine for the new path system. Handles all position, orientation, and
 * parameter interpolation.
 */
public class PathInterpolationEngine {

  /** Interpolate position between two segments */
  public static Vector3f interpolatePosition(
      PathModel.Segment current,
      PathModel.Segment next,
      float localT,
      InterpolationType type,
      int segmentIndex,
      List<PathModel.Segment> allSegments) {
    return switch (type) {
      case LINEAR -> interpolateLinear(current.position, next.position, localT);
      case COSINE -> interpolateCosine(current.position, next.position, localT);
      case HERMITE -> interpolateHermite(current, next, localT, segmentIndex, allSegments);
      case BEZIER -> interpolateBezier(current, next, localT);
      case CATMULL_UNIFORM, CATMULL_CENTRIPETAL, CATMULL_CHORDAL -> interpolateCatmullRom(
          current, next, localT, type.getCatmullAlpha(), segmentIndex, allSegments);
    };
  }

  /** Interpolate orientation between two segments using quaternion slerp */
  public static Quaternionf interpolateOrientation(
      PathModel.Segment current,
      PathModel.Segment next,
      float localT,
      boolean banking,
      float bankingStrength) {
    Quaternionf result = new Quaternionf();

    // Base orientation interpolation using slerp for smooth rotation
    current.orientation.slerp(next.orientation, localT, result);

    // Add banking if enabled
    if (banking && bankingStrength > 0f) {
      // Calculate velocity direction for banking
      Vector3f velocity = new Vector3f(next.position).sub(current.position);
      if (velocity.lengthSquared() > 1e-6f) {
        velocity.normalize();

        // Compute banking rotation (roll around velocity direction)
        // This simulates aircraft-like banking in turns
        Vector3f up = new Vector3f(0, 1, 0);
        Vector3f right = new Vector3f(velocity).cross(up);
        if (right.lengthSquared() > 1e-6f) {
          right.normalize();
          float bankAngle =
              right.dot(velocity) * bankingStrength * (float) (Math.PI / 6); // Max 30 degrees
          Quaternionf bankingRotation = new Quaternionf().rotateAxis(bankAngle, velocity);
          result.mul(bankingRotation);
        }
      }
    }

    return result;
  }

  /** Interpolate FOV with easing */
  public static float interpolateFOV(
      float currentFOV, float nextFOV, float localT, EasingType easing) {
    float easedT = easing.apply(localT);
    return Mth.lerp(easedT, currentFOV, nextFOV);
  }

  /** Interpolate roll with easing and banking blend */
  public static float interpolateRoll(
      float currentRoll,
      float nextRoll,
      float localT,
      EasingType easing,
      float bankingRoll,
      float rollMix) {
    float easedT = easing.apply(localT);
    float interpolatedRoll = Mth.lerp(easedT, currentRoll, nextRoll);

    // Blend with banking roll
    return Mth.lerp(rollMix, interpolatedRoll, bankingRoll);
  }

  // ===== POSITION INTERPOLATION METHODS =====

  private static Vector3f interpolateLinear(Vector3f a, Vector3f b, float t) {
    return new Vector3f(a).lerp(b, t);
  }

  private static Vector3f interpolateCosine(Vector3f a, Vector3f b, float t) {
    // Smooth cosine interpolation
    float cosineT = (1f - (float) Math.cos(t * Math.PI)) * 0.5f;
    return new Vector3f(a).lerp(b, cosineT);
  }

  private static Vector3f interpolateHermite(
      PathModel.Segment current,
      PathModel.Segment next,
      float t,
      int segmentIndex,
      List<PathModel.Segment> allSegments) {
    // Get control points for Hermite interpolation
    Vector3f p0 = current.position;
    Vector3f p1 = next.position;

    // Calculate tangents using finite differences with TCB parameters
    Vector3f m0 = calculateHermiteTangent(current, segmentIndex, allSegments, true);
    Vector3f m1 = calculateHermiteTangent(next, segmentIndex + 1, allSegments, false);

    // Hermite basis functions
    float t2 = t * t;
    float t3 = t2 * t;

    float h00 = 2f * t3 - 3f * t2 + 1f; // (1 + 2t)(1-t)²
    float h10 = t3 - 2f * t2 + t; // t(1-t)²
    float h01 = -2f * t3 + 3f * t2; // t²(3-2t)
    float h11 = t3 - t2; // t²(t-1)

    return new Vector3f(p0)
        .mul(h00)
        .add(new Vector3f(m0).mul(h10))
        .add(new Vector3f(p1).mul(h01))
        .add(new Vector3f(m1).mul(h11));
  }

  private static Vector3f calculateHermiteTangent(
      PathModel.Segment segment, int index, List<PathModel.Segment> allSegments, boolean incoming) {
    if (allSegments.size() < 2) {
      return new Vector3f(0, 0, 0);
    }

    Vector3f tangent = new Vector3f();

    // Get neighboring points
    Vector3f prev = index > 0 ? allSegments.get(index - 1).position : segment.position;
    Vector3f curr = segment.position;
    Vector3f next =
        index < allSegments.size() - 1 ? allSegments.get(index + 1).position : segment.position;

    // TCB parameters
    float tension = segment.tension;
    float continuity = segment.continuity;
    float bias = segment.bias;

    // Finite difference tangent calculation with TCB
    Vector3f inTangent = new Vector3f(curr).sub(prev);
    Vector3f outTangent = new Vector3f(next).sub(curr);

    if (incoming) {
      tangent =
          new Vector3f(inTangent)
              .mul((1f - tension) * (1f + continuity) * (1f + bias) / 2f)
              .add(
                  new Vector3f(outTangent)
                      .mul((1f - tension) * (1f - continuity) * (1f - bias) / 2f));
    } else {
      tangent =
          new Vector3f(inTangent)
              .mul((1f - tension) * (1f + continuity) * (1f - bias) / 2f)
              .add(
                  new Vector3f(outTangent)
                      .mul((1f - tension) * (1f - continuity) * (1f + bias) / 2f));
    }

    return tangent;
  }

  private static Vector3f interpolateBezier(
      PathModel.Segment current, PathModel.Segment next, float t) {
    // Use explicit Bezier handles if available, otherwise auto-generate
    Vector3f p0 = current.position;
    Vector3f p3 = next.position;

    Vector3f p1, p2;
    if (current.bezierOut != null) {
      p1 = new Vector3f(current.position).add(current.bezierOut);
    } else {
      // Auto-generate outgoing handle (1/3 toward next point)
      p1 = new Vector3f(current.position).lerp(next.position, 0.33f);
    }

    if (next.bezierIn != null) {
      p2 = new Vector3f(next.position).add(next.bezierIn);
    } else {
      // Auto-generate incoming handle (1/3 back from current point)
      p2 = new Vector3f(next.position).lerp(current.position, 0.33f);
    }

    // Cubic Bezier calculation
    float t2 = t * t;
    float t3 = t2 * t;
    float mt = 1f - t;
    float mt2 = mt * mt;
    float mt3 = mt2 * mt;

    return new Vector3f(p0)
        .mul(mt3)
        .add(new Vector3f(p1).mul(3f * mt2 * t))
        .add(new Vector3f(p2).mul(3f * mt * t2))
        .add(new Vector3f(p3).mul(t3));
  }

  private static Vector3f interpolateCatmullRom(
      PathModel.Segment current,
      PathModel.Segment next,
      float t,
      float alpha,
      int segmentIndex,
      List<PathModel.Segment> allSegments) {
    // Get the four control points for Catmull-Rom
    Vector3f p0, p1, p2, p3;

    p1 = current.position;
    p2 = next.position;

    // Get previous point (or extrapolate if at beginning)
    if (segmentIndex > 0) {
      p0 = allSegments.get(segmentIndex - 1).position;
    } else {
      // Extrapolate backward
      p0 = new Vector3f(p1).mul(2f).sub(p2);
    }

    // Get next next point (or extrapolate if at end)
    if (segmentIndex + 2 < allSegments.size()) {
      p3 = allSegments.get(segmentIndex + 2).position;
    } else {
      // Extrapolate forward
      p3 = new Vector3f(p2).mul(2f).sub(p1);
    }

    // Use the enhanced Catmull-Rom implementation
    return evaluateCatmullRom(t, p0, p1, p2, p3, alpha);
  }

  /**
   * Enhanced Catmull-Rom spline with configurable alpha parameter Based on the existing CatmullRom
   * class but integrated here
   */
  private static Vector3f evaluateCatmullRom(
      float t, Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, float alpha) {
    // Clamp t
    if (t <= 0f) return new Vector3f(p1);
    if (t >= 1f) return new Vector3f(p2);

    alpha = Math.max(0f, Math.min(1f, alpha));

    // Compute knot spacing based on distance with alpha parameter
    float t01 = (float) Math.pow(p0.distance(p1), alpha);
    float t12 = (float) Math.pow(p1.distance(p2), alpha);
    float t23 = (float) Math.pow(p2.distance(p3), alpha);

    // Prevent degenerate cases
    if (t01 < 1e-6f) t01 = 1e-6f;
    if (t12 < 1e-6f) t12 = 1e-6f;
    if (t23 < 1e-6f) t23 = 1e-6f;

    // Calculate tangents at p1 and p2
    Vector3f a1 =
        new Vector3f(p1)
            .sub(p0)
            .div(t01)
            .sub(new Vector3f(p2).sub(p0).div(t01 + t12))
            .add(new Vector3f(p2).sub(p1).div(t12))
            .mul(t12);

    Vector3f a2 =
        new Vector3f(p2)
            .sub(p1)
            .div(t12)
            .sub(new Vector3f(p3).sub(p1).div(t12 + t23))
            .add(new Vector3f(p3).sub(p2).div(t23))
            .mul(t12);

    // Hermite interpolation between p1 and p2 using calculated tangents
    float t2 = t * t;
    float t3 = t2 * t;

    float h00 = 2f * t3 - 3f * t2 + 1f;
    float h10 = t3 - 2f * t2 + t;
    float h01 = -2f * t3 + 3f * t2;
    float h11 = t3 - t2;

    return new Vector3f(p1)
        .mul(h00)
        .add(new Vector3f(a1).mul(h10))
        .add(new Vector3f(p2).mul(h01))
        .add(new Vector3f(a2).mul(h11));
  }

  /** Calculate banking roll for a segment based on path curvature */
  public static float calculateBankingRoll(
      PathModel.Segment current,
      PathModel.Segment next,
      int segmentIndex,
      List<PathModel.Segment> allSegments,
      float bankingStrength) {
    if (allSegments.size() < 3 || bankingStrength <= 0f) {
      return 0f;
    }

    // Get three points to calculate curvature
    Vector3f prev =
        segmentIndex > 0 ? allSegments.get(segmentIndex - 1).position : current.position;
    Vector3f curr = current.position;
    Vector3f next_pos = next.position;

    // Calculate vectors
    Vector3f v1 = new Vector3f(curr).sub(prev);
    Vector3f v2 = new Vector3f(next_pos).sub(curr);

    if (v1.lengthSquared() < 1e-6f || v2.lengthSquared() < 1e-6f) {
      return 0f;
    }

    v1.normalize();
    v2.normalize();

    // Calculate curvature using cross product
    Vector3f cross = new Vector3f(v1).cross(v2);
    float curvature = cross.length();

    // Determine banking direction (left or right turn)
    float bankDirection = cross.y; // Y component indicates left/right

    // Convert to banking angle (in degrees)
    float maxBankAngle = 30f; // Maximum banking angle
    return Math.signum(bankDirection) * curvature * bankingStrength * maxBankAngle;
  }
}
