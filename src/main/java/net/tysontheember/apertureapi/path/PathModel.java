package net.tysontheember.apertureapi.path;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import net.minecraft.nbt.CompoundTag;
import net.tysontheember.apertureapi.path.interpolation.InterpolationType;
import net.tysontheember.apertureapi.path.interpolation.EasingType;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Enhanced path model for CMDCam parity featuring arc-length parameterization,
 * quaternion orientations, and advanced interpolation modes.
 */
public class PathModel {
    public static final int VERSION = 2;
    
    private final String id;
    private final String name;
    private int version = VERSION;
    private boolean loop = false;
    
    // Default settings
    private final PathDefaults defaults;
    
    // Speed settings
    private final SpeedSettings speed;
    
    // Segments (keyframes)
    private final List<Segment> segments;
    
    // Arc-length lookup table (cached)
    private volatile ArcLengthLUT arcLengthLUT;
    private volatile boolean lutDirty = true;
    
    // Metadata
    private long lastModified;
    private UUID lastModifier;

    public PathModel(String id, String name) {
        this.id = id;
        this.name = name;
        this.defaults = new PathDefaults();
        this.speed = new SpeedSettings();
        this.segments = new ArrayList<>();
        this.lastModified = System.currentTimeMillis();
        this.lastModifier = UUID.fromString("00000000-0000-0000-0000-000000000000");
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public int getVersion() { return version; }
    public boolean isLoop() { return loop; }
    public PathDefaults getDefaults() { return defaults; }
    public SpeedSettings getSpeed() { return speed; }
    public List<Segment> getSegments() { return segments; }
    public long getLastModified() { return lastModified; }
    public UUID getLastModifier() { return lastModifier; }

    // Setters
    public void setLoop(boolean loop) { 
        this.loop = loop; 
        markDirty(); 
    }
    
    public void setLastModifier(UUID modifier) { 
        this.lastModifier = modifier; 
        this.lastModified = System.currentTimeMillis(); 
    }

    /**
     * Add a segment to the path
     */
    public void addSegment(Segment segment) {
        segments.add(segment);
        markDirty();
    }
    
    /**
     * Insert segment at specific index
     */
    public void insertSegment(int index, Segment segment) {
        segments.add(index, segment);
        markDirty();
    }
    
    /**
     * Remove segment at index
     */
    public void removeSegment(int index) {
        if (index >= 0 && index < segments.size()) {
            segments.remove(index);
            markDirty();
        }
    }
    
    /**
     * Get segment at index
     */
    public @Nullable Segment getSegment(int index) {
        return (index >= 0 && index < segments.size()) ? segments.get(index) : null;
    }

    /**
     * Mark LUT as dirty - will be rebuilt on next evaluation
     */
    private void markDirty() {
        lutDirty = true;
        lastModified = System.currentTimeMillis();
    }

    /**
     * Get the arc-length lookup table, building if necessary
     */
    public ArcLengthLUT getArcLengthLUT() {
        if (lutDirty || arcLengthLUT == null) {
            synchronized (this) {
                if (lutDirty || arcLengthLUT == null) {
                    arcLengthLUT = buildArcLengthLUT();
                    lutDirty = false;
                }
            }
        }
        return arcLengthLUT;
    }
    
    /**
     * Build arc-length lookup table using adaptive sampling
     */
    private ArcLengthLUT buildArcLengthLUT() {
        if (segments.size() < 2) {
            return ArcLengthLUT.empty();
        }
        
        // Adaptive sampling based on curvature
        FloatList parameterValues = new FloatArrayList();
        FloatList arcLengthValues = new FloatArrayList();
        
        float totalLength = 0f;
        parameterValues.add(0f);
        arcLengthValues.add(0f);
        
        // Sample each segment
        for (int i = 0; i < segments.size() - 1; i++) {
            Segment current = segments.get(i);
            Segment next = segments.get(i + 1);
            
            // Adaptive sampling - more samples for curves with higher curvature
            int samples = calculateOptimalSamples(current, next);
            Vector3f prevPos = current.position;
            
            for (int s = 1; s <= samples; s++) {
                float t = (float) s / samples;
                float globalT = (i + t) / (segments.size() - 1);
                
                // Evaluate position at this parameter
                Vector3f currentPos = evaluatePositionAt(i, t);
                
                // Add arc length
                float segmentLength = prevPos.distance(currentPos);
                totalLength += segmentLength;
                
                parameterValues.add(globalT);
                arcLengthValues.add(totalLength);
                
                prevPos = currentPos;
            }
        }
        
        return new ArcLengthLUT(parameterValues.toFloatArray(), arcLengthValues.toFloatArray(), totalLength);
    }
    
    /**
     * Calculate optimal sample count based on segment curvature
     */
    private int calculateOptimalSamples(Segment current, Segment next) {
        // Base samples
        int baseSamples = 8;
        
        // Increase samples based on distance and potential curvature
        float distance = current.position.distance(next.position);
        float curvatureFactor = 1f;
        
        // Check for sharp direction changes (approximate curvature)
        if (segments.size() > 2) {
            // Simple curvature approximation using three points
            Vector3f dir1 = new Vector3f(current.position).sub(getPrevPosition(current));
            Vector3f dir2 = new Vector3f(next.position).sub(current.position);
            dir1.normalize();
            dir2.normalize();
            
            float dot = dir1.dot(dir2);
            // More samples for sharper turns (lower dot product)
            curvatureFactor = (2f - dot) / 2f; // Range 0-1, higher for sharper turns
        }
        
        // Scale samples based on distance and curvature
        int samples = Math.max(baseSamples, (int)(baseSamples * (1 + distance * 0.1f + curvatureFactor * 2f)));
        return Math.min(samples, 64); // Cap at 64 samples per segment
    }
    
    /**
     * Get position of segment before given segment
     */
    private Vector3f getPrevPosition(Segment segment) {
        int index = segments.indexOf(segment);
        return index > 0 ? segments.get(index - 1).position : segment.position;
    }
    
    /**
     * Evaluate position at segment + local parameter
     */
    private Vector3f evaluatePositionAt(int segmentIndex, float localT) {
        if (segmentIndex < 0 || segmentIndex >= segments.size() - 1) {
            return segmentIndex < 0 ? segments.get(0).position : segments.get(segments.size() - 1).position;
        }
        
        Segment current = segments.get(segmentIndex);
        Segment next = segments.get(segmentIndex + 1);
        
        // Use segment's interpolation type or fallback to defaults
        InterpolationType interpType = current.interpolationType != null ? 
            current.interpolationType : defaults.interpolationType;
            
        return PathInterpolationEngine.interpolatePosition(
            current, next, localT, interpType, segmentIndex, segments
        );
    }

    /**
     * Path defaults
     */
    public static class PathDefaults {
        public InterpolationType interpolationType = InterpolationType.CATMULL_CENTRIPETAL;
        public EasingType easingType = EasingType.CUBIC_IN_OUT;
        public SpeedMode speedMode = SpeedMode.DURATION;
        public boolean banking = false;
        public float bankingStrength = 1.0f;
        public float rollMix = 0.5f;
        
        public enum SpeedMode {
            DURATION,   // Total path duration in seconds
            SPEED       // Blocks per second
        }
    }

    /**
     * Speed configuration
     */
    public static class SpeedSettings {
        public float durationSec = 10.0f;
        public @Nullable Float blocksPerSec = null; // null means use duration mode
        
        public boolean isSpeedMode() {
            return blocksPerSec != null;
        }
        
        public void setDurationMode(float duration) {
            this.durationSec = duration;
            this.blocksPerSec = null;
        }
        
        public void setSpeedMode(float blocksPerSec) {
            this.blocksPerSec = blocksPerSec;
        }
    }

    /**
     * Enhanced segment supporting new features
     */
    public static class Segment {
        public Vector3f position;
        public Quaternionf orientation; // Using quaternions to avoid gimbal lock
        public float roll = 0f; // Additional roll for banking
        public float fov = 90f;
        
        // Timing
        public @Nullable Float durationSec = null; // null = use path default
        public @Nullable Float weight = null; // Relative timing weight
        
        // Per-segment overrides
        public @Nullable InterpolationType interpolationType = null;
        public @Nullable EasingType easingType = null;
        
        // Follow target
        public @Nullable FollowTarget followTarget = null;
        
        // Hermite/TCB parameters
        public float tension = 0f;
        public float continuity = 0f;
        public float bias = 0f;
        
        // Bezier handles (for cubic bezier interpolation)
        public @Nullable Vector3f bezierIn = null;
        public @Nullable Vector3f bezierOut = null;

        public Segment(Vector3f position, Quaternionf orientation) {
            this.position = new Vector3f(position);
            this.orientation = new Quaternionf(orientation);
        }
        
        public Segment(Vector3f position, float yaw, float pitch, float roll) {
            this.position = new Vector3f(position);
            this.orientation = new Quaternionf().rotateYXZ(
                (float) Math.toRadians(yaw),
                (float) Math.toRadians(pitch), 
                0f // Roll handled separately for banking
            );
            this.roll = roll;
        }
        
        /**
         * Convert to Euler angles (yaw, pitch, roll in degrees)
         */
        public Vector3f toEulerDegrees() {
            Vector3f euler = new Vector3f();
            orientation.getEulerAnglesYXZ(euler);
            return euler.mul((float)(180.0 / Math.PI));
        }
    }

    /**
     * Follow target configuration
     */
    public static class FollowTarget {
        public FollowType type = FollowType.NONE;
        public @Nullable UUID entityId = null;
        public @Nullable Vector3f position = null;
        public float followSpeed = 1.0f; // Lag factor
        
        public enum FollowType {
            NONE, SELF, ENTITY, BLOCK
        }
    }

    /**
     * Arc-length lookup table for constant speed playback
     */
    public static class ArcLengthLUT {
        private final float[] parameters;    // t values
        private final float[] arcLengths;   // s values  
        private final float totalLength;

        public ArcLengthLUT(float[] parameters, float[] arcLengths, float totalLength) {
            this.parameters = parameters;
            this.arcLengths = arcLengths;
            this.totalLength = totalLength;
        }
        
        public static ArcLengthLUT empty() {
            return new ArcLengthLUT(new float[]{0f, 1f}, new float[]{0f, 0f}, 0f);
        }

        /**
         * Convert arc-length to parameter (s -> t)
         */
        public float arcLengthToParameter(float s) {
            if (s <= 0f) return 0f;
            if (s >= totalLength) return 1f;
            
            // Binary search for the appropriate segment
            int index = binarySearchArcLength(s);
            if (index < 0) index = 0;
            if (index >= arcLengths.length - 1) return 1f;
            
            // Linear interpolation between the two closest points
            float s1 = arcLengths[index];
            float s2 = arcLengths[index + 1];
            float t1 = parameters[index];
            float t2 = parameters[index + 1];
            
            float alpha = (s - s1) / (s2 - s1);
            return t1 + alpha * (t2 - t1);
        }
        
        /**
         * Convert parameter to arc-length (t -> s)
         */
        public float parameterToArcLength(float t) {
            if (t <= 0f) return 0f;
            if (t >= 1f) return totalLength;
            
            // Find the segment containing this parameter
            int index = binarySearchParameter(t);
            if (index < 0) index = 0;
            if (index >= parameters.length - 1) return totalLength;
            
            // Linear interpolation
            float t1 = parameters[index];
            float t2 = parameters[index + 1];
            float s1 = arcLengths[index];
            float s2 = arcLengths[index + 1];
            
            float alpha = (t - t1) / (t2 - t1);
            return s1 + alpha * (s2 - s1);
        }
        
        private int binarySearchArcLength(float s) {
            int low = 0;
            int high = arcLengths.length - 1;
            
            while (low <= high) {
                int mid = (low + high) / 2;
                if (arcLengths[mid] < s) {
                    low = mid + 1;
                } else if (arcLengths[mid] > s) {
                    high = mid - 1;
                } else {
                    return mid;
                }
            }
            return high; // Return the index before the insertion point
        }
        
        private int binarySearchParameter(float t) {
            int low = 0;
            int high = parameters.length - 1;
            
            while (low <= high) {
                int mid = (low + high) / 2;
                if (parameters[mid] < t) {
                    low = mid + 1;
                } else if (parameters[mid] > t) {
                    high = mid - 1;
                } else {
                    return mid;
                }
            }
            return high;
        }
        
        public float getTotalLength() { return totalLength; }
    }
    
    /**
     * Convert to JSON (v2 format)
     */
    public JsonObject toJson(Gson gson) {
        // Implementation will be added in the next step
        return new JsonObject();
    }
    
    /**
     * Load from JSON
     */
    public static PathModel fromJson(JsonObject json, Gson gson) {
        // Implementation will be added in the next step
        return new PathModel("temp", "temp");
    }
    
    /**
     * Convert to NBT
     */
    public CompoundTag toNBT() {
        // Implementation will be added in the next step
        return new CompoundTag();
    }
    
    /**
     * Load from NBT
     */
    public static PathModel fromNBT(CompoundTag nbt) {
        // Implementation will be added in the next step
        return new PathModel("temp", "temp");
    }
}