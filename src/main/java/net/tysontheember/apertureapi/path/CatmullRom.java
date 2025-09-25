package net.tysontheember.apertureapi.path;

import org.joml.Vector3f;

/**
 * Catmull-Rom spline evaluator with configurable alpha parameter. alpha = 0.0 (uniform), 0.5
 * (centripetal), 1.0 (chordal) Implementation based on Mika's Coding Bits article and standard
 * formulations.
 */
public final class CatmullRom {
  private CatmullRom() {}

  public static Vector3f eval(
      float t, Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, float alpha, Vector3f out) {
    // Clamp t
    if (t <= 0f) return out.set(p1);
    if (t >= 1f) return out.set(p2);
    alpha = clamp(alpha, 0f, 1f);
    // Compute knot spacing
    float t01 = (float) Math.pow(dist(p0, p1), alpha);
    float t12 = (float) Math.pow(dist(p1, p2), alpha);
    float t23 = (float) Math.pow(dist(p2, p3), alpha);
    // Prevent degenerate
    if (t01 < 1e-6f) t01 = 1e-6f;
    if (t12 < 1e-6f) t12 = 1e-6f;
    if (t23 < 1e-6f) t23 = 1e-6f;

    // Tangents m1, m2
    // Using finite differences per centripetal CR formula
    float invT01 = 1f / t01;
    float invT12 = 1f / t12;
    float invT23 = 1f / t23;

    Vector3f term1 = new Vector3f(p1).sub(p0).mul(invT01);
    Vector3f term2 = new Vector3f(p2).sub(p0).mul(1f / (t01 + t12));
    Vector3f term3 = new Vector3f(p2).sub(p1).mul(invT12);
    Vector3f m1 = term1.sub(term2).add(term3);
    m1.mul((1f - 0f) * t12); // (1 - tension) with default tension 0

    term1.set(p2).sub(p1).mul(invT12);
    term2.set(p3).sub(p1).mul(1f / (t12 + t23));
    term3.set(p3).sub(p2).mul(invT23);
    Vector3f m2 = term1.sub(term2).add(term3);
    m2.mul((1f - 0f) * t12);

    // Hermite basis blending between p1 and p2
    float t2 = t * t;
    float t3 = t2 * t;
    float h00 = 2f * t3 - 3f * t2 + 1f;
    float h10 = t3 - 2f * t2 + t;
    float h01 = -2f * t3 + 3f * t2;
    float h11 = t3 - t2;

    return out.set(
        new Vector3f(p1)
            .mul(h00)
            .add(new Vector3f(m1).mul(h10))
            .add(new Vector3f(p2).mul(h01))
            .add(new Vector3f(m2).mul(h11)));
  }

  private static float dist(Vector3f a, Vector3f b) {
    float dx = b.x - a.x, dy = b.y - a.y, dz = b.z - a.z;
    return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
  }

  private static float clamp(float v, float lo, float hi) {
    return (v < lo) ? lo : (v > hi) ? hi : v;
  }
}
