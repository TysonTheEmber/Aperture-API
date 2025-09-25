package net.tysontheember.apertureapi.common.animation;

import org.joml.Vector2f;

/**
 * Configuration for a value curve used by the camera animation system.
 * This is intentionally lightweight and uses public fields so it can be
 * serialized/deserialized via Gson as used elsewhere in the codebase.
 */
public class CurveConfig {
    /** The curve type. */
    public CurveType type;

    /** Incoming handle for bezier-like curves (x,y in [0,1]). */
    public Vector2f inHandle;

    /** Outgoing handle for bezier-like curves (x,y in [0,1]). */
    public Vector2f outHandle;

    /** No-arg constructor for Gson. Defaults to SMOOTH. */
    public CurveConfig() {
        this(CurveType.SMOOTH);
    }

    public CurveConfig(CurveType type) {
        this(type, new Vector2f(0.33f, 0.33f), new Vector2f(0.66f, 0.66f));
    }

    public CurveConfig(CurveType type, Vector2f inHandle, Vector2f outHandle) {
        this.type = type;
        // Defensive copies
        this.inHandle = inHandle != null ? new Vector2f(inHandle) : new Vector2f();
        this.outHandle = outHandle != null ? new Vector2f(outHandle) : new Vector2f(1f, 1f);
    }

    /** Create a deep copy of this config. */
    public CurveConfig copy() {
        return new CurveConfig(this.type,
                this.inHandle != null ? new Vector2f(this.inHandle) : null,
                this.outHandle != null ? new Vector2f(this.outHandle) : null);
    }
}
