package net.tysontheember.apertureapi.path;

import net.tysontheember.apertureapi.path.interpolation.EasingType;
import net.tysontheember.apertureapi.path.interpolation.InterpolationType;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic unit tests for the new path system demonstrating CMDCam parity features.
 */
public class PathSystemTest {

    @Test
    public void testBasicPathCreation() {
        PathModel path = new PathModel("test", "Test Path");
        
        assertEquals("test", path.getId());
        assertEquals("Test Path", path.getName());
        assertEquals(2, path.getVersion());
        assertFalse(path.isLoop());
        assertTrue(path.getSegments().isEmpty());
        
        // Test defaults
        assertEquals(InterpolationType.CATMULL_CENTRIPETAL, path.getDefaults().interpolationType);
        assertEquals(EasingType.CUBIC_IN_OUT, path.getDefaults().easingType);
        assertEquals(PathModel.PathDefaults.SpeedMode.DURATION, path.getDefaults().speedMode);
        assertFalse(path.getDefaults().banking);
        assertEquals(1.0f, path.getDefaults().bankingStrength, 0.001f);
        assertEquals(0.5f, path.getDefaults().rollMix, 0.001f);
    }
    
    @Test
    public void testSegmentCreation() {
        Vector3f position = new Vector3f(10, 70, 5);
        PathModel.Segment segment = new PathModel.Segment(position, 45f, -10f, 5f);
        
        assertEquals(position, segment.position);
        assertEquals(5f, segment.roll, 0.001f);
        assertEquals(90f, segment.fov, 0.001f);
        
        // Test Euler angle conversion
        Vector3f euler = segment.toEulerDegrees();
        assertEquals(-10f, euler.x, 1f); // pitch
        assertEquals(45f, euler.y, 1f);  // yaw
        assertEquals(0f, euler.z, 1f);   // internal roll (separate from banking roll)
    }
    
    @Test
    public void testPathEvaluation() {
        // Create a simple 2-point path
        PathModel path = PathEvaluator.createSimplePath("test", 
            new Vector3f(0, 70, 0), new Vector3f(10, 75, 5), 2.0f);
        
        assertEquals(2, path.getSegments().size());
        assertEquals(2.0f, path.getSpeed().durationSec, 0.001f);
        assertNull(path.getSpeed().blocksPerSec);
        
        // Test evaluation at different points
        PathEvaluator.EvaluationResult start = PathEvaluator.evaluateAtTime(path, 0f);
        PathEvaluator.EvaluationResult middle = PathEvaluator.evaluateAtTime(path, 1f);
        PathEvaluator.EvaluationResult end = PathEvaluator.evaluateAtTime(path, 2f);
        
        // Start should be at first segment
        assertEquals(0, start.segmentIndex);
        assertEquals(0f, start.segmentProgress, 0.001f);
        assertEquals(0f, start.position.x, 0.001f);
        assertEquals(70f, start.position.y, 0.001f);
        assertEquals(0f, start.position.z, 0.001f);
        
        // Middle should be interpolated
        assertEquals(0, middle.segmentIndex);
        assertEquals(0.5f, middle.segmentProgress, 0.001f);
        assertEquals(5f, middle.position.x, 1f);    // Approximately halfway
        assertEquals(72.5f, middle.position.y, 1f); // Approximately halfway
        assertEquals(2.5f, middle.position.z, 1f);  // Approximately halfway
        
        // End should be at second segment
        assertEquals(0, end.segmentIndex);
        assertEquals(1f, end.segmentProgress, 0.001f);
        assertEquals(10f, end.position.x, 0.001f);
        assertEquals(75f, end.position.y, 0.001f);
        assertEquals(5f, end.position.z, 0.001f);
    }
    
