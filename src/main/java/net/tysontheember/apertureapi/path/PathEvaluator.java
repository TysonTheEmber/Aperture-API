package net.tysontheember.apertureapi.path;

import net.tysontheember.apertureapi.path.interpolation.EasingType;
import net.tysontheember.apertureapi.path.interpolation.InterpolationType;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/**
 * Main API for evaluating camera paths with constant speed playback.
 * This is the primary interface that clients use to evaluate paths.
 */
public class PathEvaluator {
    
    /**
     * Complete evaluation result containing all camera parameters
     */
    public static class EvaluationResult {
        public final Vector3f position;
        public final Quaternionf orientation;
        public final float roll;
        public final float fov;
        public final float speed; // Current speed in blocks/second
        public final int segmentIndex;
        public final float segmentProgress; // 0-1 within current segment
        
        public EvaluationResult(Vector3f position, Quaternionf orientation, float roll, float fov, 
                                float speed, int segmentIndex, float segmentProgress) {
            this.position = position;
            this.orientation = orientation;
            this.roll = roll;
            this.fov = fov;
            this.speed = speed;
            this.segmentIndex = segmentIndex;
            this.segmentProgress = segmentProgress;
        }
    }
    
    /**
     * Evaluate path at a given time in seconds
     */
    public static EvaluationResult evaluateAtTime(PathModel path, float timeSeconds) {
        List<PathModel.Segment> segments = path.getSegments();
        if (segments.size() < 2) {
            return createDefaultResult(segments.isEmpty() ? null : segments.get(0));
        }
        
        // Convert time to path parameter based on speed mode
        float globalT;
        if (path.getSpeed().isSpeedMode()) {
            // Speed mode: constant blocks per second
            float blocksPerSec = path.getSpeed().blocksPerSec;
            float arcLength = timeSeconds * blocksPerSec;
            globalT = path.getArcLengthLUT().arcLengthToParameter(arcLength);
        } else {
            // Duration mode: normalize time by total duration
            float totalDuration = path.getSpeed().durationSec;
            globalT = Math.max(0f, Math.min(1f, timeSeconds / totalDuration));
        }
        
        return evaluateAtParameter(path, globalT);
    }
    
    /**
     * Evaluate path at a normalized parameter [0,1]
     */
    public static EvaluationResult evaluateAtParameter(PathModel path, float globalT) {
        List<PathModel.Segment> segments = path.getSegments();
        if (segments.size() < 2) {
            return createDefaultResult(segments.isEmpty() ? null : segments.get(0));
        }
        
        // Handle looping
        if (path.isLoop() && globalT >= 1f) {
            globalT = globalT - (float)Math.floor(globalT); // Wrap around
        } else {
            globalT = Math.max(0f, Math.min(1f, globalT)); // Clamp
        }
        
        // Find the current segment
        int numSegments = segments.size() - 1; // Number of interpolation segments
        float segmentFloat = globalT * numSegments;
        int segmentIndex = (int) Math.floor(segmentFloat);
        float localT = segmentFloat - segmentIndex;
        
        // Handle edge case at end
        if (segmentIndex >= numSegments) {
            segmentIndex = numSegments - 1;
            localT = 1f;
        }
        
        PathModel.Segment current = segments.get(segmentIndex);
        PathModel.Segment next = segments.get(segmentIndex + 1);
        
        // Get interpolation settings (per-segment or fallback to defaults)
        InterpolationType interpType = current.interpolationType != null ? 
            current.interpolationType : path.getDefaults().interpolationType;
        EasingType easingType = current.easingType != null ?
            current.easingType : path.getDefaults().easingType;
        
        // Apply easing to local parameter
        float easedT = easingType.apply(localT);
        
        // Interpolate position
        Vector3f position = PathInterpolationEngine.interpolatePosition(
            current, next, easedT, interpType, segmentIndex, segments
        );
        
        // Interpolate orientation with banking
        Quaternionf orientation = PathInterpolationEngine.interpolateOrientation(
            current, next, easedT, 
            path.getDefaults().banking, path.getDefaults().bankingStrength
        );
        
        // Calculate banking roll if enabled
        float bankingRoll = 0f;
        if (path.getDefaults().banking) {
            bankingRoll = PathInterpolationEngine.calculateBankingRoll(
                current, next, segmentIndex, segments, path.getDefaults().bankingStrength
            );
        }
        
        // Interpolate roll (combines keyframed roll with banking)
        float roll = PathInterpolationEngine.interpolateRoll(
            current.roll, next.roll, easedT, easingType, bankingRoll, path.getDefaults().rollMix
        );
        
        // Interpolate FOV
        float fov = PathInterpolationEngine.interpolateFOV(
            current.fov, next.fov, easedT, easingType
        );
        
        // Calculate current speed
        float speed = calculateCurrentSpeed(path, globalT, segmentIndex, localT);
        
        return new EvaluationResult(position, orientation, roll, fov, speed, segmentIndex, localT);
    }
    
    /**
     * Evaluate path at arc-length (distance along curve)
     */
    public static EvaluationResult evaluateAtArcLength(PathModel path, float arcLength) {
        PathModel.ArcLengthLUT lut = path.getArcLengthLUT();
        float globalT = lut.arcLengthToParameter(arcLength);
        return evaluateAtParameter(path, globalT);
    }
    
    /**
     * Get the total duration of the path in seconds
     */
    public static float getTotalDuration(PathModel path) {
        if (path.getSpeed().isSpeedMode()) {
            // Speed mode: duration = length / speed
            float totalLength = path.getArcLengthLUT().getTotalLength();
            return totalLength / path.getSpeed().blocksPerSec;
        } else {
            // Duration mode: use specified duration
            return path.getSpeed().durationSec;
        }
    }
    
