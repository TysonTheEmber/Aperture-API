package net.tysontheember.apertureapi.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.tysontheember.apertureapi.InterpolationMath;
import net.tysontheember.apertureapi.common.animation.CameraKeyframe;
import net.tysontheember.apertureapi.common.animation.GlobalCameraPath;
import net.tysontheember.apertureapi.common.animation.PathInterpolator;
import net.tysontheember.apertureapi.path.CatmullRom;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Lightweight debug path renderer that draws curve lines and optional velocity coloring. Intended
 * for edit/preview mode live visualization.
 */
public final class PathDebugRenderer {
  private static final Vector3f V0 = new Vector3f();
  private static final Vector3f V1 = new Vector3f();
  private static final Vector3f V2 = new Vector3f();
  private static final Vector3f V3 = new Vector3f();
  private static final Vector3f P = new Vector3f();
  private static final Vector3f Q = new Vector3f();

  public static boolean showVelocityColor = false;
  public static boolean showDirectionTicks = false;

  private PathDebugRenderer() {}

  public static void render(
      GlobalCameraPath path,
      PoseStack.Pose pose,
      MultiBufferSource.BufferSource buffers,
      Vec3 cameraPos) {
    ArrayList<CameraKeyframe> points = path.getPoints();
    if (points.size() < 2) return;
    VertexConsumer lines = buffers.getBuffer(RenderType.LINES);
    Matrix4f m = pose.pose();
    Matrix3f n = pose.normal();
    // For each segment between keyframes
    for (int i = 1; i < points.size(); i++) {
      CameraKeyframe pre = points.get(i - 1);
      CameraKeyframe next = points.get(i);
      Vector3f p1 =
          new Vector3f(pre.getPos())
              .sub((float) cameraPos.x, (float) cameraPos.y, (float) cameraPos.z);
      Vector3f p2 =
          new Vector3f(next.getPos())
              .sub((float) cameraPos.x, (float) cameraPos.y, (float) cameraPos.z);

      int samples = 32;
      Vector3f prev = new Vector3f(p1);
      float maxVel = 1e-6f;
      float[] speeds = new float[samples + 1];
      Vector3f[] pts = new Vector3f[samples + 1];
      pts[0] = new Vector3f(prev);
      speeds[0] = 0f;

      for (int s = 1; s <= samples; s++) {
        float t = (float) s / samples;
        eval(path, i, t, pre, next, cameraPos, P);
        pts[s] = new Vector3f(P);
        float v = P.distance(prev);
        speeds[s] = v;
        if (v > maxVel) maxVel = v;
        prev.set(P);
      }

      // draw with velocity color or white
      for (int s = 1; s <= samples; s++) {
        int color;
        if (showVelocityColor) {
          float v = speeds[s] / (maxVel <= 1e-6f ? 1f : maxVel);
          color = lerpColor(0xff0000ff, 0xffff0000, v); // blue->red
        } else {
          color = 0xffffffff;
        }
        Vector3f a = pts[s - 1];
        Vector3f b = pts[s];
        addLine(lines, m, n, a, b, color);
        if (showDirectionTicks && s % 4 == 0) {
          // small tick in direction
          Q.set(b).sub(a).normalize().mul(0.2f);
          Vector3f tickEnd = new Vector3f(b).add(Q);
          addLine(lines, m, n, b, tickEnd, 0xff00ffff);
        }
      }
    }
    buffers.endBatch(RenderType.LINES);
  }