    @Test
    public void testArcLengthParameterization() {
        PathModel path = PathEvaluator.createSimplePath("test", 
            new Vector3f(0, 0, 0), new Vector3f(10, 0, 0), 5.0f);
        
        PathModel.ArcLengthLUT lut = path.getArcLengthLUT();
        
        // Total length should be approximately 10 blocks
        float totalLength = lut.getTotalLength();
        assertEquals(10f, totalLength, 0.1f);
        
        // Test parameter-to-arc-length conversion
        float halfwayArcLength = lut.parameterToArcLength(0.5f);
        assertEquals(5f, halfwayArcLength, 0.1f);
        
        // Test arc-length-to-parameter conversion
        float halfwayParameter = lut.arcLengthToParameter(5f);
        assertEquals(0.5f, halfwayParameter, 0.01f);
    }
    
    @Test
    public void testConstantSpeedMode() {
        PathModel path = PathEvaluator.createSimplePath("test", 
            new Vector3f(0, 0, 0), new Vector3f(20, 0, 0), 5.0f);
        
        // Switch to speed mode: 2 blocks per second
        path.getSpeed().setSpeedMode(2.0f);
        
        assertTrue(path.getSpeed().isSpeedMode());
        assertEquals(2.0f, path.getSpeed().blocksPerSec, 0.001f);
        
        // Duration should now be length/speed = 20/2 = 10 seconds
        float totalDuration = PathEvaluator.getTotalDuration(path);
        assertEquals(10f, totalDuration, 0.1f);
        
        // Test evaluation at constant speed
        PathEvaluator.EvaluationResult result1 = PathEvaluator.evaluateAtTime(path, 1f);
        PathEvaluator.EvaluationResult result2 = PathEvaluator.evaluateAtTime(path, 2f);
        
        // Should move 2 blocks per second
        float distance = result2.position.distance(result1.position);
        assertEquals(2f, distance, 0.2f);
    }
    
    @Test
    public void testInterpolationTypes() {
        // Test that all interpolation types parse correctly
        assertEquals(InterpolationType.LINEAR, InterpolationType.fromString("linear"));
        assertEquals(InterpolationType.COSINE, InterpolationType.fromString("cosine"));
        assertEquals(InterpolationType.HERMITE, InterpolationType.fromString("hermite"));
        assertEquals(InterpolationType.BEZIER, InterpolationType.fromString("bezier"));
        assertEquals(InterpolationType.CATMULL_UNIFORM, InterpolationType.fromString("catmull:uniform"));
        assertEquals(InterpolationType.CATMULL_CENTRIPETAL, InterpolationType.fromString("catmull:centripetal"));
        assertEquals(InterpolationType.CATMULL_CHORDAL, InterpolationType.fromString("catmull:chordal"));
        
        // Test legacy mapping
        assertEquals(InterpolationType.CATMULL_CENTRIPETAL, InterpolationType.fromString("smooth"));
        assertEquals(InterpolationType.LINEAR, InterpolationType.fromString("step"));
        
        // Test alpha values
        assertEquals(0.0f, InterpolationType.CATMULL_UNIFORM.getCatmullAlpha(), 0.001f);
        assertEquals(0.5f, InterpolationType.CATMULL_CENTRIPETAL.getCatmullAlpha(), 0.001f);
        assertEquals(1.0f, InterpolationType.CATMULL_CHORDAL.getCatmullAlpha(), 0.001f);
    }
    
    @Test
    public void testEasingFunctions() {
        // Test basic easing function behavior
        assertEquals(0f, EasingType.LINEAR.apply(0f), 0.001f);
        assertEquals(1f, EasingType.LINEAR.apply(1f), 0.001f);
        assertEquals(0.5f, EasingType.LINEAR.apply(0.5f), 0.001f);
        
        // Test cubic ease in/out behavior
        assertTrue(EasingType.CUBIC_IN.apply(0.5f) < 0.5f); // Slow start
        assertTrue(EasingType.CUBIC_OUT.apply(0.5f) > 0.5f); // Fast start
        
        // Test parsing
        assertEquals(EasingType.CUBIC_IN_OUT, EasingType.fromString("cubicInOut"));
        assertEquals(EasingType.CUBIC_IN_OUT, EasingType.fromString("cubic"));
        assertEquals(EasingType.CUBIC_IN_OUT, EasingType.fromString("ease"));
    }
    
