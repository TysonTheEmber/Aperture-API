package net.tysontheember.apertureapi.path;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
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
        JsonObject json = new JsonObject();
        
        // Header
        json.addProperty("version", VERSION);
        json.addProperty("id", id);
        json.addProperty("name", name);
        json.addProperty("loop", loop);
        
        // Defaults
        JsonObject defaultsObj = new JsonObject();
        defaultsObj.addProperty("interp", defaults.interpolationType.getName());
        defaultsObj.addProperty("ease", defaults.easingType.getName());
        defaultsObj.addProperty("speedMode", defaults.speedMode.name().toLowerCase());
        defaultsObj.addProperty("banking", defaults.banking);
        defaultsObj.addProperty("bankingStrength", defaults.bankingStrength);
        defaultsObj.addProperty("rollMix", defaults.rollMix);
        json.add("defaults", defaultsObj);
        
        // Speed settings
        JsonObject speedObj = new JsonObject();
        speedObj.addProperty("durationSec", speed.durationSec);
        if (speed.blocksPerSec != null) {
            speedObj.addProperty("blocksPerSec", speed.blocksPerSec);
        }
        json.add("speed", speedObj);
        
        // Segments
        JsonArray segmentsArray = new JsonArray();
        for (Segment segment : segments) {
            segmentsArray.add(segmentToJson(segment));
        }
        json.add("segments", segmentsArray);
        
        // Metadata
        json.addProperty("lastModified", lastModified);
        json.addProperty("lastModifier", lastModifier.toString());
        
        return json;
    }
    
    private JsonObject segmentToJson(Segment segment) {
        JsonObject json = new JsonObject();
        
        // Position
        JsonArray pos = new JsonArray();
        pos.add(segment.position.x);
        pos.add(segment.position.y);
        pos.add(segment.position.z);
        json.add("p", pos);
        
        // Orientation as quaternion and Euler
        JsonObject rot = new JsonObject();
        Vector3f euler = segment.toEulerDegrees();
        rot.addProperty("yaw", euler.y);
        rot.addProperty("pitch", euler.x);
        rot.addProperty("roll", euler.z);
        
        // Also store quaternion for precision
        JsonArray quat = new JsonArray();
        quat.add(segment.orientation.x);
        quat.add(segment.orientation.y);
        quat.add(segment.orientation.z);
        quat.add(segment.orientation.w);
        rot.add("q", quat);
        json.add("rot", rot);
        
        // Additional roll for banking
        json.addProperty("roll", segment.roll);
        json.addProperty("fov", segment.fov);
        
        // Timing
        if (segment.durationSec != null) {
            json.addProperty("durationSec", segment.durationSec);
        }
        if (segment.weight != null) {
            json.addProperty("weight", segment.weight);
        }
        
        // Per-segment overrides
        if (segment.interpolationType != null) {
            json.addProperty("interp", segment.interpolationType.getName());
        }
        if (segment.easingType != null) {
            json.addProperty("ease", segment.easingType.getName());
        }
        
        // Follow target
        if (segment.followTarget != null) {
            JsonObject followObj = new JsonObject();
            followObj.addProperty("type", segment.followTarget.type.name().toLowerCase());
            if (segment.followTarget.entityId != null) {
                followObj.addProperty("id", segment.followTarget.entityId.toString());
            }
            if (segment.followTarget.position != null) {
                JsonArray followPos = new JsonArray();
                followPos.add(segment.followTarget.position.x);
                followPos.add(segment.followTarget.position.y);
                followPos.add(segment.followTarget.position.z);
                followObj.add("pos", followPos);
            }
            followObj.addProperty("followSpeed", segment.followTarget.followSpeed);
            json.add("lookAt", followObj);
        }
        
        // TCB parameters
        if (segment.tension != 0f || segment.continuity != 0f || segment.bias != 0f) {
            JsonObject tcb = new JsonObject();
            tcb.addProperty("t", segment.tension);
            tcb.addProperty("c", segment.continuity);
            tcb.addProperty("b", segment.bias);
            json.add("tcb", tcb);
        }
        
        // Bezier handles
        if (segment.bezierIn != null || segment.bezierOut != null) {
            JsonObject bezier = new JsonObject();
            if (segment.bezierIn != null) {
                JsonArray inArray = new JsonArray();
                inArray.add(segment.bezierIn.x);
                inArray.add(segment.bezierIn.y);
                inArray.add(segment.bezierIn.z);
                bezier.add("in", inArray);
            }
            if (segment.bezierOut != null) {
                JsonArray outArray = new JsonArray();
                outArray.add(segment.bezierOut.x);
                outArray.add(segment.bezierOut.y);
                outArray.add(segment.bezierOut.z);
                bezier.add("out", outArray);
            }
            json.add("bezier", bezier);
        }
        
        return json;
    }
    
    /**
     * Load from JSON
     */
    public static PathModel fromJson(JsonObject json, Gson gson) {
        // Check version
        int version = json.has("version") ? json.get("version").getAsInt() : 1;
        
        if (version == 1) {
            // Migrate from v1
            return migrateFromV1(json);
        }
        
        // Parse v2
        String id = json.get("id").getAsString();
        String name = json.has("name") ? json.get("name").getAsString() : id;
        
        PathModel path = new PathModel(id, name);
        
        // Basic properties
        if (json.has("loop")) {
            path.setLoop(json.get("loop").getAsBoolean());
        }
        
        // Defaults
        if (json.has("defaults")) {
            JsonObject defaults = json.getAsJsonObject("defaults");
            if (defaults.has("interp")) {
                path.defaults.interpolationType = InterpolationType.fromString(defaults.get("interp").getAsString());
            }
            if (defaults.has("ease")) {
                path.defaults.easingType = EasingType.fromString(defaults.get("ease").getAsString());
            }
            if (defaults.has("speedMode")) {
                String speedMode = defaults.get("speedMode").getAsString();
                path.defaults.speedMode = speedMode.equals("speed") ? 
                    PathDefaults.SpeedMode.SPEED : PathDefaults.SpeedMode.DURATION;
            }
            if (defaults.has("banking")) {
                path.defaults.banking = defaults.get("banking").getAsBoolean();
            }
            if (defaults.has("bankingStrength")) {
                path.defaults.bankingStrength = defaults.get("bankingStrength").getAsFloat();
            }
            if (defaults.has("rollMix")) {
                path.defaults.rollMix = defaults.get("rollMix").getAsFloat();
            }
        }
        
        // Speed settings
        if (json.has("speed")) {
            JsonObject speed = json.getAsJsonObject("speed");
            if (speed.has("durationSec")) {
                path.speed.durationSec = speed.get("durationSec").getAsFloat();
            }
            if (speed.has("blocksPerSec")) {
                path.speed.blocksPerSec = speed.get("blocksPerSec").getAsFloat();
            }
        }
        
        // Segments
        if (json.has("segments")) {
            JsonArray segments = json.getAsJsonArray("segments");
            for (int i = 0; i < segments.size(); i++) {
                Segment segment = segmentFromJson(segments.get(i).getAsJsonObject());
                path.addSegment(segment);
            }
        }
        
        // Metadata
        if (json.has("lastModified")) {
            path.lastModified = json.get("lastModified").getAsLong();
        }
        if (json.has("lastModifier")) {
            path.lastModifier = UUID.fromString(json.get("lastModifier").getAsString());
        }
        
        return path;
    }
    
    private static Segment segmentFromJson(JsonObject json) {
        // Position
        JsonArray posArray = json.getAsJsonArray("p");
        Vector3f position = new Vector3f(
            posArray.get(0).getAsFloat(),
            posArray.get(1).getAsFloat(),
            posArray.get(2).getAsFloat()
        );
        
        // Orientation
        Quaternionf orientation = new Quaternionf();
        if (json.has("rot")) {
            JsonObject rot = json.getAsJsonObject("rot");
            if (rot.has("q")) {
                // Use quaternion if available (more precise)
                JsonArray quat = rot.getAsJsonArray("q");
                orientation.set(
                    quat.get(0).getAsFloat(),
                    quat.get(1).getAsFloat(),
                    quat.get(2).getAsFloat(),
                    quat.get(3).getAsFloat()
                );
            } else {
                // Convert from Euler angles
                float yaw = rot.has("yaw") ? rot.get("yaw").getAsFloat() : 0f;
                float pitch = rot.has("pitch") ? rot.get("pitch").getAsFloat() : 0f;
                float roll = rot.has("roll") ? rot.get("roll").getAsFloat() : 0f;
                
                orientation.rotateYXZ(
                    (float) Math.toRadians(yaw),
                    (float) Math.toRadians(pitch),
                    (float) Math.toRadians(roll)
                );
            }
        }
        
        Segment segment = new Segment(position, orientation);
        
        // Additional roll for banking
        if (json.has("roll")) {
            segment.roll = json.get("roll").getAsFloat();
        }
        
        // FOV
        if (json.has("fov")) {
            segment.fov = json.get("fov").getAsFloat();
        }
        
        // Timing
        if (json.has("durationSec")) {
            segment.durationSec = json.get("durationSec").getAsFloat();
        }
        if (json.has("weight")) {
            segment.weight = json.get("weight").getAsFloat();
        }
        
        // Per-segment overrides
        if (json.has("interp")) {
            segment.interpolationType = InterpolationType.fromString(json.get("interp").getAsString());
        }
        if (json.has("ease")) {
            segment.easingType = EasingType.fromString(json.get("ease").getAsString());
        }
        
        // Follow target
        if (json.has("lookAt")) {
            JsonObject lookAt = json.getAsJsonObject("lookAt");
            FollowTarget target = new FollowTarget();
            
            if (lookAt.has("type")) {
                String type = lookAt.get("type").getAsString().toUpperCase();
                target.type = FollowTarget.FollowType.valueOf(type);
            }
            if (lookAt.has("id")) {
                target.entityId = UUID.fromString(lookAt.get("id").getAsString());
            }
            if (lookAt.has("pos")) {
                JsonArray pos = lookAt.getAsJsonArray("pos");
                target.position = new Vector3f(
                    pos.get(0).getAsFloat(),
                    pos.get(1).getAsFloat(),
                    pos.get(2).getAsFloat()
                );
            }
            if (lookAt.has("followSpeed")) {
                target.followSpeed = lookAt.get("followSpeed").getAsFloat();
            }
            
            segment.followTarget = target;
        }
        
        // TCB parameters
        if (json.has("tcb")) {
            JsonObject tcb = json.getAsJsonObject("tcb");
            if (tcb.has("t")) segment.tension = tcb.get("t").getAsFloat();
            if (tcb.has("c")) segment.continuity = tcb.get("c").getAsFloat();
            if (tcb.has("b")) segment.bias = tcb.get("b").getAsFloat();
        }
        
        // Bezier handles
        if (json.has("bezier")) {
            JsonObject bezier = json.getAsJsonObject("bezier");
            if (bezier.has("in")) {
                JsonArray inArray = bezier.getAsJsonArray("in");
                segment.bezierIn = new Vector3f(
                    inArray.get(0).getAsFloat(),
                    inArray.get(1).getAsFloat(),
                    inArray.get(2).getAsFloat()
                );
            }
            if (bezier.has("out")) {
                JsonArray outArray = bezier.getAsJsonArray("out");
                segment.bezierOut = new Vector3f(
                    outArray.get(0).getAsFloat(),
                    outArray.get(1).getAsFloat(),
                    outArray.get(2).getAsFloat()
                );
            }
        }
        
        return segment;
    }
    
    /**
     * Migrate from JSON v1 to PathModel v2
     */
    private static PathModel migrateFromV1(JsonObject json) {
        String id = json.get("id").getAsString();
        PathModel path = new PathModel(id, id);
        
        // Migrate basic properties
        if (json.has("loop")) {
            path.setLoop(json.get("loop").getAsBoolean());
        }
        
        // Migrate speed (v1 used simple float)
        if (json.has("speed")) {
            float speed = json.get("speed").getAsFloat();
            // Assume it's a duration multiplier, convert to actual duration
            path.getSpeed().setDurationMode(10f / speed); // Base 10 second path
        }
        
        // Migrate keyframes to segments
        if (json.has("keyframes")) {
            JsonArray keyframes = json.getAsJsonArray("keyframes");
            for (int i = 0; i < keyframes.size(); i++) {
                JsonObject kf = keyframes.get(i).getAsJsonObject();
                
                // Position
                JsonObject pos = kf.getAsJsonObject("pos");
                Vector3f position = new Vector3f(
                    (float) pos.get("x").getAsDouble(),
                    (float) pos.get("y").getAsDouble(),
                    (float) pos.get("z").getAsDouble()
                );
                
                // Orientation from Euler or lookAt
                float yaw = 0f, pitch = 0f, roll = 0f;
                if (kf.has("rot")) {
                    JsonObject rot = kf.getAsJsonObject("rot");
                    yaw = rot.has("yaw") ? rot.get("yaw").getAsFloat() : 0f;
                    pitch = rot.has("pitch") ? rot.get("pitch").getAsFloat() : 0f;
                    roll = rot.has("roll") ? rot.get("roll").getAsFloat() : 0f;
                }
                
                Segment segment = new Segment(position, yaw, pitch, roll);
                
                // FOV
                if (kf.has("fov")) {
                    segment.fov = kf.get("fov").getAsFloat();
                }
                
                // Handle lookAt by creating follow target
                if (kf.has("lookAt")) {
                    JsonObject lookAt = kf.getAsJsonObject("lookAt");
                    FollowTarget target = new FollowTarget();
                    target.type = FollowTarget.FollowType.BLOCK;
                    target.position = new Vector3f(
                        (float) lookAt.get("x").getAsDouble(),
                        (float) lookAt.get("y").getAsDouble(),
                        (float) lookAt.get("z").getAsDouble()
                    );
                    segment.followTarget = target;
                }
                
                path.addSegment(segment);
            }
        }
        
        // Set reasonable defaults for migrated paths
        path.getDefaults().interpolationType = InterpolationType.CATMULL_CENTRIPETAL;
        path.getDefaults().easingType = EasingType.CUBIC_IN_OUT;
        
        return path;
    }
    
    /**
     * Convert to NBT
     */
    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        
        nbt.putString("id", id);
        nbt.putString("name", name);
        nbt.putInt("version", version);
        nbt.putBoolean("loop", loop);
        
        // Serialize as JSON string for now (NBT is mainly for save data)
        nbt.putString("jsonData", toJson(new Gson()).toString());
        
        nbt.putLong("lastModified", lastModified);
        nbt.putString("lastModifier", lastModifier.toString());
        
        return nbt;
    }
    
    /**
     * Load from NBT
     */
    public static PathModel fromNBT(CompoundTag nbt) {
        if (nbt.contains("jsonData")) {
            // Load from embedded JSON
            String jsonData = nbt.getString("jsonData");
            JsonObject json = new Gson().fromJson(jsonData, JsonObject.class);
            return fromJson(json, new Gson());
        }
        
        // Fallback for basic NBT-only data
        String id = nbt.getString("id");
        String name = nbt.contains("name") ? nbt.getString("name") : id;
        PathModel path = new PathModel(id, name);
        
        if (nbt.contains("loop")) {
            path.setLoop(nbt.getBoolean("loop"));
        }
        
        if (nbt.contains("lastModified")) {
            path.lastModified = nbt.getLong("lastModified");
        }
        if (nbt.contains("lastModifier")) {
            path.lastModifier = UUID.fromString(nbt.getString("lastModifier"));
        }
        
        return path;
    }
}