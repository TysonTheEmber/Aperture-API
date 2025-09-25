package net.tysontheember.apertureapi.common.camera;

import org.joml.Vector3f;

/**
 * Minimal data object describing how the camera should follow a target during a segment.
 * Currently unused by server commands, but available for JSON definitions.
 */
public class CameraFollowConfig {
    /** Target selector or identifier (implementation-defined). */
    public String target;

    /** Offset from target (world units). */
    public Vector3f offset = new Vector3f();

    /** Desired follow distance; semantics depend on mode. */
    public float distance = 0f;

    /** Whether to maintain target yaw/pitch alignment. */
    public boolean alignRotation = true;

    /** Smoothing factor [0..1], higher = slower response. */
    public float smoothing = 0.0f;

    public CameraFollowConfig() {}
}