    @Test
    public void testJSONSerialization() {
        // Create a test path with various features
        PathModel originalPath = new PathModel("test_json", "JSON Test Path");
        originalPath.setLoop(true);
        originalPath.getDefaults().interpolationType = InterpolationType.CATMULL_CENTRIPETAL;
        originalPath.getDefaults().easingType = EasingType.CUBIC_IN_OUT;
        originalPath.getDefaults().banking = true;
        originalPath.getDefaults().bankingStrength = 0.7f;
        
        // Add some segments
        PathModel.Segment segment1 = new PathModel.Segment(new Vector3f(0, 70, 0), 0f, 0f, 5f);
        segment1.fov = 85f;
        originalPath.addSegment(segment1);
        
        PathModel.Segment segment2 = new PathModel.Segment(new Vector3f(10, 75, 5), 45f, -10f, -5f);
        segment2.fov = 95f;
        segment2.interpolationType = InterpolationType.BEZIER;
        segment2.easingType = EasingType.SINE_IN_OUT;
        originalPath.addSegment(segment2);
        
        // Serialize to JSON
        com.google.gson.Gson gson = new com.google.gson.Gson();
        com.google.gson.JsonObject json = originalPath.toJson(gson);
        
        // Verify JSON structure
        assertEquals(2, json.get("version").getAsInt());
        assertEquals("test_json", json.get("id").getAsString());
        assertEquals("JSON Test Path", json.get("name").getAsString());
        assertTrue(json.get("loop").getAsBoolean());
        
        // Deserialize back
        PathModel deserializedPath = PathModel.fromJson(json, gson);
        
        // Verify deserialization
        assertEquals("test_json", deserializedPath.getId());
        assertEquals("JSON Test Path", deserializedPath.getName());
        assertTrue(deserializedPath.isLoop());
        assertEquals(InterpolationType.CATMULL_CENTRIPETAL, deserializedPath.getDefaults().interpolationType);
        assertEquals(EasingType.CUBIC_IN_OUT, deserializedPath.getDefaults().easingType);
        assertTrue(deserializedPath.getDefaults().banking);
        assertEquals(0.7f, deserializedPath.getDefaults().bankingStrength, 0.001f);
        
        assertEquals(2, deserializedPath.getSegments().size());
        
        PathModel.Segment deSegment1 = deserializedPath.getSegments().get(0);
        assertEquals(85f, deSegment1.fov, 0.001f);
        assertEquals(5f, deSegment1.roll, 0.001f);
        
        PathModel.Segment deSegment2 = deserializedPath.getSegments().get(1);
        assertEquals(95f, deSegment2.fov, 0.001f);
        assertEquals(-5f, deSegment2.roll, 0.001f);
        assertEquals(InterpolationType.BEZIER, deSegment2.interpolationType);
        assertEquals(EasingType.SINE_IN_OUT, deSegment2.easingType);
    }
    
    @Test
    public void testPathInfoUtilities() {
        PathModel path = PathEvaluator.createTestPath();
        
        // Test utility methods
        float duration = PathEvaluator.getTotalDuration(path);
        float length = PathEvaluator.getTotalLength(path);
        
        assertTrue(duration > 0f);
        assertTrue(length > 0f);
        
        // Test debug info
        String debugInfo = PathEvaluator.getDebugInfo(path, 2.5f);
        assertNotNull(debugInfo);
        assertTrue(debugInfo.contains("Time: 2.50s"));
        assertTrue(debugInfo.contains("Position:"));
        assertTrue(debugInfo.contains("Velocity:"));
        
        // Test sampling
        PathEvaluator.EvaluationResult[] samples = PathEvaluator.samplePath(path, 10);
        assertEquals(10, samples.length);
        
        // First sample should be at start
        assertEquals(0, samples[0].segmentIndex);
        assertEquals(0f, samples[0].segmentProgress, 0.001f);
        
        // Last sample should be at end
        assertEquals(path.getSegments().size() - 2, samples[9].segmentIndex);
        assertEquals(1f, samples[9].segmentProgress, 0.001f);
    }
}