  private static void eval(
      GlobalCameraPath path,
      int segIndex,
      float t,
      CameraKeyframe pre,
      CameraKeyframe next,
      Vec3 cam,
      Vector3f out) {
    PathInterpolator mode = next.getPathInterpolator();
    // Need neighbors for catmull
    Vector3f p0, p3;
    int i = segIndex;
    ArrayList<CameraKeyframe> pts = path.getPoints();
    p0 = (i - 2 >= 0) ? pts.get(i - 2).getPos() : pre.getPos();
    p3 = (i < pts.size() ? pts.get(i).getPos() : next.getPos());

    switch (mode) {
      case LINEAR -> InterpolationMath.line(
          t,
          new Vector3f(pre.getPos()).sub((float) cam.x, (float) cam.y, (float) cam.z),
          new Vector3f(next.getPos()).sub((float) cam.x, (float) cam.y, (float) cam.z),
          out);
      case COSINE -> {
        float tc = (1.0f - (float) Math.cos(Math.PI * t)) * 0.5f;
        InterpolationMath.line(
            tc,
            new Vector3f(pre.getPos()).sub((float) cam.x, (float) cam.y, (float) cam.z),
            new Vector3f(next.getPos()).sub((float) cam.x, (float) cam.y, (float) cam.z),
            out);
      }
      case SMOOTH -> InterpolationMath.catmullRom(
          t,
          new Vector3f(p0).sub((float) cam.x, (float) cam.y, (float) cam.z),
          new Vector3f(pre.getPos()).sub((float) cam.x, (float) cam.y, (float) cam.z),
          new Vector3f(next.getPos()).sub((float) cam.x, (float) cam.y, (float) cam.z),
          new Vector3f(p3).sub((float) cam.x, (float) cam.y, (float) cam.z),
          out);
      case CATMULL_UNIFORM -> CatmullRom.eval(
          t,
          new Vector3f(p0).sub((float) cam.x, (float) cam.y, (float) cam.z),
          new Vector3f(pre.getPos()).sub((float) cam.x, (float) cam.y, (float) cam.z),
          new Vector3f(next.getPos()).sub((float) cam.x, (float) cam.y, (float) cam.z),
          new Vector3f(p3).sub((float) cam.x, (float) cam.y, (float) cam.z),
          0.0f,
          out);
      case CATMULL_CENTRIPETAL -> CatmullRom.eval(
          t,
          new Vector3f(p0).sub((float) cam.x, (float) cam.y, (float) cam.z),
          new Vector3f(pre.getPos()).sub((float) cam.x, (float) cam.y, (float) cam.z),
          new Vector3f(next.getPos()).sub((float) cam.x, (float) cam.y, (float) cam.z),
          new Vector3f(p3).sub((float) cam.x, (float) cam.y, (float) cam.z),
          0.5f,
          out);
      case CATMULL_CHORDAL -> CatmullRom.eval(
          t,
          new Vector3f(p0).sub((float) cam.x, (float) cam.y, (float) cam.z),
          new Vector3f(pre.getPos()).sub((float) cam.x, (float) cam.y, (float) cam.z),
          new Vector3f(next.getPos()).sub((float) cam.x, (float) cam.y, (float) cam.z),
          new Vector3f(p3).sub((float) cam.x, (float) cam.y, (float) cam.z),
          1.0f,
          out);
      case BEZIER -> pre.getPathBezier()
          .interpolate(
              t,
              new Vector3f(pre.getPos()).sub((float) cam.x, (float) cam.y, (float) cam.z),
              new Vector3f(next.getPos()).sub((float) cam.x, (float) cam.y, (float) cam.z),
              out);
      case STEP -> out.set(
          new Vector3f(pre.getPos()).sub((float) cam.x, (float) cam.y, (float) cam.z));
    }
  }

  private static void addLine(
      VertexConsumer buffer, Matrix4f m, Matrix3f nrm, Vector3f a, Vector3f b, int color) {
    Vector3f dir = new Vector3f(b).sub(a).normalize();
    buffer.vertex(m, a.x, a.y, a.z).color(color).normal(nrm, dir.x, dir.y, dir.z).endVertex();
    buffer.vertex(m, b.x, b.y, b.z).color(color).normal(nrm, dir.x, dir.y, dir.z).endVertex();
  }

  private static int lerpColor(int c1, int c2, float t) {
    t = Mth.clamp(t, 0f, 1f);
    int a1 = (c1 >>> 24) & 0xff, r1 = (c1 >>> 16) & 0xff, g1 = (c1 >>> 8) & 0xff, b1 = c1 & 0xff;
    int a2 = (c2 >>> 24) & 0xff, r2 = (c2 >>> 16) & 0xff, g2 = (c2 >>> 8) & 0xff, b2 = c2 & 0xff;
    int a = (int) (a1 + (a2 - a1) * t);
    int r = (int) (r1 + (r2 - r1) * t);
    int g = (int) (g1 + (g2 - g1) * t);
    int b = (int) (b1 + (b2 - b1) * t);
    return (a << 24) | (r << 16) | (g << 8) | b;
  }
}
