package net.tysontheember.apertureapi.camera;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Minimal model for JSON camera paths used by the loader/registry. This is a lightweight DTO to
 * capture IDs and a few top-level fields. Extend as the feature set is implemented.
 */
public class CameraPathDef {
  public int schema = 1;
  public String id;
  public boolean loop = false;
  public float speed = 1.0f;

  @SerializedName("fov")
  public @Nullable FovDef fov;

  public @Nullable String easing;

  public List<KeyframeDef> keyframes;

  public static class FovDef {
    public String mode = "inherit"; // inherit | fixed | keyframed
    public float _default = 70.0f;

    @SerializedName("default")
    public void setDefault(float d) {
      this._default = d;
    }
  }

  public static class Vec3Def {
    public double x, y, z;
  }

  public static class RotDef {
    public float yaw, pitch;
    public float roll = 0f;
  }

  public static class KeyframeDef {
    public float time;
    public Vec3Def pos;
    public @Nullable RotDef rot;
    public @Nullable Vec3Def lookAt;
    public @Nullable Float fov;
    public @Nullable String easeIn;
    public @Nullable String easeOut;
  }

  /** Utility to derive a ResourceLocation for storage, if needed. */
  public ResourceLocation asResourceLocation() {
    return new ResourceLocation("apertureapi", id);
  }
}
