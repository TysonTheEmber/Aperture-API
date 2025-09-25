package net.tysontheember.apertureapi.common.animation;

import org.joml.Vector3f;

/** Utilities to prevent single-frame jitters at keyframe boundaries */
public class JitterPrevention {

  private static final float EPSILON = 0.001f;

  /**
   * Smooth interpolation parameter calculation that prevents jitters at keyframe boundaries
   *
   * @param currentTime Current animation time (including partial ticks)
   * @param keyframeTime1 Previous keyframe time
   * @param keyframeTime2 Next keyframe time
   * @return Smoothed interpolation parameter between 0.0 and 1.0
   */
  public static float calculateSmoothT(float currentTime, int keyframeTime1, int keyframeTime2) {
    float timeDelta = keyframeTime2 - keyframeTime1;

    // Handle edge cases
    if (timeDelta <= EPSILON) {
      return 0.0f;
    }

    float rawT = (currentTime - keyframeTime1) / timeDelta;

    // Clamp to prevent overshoot
    return Math.max(0.0f, Math.min(1.0f, rawT));
  }

  /**
   * Check if two times are effectively equal (within epsilon)
   *
   * @param time1 First time
   * @param time2 Second time
   * @return True if times are effectively equal
   */
  public static boolean isTimeEqual(float time1, float time2) {
    return Math.abs(time1 - time2) <= EPSILON;
  }

  /**
   * Smooth vector interpolation that prevents micro-jitters
   *
   * @param t Interpolation parameter (0.0 to 1.0)
   * @param start Start vector
   * @param end End vector
   * @param result Result vector
   * @return The result vector for chaining
   */
  public static Vector3f smoothLerp(float t, Vector3f start, Vector3f end, Vector3f result) {
    // Apply small smoothing to t to prevent micro-stutters
    float smoothT = smoothStep(t);
    return result.set(start).lerp(end, smoothT);
  }

  /**
   * Smooth step function that provides smoother transitions than linear interpolation
   *
   * @param t Input parameter (0.0 to 1.0)
   * @return Smoothed parameter
   */
  private static float smoothStep(float t) {
    // Clamp input
    t = Math.max(0.0f, Math.min(1.0f, t));

    // Apply smoothstep function: 3t² - 2t³
    return t * t * (3.0f - 2.0f * t);
  }

  /**
   * Enhanced rotation interpolation that handles angle wrapping smoothly
   *
   * @param t Interpolation parameter
   * @param startRot Start rotation (degrees)
   * @param endRot End rotation (degrees)
   * @param result Result rotation
   * @return The result vector for chaining
   */
  public static Vector3f smoothRotationLerp(
      float t, Vector3f startRot, Vector3f endRot, Vector3f result) {
    float smoothT = smoothStep(t);

    // Handle angle wrapping for each component
    float x = lerpAngle(startRot.x, endRot.x, smoothT);
    float y = lerpAngle(startRot.y, endRot.y, smoothT);
    float z = lerpAngle(startRot.z, endRot.z, smoothT);

    return result.set(x, y, z);
  }

  /**
   * Linear interpolation for angles that handles wraparound correctly
   *
   * @param start Start angle in degrees
   * @param end End angle in degrees
   * @param t Interpolation parameter
   * @return Interpolated angle in degrees
   */
  private static float lerpAngle(float start, float end, float t) {
    // Normalize angles to [-180, 180] range
    start = normalizeAngle(start);
    end = normalizeAngle(end);

    // Find the shortest path between angles
    float diff = end - start;
    if (diff > 180.0f) {
      diff -= 360.0f;
    } else if (diff < -180.0f) {
      diff += 360.0f;
    }

    return normalizeAngle(start + diff * t);
  }

  /**
   * Normalize angle to [-180, 180] range
   *
   * @param angle Input angle in degrees
   * @return Normalized angle
   */
  private static float normalizeAngle(float angle) {
    while (angle > 180.0f) {
      angle -= 360.0f;
    }
    while (angle < -180.0f) {
      angle += 360.0f;
    }
    return angle;
  }

  /**
   * Enhanced FOV interpolation with smooth transitions
   *
   * @param t Interpolation parameter
   * @param startFov Start FOV
   * @param endFov End FOV
   * @return Smoothly interpolated FOV
   */
  public static float smoothFovLerp(float t, float startFov, float endFov) {
    float smoothT = smoothStep(t);
    return startFov + (endFov - startFov) * smoothT;
  }

  /**
   * Check if we're very close to a keyframe boundary and apply micro-smoothing This prevents
   * single-frame snaps that can occur due to floating point precision
   *
   * @param currentTime Current animation time
   * @param keyframeTime Keyframe time to check against
   * @param tolerance Tolerance for boundary detection
   * @return True if we're at a keyframe boundary
   */
  public static boolean isAtKeyframeBoundary(float currentTime, int keyframeTime, float tolerance) {
    return Math.abs(currentTime - keyframeTime) <= tolerance;
  }

  /**
   * Apply temporal smoothing to reduce frame-to-frame jitter This is particularly useful for high
   * refresh rate displays
   *
   * @param currentValue Current interpolated value
   * @param previousValue Previous frame's value
   * @param smoothingFactor Smoothing strength (0.0 = no smoothing, 1.0 = maximum smoothing)
   * @return Smoothed value
   */
  public static float temporalSmoothing(
      float currentValue, float previousValue, float smoothingFactor) {
    smoothingFactor = Math.max(0.0f, Math.min(1.0f, smoothingFactor));
    return currentValue + (previousValue - currentValue) * smoothingFactor;
  }
}