    /**
     * Get the total arc-length of the path in blocks
     */
    public static float getTotalLength(PathModel path) {
        return path.getArcLengthLUT().getTotalLength();
    }
    
    /**
     * Get velocity vector at a given time
     */
    public static Vector3f getVelocityAtTime(PathModel path, float timeSeconds) {
        float dt = 0.016f; // ~1 frame at 60fps
        
        EvaluationResult current = evaluateAtTime(path, timeSeconds);
        EvaluationResult next = evaluateAtTime(path, timeSeconds + dt);
        
        return new Vector3f(next.position).sub(current.position).div(dt);
    }
    
    /**
     * Get velocity vector at parameter
     */
    public static Vector3f getVelocityAtParameter(PathModel path, float globalT) {
        float dt = 0.001f; // Small parameter delta
        
        EvaluationResult current = evaluateAtParameter(path, globalT);
        EvaluationResult next = evaluateAtParameter(path, globalT + dt);
        
        return new Vector3f(next.position).sub(current.position).div(dt);
    }
    
    /**
     * Sample the path at regular intervals for preview/debugging
     */
    public static EvaluationResult[] samplePath(PathModel path, int numSamples) {
        EvaluationResult[] samples = new EvaluationResult[numSamples];
        
        for (int i = 0; i < numSamples; i++) {
            float t = (float) i / (numSamples - 1);
            samples[i] = evaluateAtParameter(path, t);
        }
        
        return samples;
    }
    
    /**
     * Get debug information for a specific time
     */
    public static String getDebugInfo(PathModel path, float timeSeconds) {
        EvaluationResult result = evaluateAtTime(path, timeSeconds);
        Vector3f velocity = getVelocityAtTime(path, timeSeconds);
        
        return String.format(
            "Time: %.2fs\n" +
            "Position: (%.2f, %.2f, %.2f)\n" +
            "Velocity: (%.2f, %.2f, %.2f) [%.2f blocks/s]\n" +
            "Roll: %.2f°\n" +
            "FOV: %.1f°\n" +
            "Segment: %d (%.2f%%)",
            timeSeconds,
            result.position.x, result.position.y, result.position.z,
            velocity.x, velocity.y, velocity.z, velocity.length(),
            result.roll,
            result.fov,
            result.segmentIndex, result.segmentProgress * 100
        );
    }
    
    private static EvaluationResult createDefaultResult(PathModel.Segment segment) {
        if (segment == null) {
            return new EvaluationResult(
                new Vector3f(0, 0, 0),
                new Quaternionf(),
                0f, 90f, 0f, 0, 0f
            );
        }
        
        return new EvaluationResult(
            new Vector3f(segment.position),
            new Quaternionf(segment.orientation),
            segment.roll,
            segment.fov,
            0f, 0, 0f
        );
    }
    
    private static float calculateCurrentSpeed(PathModel path, float globalT, int segmentIndex, float localT) {
        if (path.getSpeed().isSpeedMode()) {
            return path.getSpeed().blocksPerSec;
        }
        
        // For duration mode, calculate instantaneous speed based on arc-length derivative
        PathModel.ArcLengthLUT lut = path.getArcLengthLUT();
        float currentArcLength = lut.parameterToArcLength(globalT);
        
        // Approximate derivative using finite difference
        float dt = 0.001f;
        float nextT = Math.min(1f, globalT + dt);
        float nextArcLength = lut.parameterToArcLength(nextT);
        
        float dsdt = (nextArcLength - currentArcLength) / dt; // ds/dt in parameter space
        float dtds_time = path.getSpeed().durationSec; // dt/ds in time space
        
        return dsdt / dtds_time; // ds/dt_time = (ds/dt_param) / (dt_time/dt_param)
    }
    
    /**
     * Create a simple linear path between two points for testing
     */
    public static PathModel createSimplePath(String id, Vector3f start, Vector3f end, float duration) {
        PathModel path = new PathModel(id, id);
        path.addSegment(new PathModel.Segment(start, 0f, 0f, 0f));
        path.addSegment(new PathModel.Segment(end, 0f, 0f, 0f));
        path.getSpeed().setDurationMode(duration);
        return path;
    }
    
    /**
     * Create a test path with multiple segments demonstrating various features
     */
    public static PathModel createTestPath() {
        PathModel path = new PathModel("test_path", "Test Path");
        
        // Defaults to smooth centripetal Catmull-Rom
        path.getDefaults().interpolationType = InterpolationType.CATMULL_CENTRIPETAL;
        path.getDefaults().easingType = EasingType.CUBIC_IN_OUT;
        path.getDefaults().banking = true;
        path.getDefaults().bankingStrength = 0.5f;
        
        // Add some test segments
        path.addSegment(new PathModel.Segment(new Vector3f(0, 70, 0), 0f, 0f, 0f));
        path.addSegment(new PathModel.Segment(new Vector3f(10, 75, 5), 45f, -10f, 5f));
        path.addSegment(new PathModel.Segment(new Vector3f(20, 73, -2), 90f, 0f, -5f));
        path.addSegment(new PathModel.Segment(new Vector3f(25, 78, 10), 180f, 15f, 0f));
        path.addSegment(new PathModel.Segment(new Vector3f(15, 80, 20), 270f, -5f, 0f));
        
        path.getSpeed().setDurationMode(10f); // 10 second duration
        
        return path;
    }
}