package net.tysontheember.apertureapi.path;

import org.joml.Vector3f;

/**
 * Fixed-sample arc-length lookup table for a curve segment P(t), t in [0,1]. Provides t->s and s->t
 * mappings for constant-speed traversal.
 */
public final class ArcLengthLUT {
  private final float[] t;
  private final float[] s;
  private final float totalLength;

  @FunctionalInterface
  public interface Evaluator {
    Vector3f eval(float t, Vector3f out);
  }

  public ArcLengthLUT(Evaluator f, int samples) {
    if (samples < 2) samples = 2;
    this.t = new float[samples];
    this.s = new float[samples];
    Vector3f a = new Vector3f();
    Vector3f b = new Vector3f();
    float accum = 0f;
    for (int i = 0; i < samples; i++) {
      float tt = (float) i / (samples - 1);
      t[i] = tt;
      f.eval(tt, b);
      if (i == 0) {
        s[i] = 0f;
        a.set(b);
      } else {
        accum += a.distance(b);
        s[i] = accum;
        a.set(b);
      }
    }
    this.totalLength = accum;
    if (totalLength <= 1e-6f) {
      // avoid division by zero later
      for (int i = 0; i < samples; i++) s[i] = 0f;
    }
  }

  public float totalLength() {
    return totalLength;
  }

  /** Map arc distance d in [0,totalLength] to parameter t in [0,1]. */
  public float tForDistance(float d) {
    if (d <= 0f || totalLength <= 1e-6f) return 0f;
    if (d >= totalLength) return 1f;
    // binary search on s[]
    int lo = 0, hi = s.length - 1;
    while (lo + 1 < hi) {
      int mid = (lo + hi) >>> 1;
      if (s[mid] < d) lo = mid;
      else hi = mid;
    }
    float segLen = s[hi] - s[lo];
    if (segLen <= 1e-6f) return t[lo];
    float alpha = (d - s[lo]) / segLen;
    return t[lo] + (t[hi] - t[lo]) * alpha;
  }

  /** Map parameter t in [0,1] to arc distance in [0,totalLength]. */
  public float distanceForT(float tt) {
    if (tt <= 0f) return 0f;
    if (tt >= 1f) return totalLength;
    float d = tt * (s.length - 1);
    int i = (int) Math.floor(d);
    int j = Math.min(i + 1, s.length - 1);
    float frac = d - i;
    return s[i] + (s[j] - s[i]) * frac;
  }
}
