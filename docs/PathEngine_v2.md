# Aperture-API Path Engine v2: CMDCam Parity

This document describes the new path engine implemented in Aperture-API to achieve full parity with CMDCam's buttery-smooth camera movement and advanced features.

## Overview

The new Path Engine v2 provides:

- **Arc-length parameterization** for constant-speed playback
- **Centripetal Catmull-Rom splines** as default (α=0.5) for stable, smooth interpolation
- **Quaternion-based orientations** to prevent gimbal lock
- **Banking system** for natural camera movement in turns
- **Comprehensive easing functions** for professional animation
- **Per-segment customization** of interpolation and timing
- **JSON v2 schema** with backward compatibility
- **Real-time path evaluation** with debug utilities

## Key Features

### 1. Arc-Length Parameterization

Traditional camera systems use time-based parameters that can cause uneven visual speed when keyframes are unevenly spaced. Our arc-length system ensures constant visual speed:

```java
// Duration mode: 10 seconds total regardless of path complexity
path.getSpeed().setDurationMode(10f);

// Speed mode: constant 5 blocks per second
path.getSpeed().setSpeedMode(5f);
```

The system automatically builds adaptive lookup tables that sample more densely in curved sections for accuracy.

### 2. Interpolation Types

| Type | Description | Use Case |
|------|-------------|----------|
| `LINEAR` | Straight lines | Sharp, mechanical movement |
| `COSINE` | Smooth cosine transitions | Gentle easing |
| `HERMITE` | TCB (Tension/Continuity/Bias) | Fine-tuned control |
| `BEZIER` | Cubic Bezier with handles | Maximum artistic control |
| `CATMULL_UNIFORM` | α=0.0 Catmull-Rom | Uniform parameterization |
| **`CATMULL_CENTRIPETAL`** | **α=0.5 Catmull-Rom** | **Default - prevents loops/cusps** |
| `CATMULL_CHORDAL` | α=1.0 Catmull-Rom | Tight curves |

**Default Choice**: Centripetal Catmull-Rom (α=0.5) provides the best balance of smoothness and stability, preventing the self-intersecting loops that can occur with uniform Catmull-Rom.

### 3. Easing Functions

Professional animation easing with 27 different types:

- **Linear**: Constant velocity
- **Quadratic/Cubic/Quartic/Quintic**: Polynomial curves (In/Out/InOut variants)
- **Sinusoidal**: Smooth, natural feeling
- **Exponential**: Dramatic acceleration/deceleration
- **Circular**: Arc-based transitions
- **Back**: Overshoot effects
- **Elastic**: Spring-like movement
- **Bounce**: Bouncing ball effect

### 4. Banking System

Automatic banking (roll in turns) simulates aircraft-like movement:

```java
path.getDefaults().banking = true;
path.getDefaults().bankingStrength = 0.7f; // 70% strength
path.getDefaults().rollMix = 0.5f; // 50/50 mix with keyframed roll
```

Banking is calculated based on path curvature and velocity direction.

### 5. Quaternion Orientations

All rotations use quaternions internally to prevent gimbal lock:

```java
// Create segment with Euler angles (converted to quaternions)
Segment segment = new Segment(position, yaw, pitch, roll);

// Or directly with quaternions for precision
Segment segment = new Segment(position, quaternion);
```

### 6. JSON v2 Schema

Complete schema with backward compatibility:

```json
{
  "version": 2,
  "id": "my_path",
  "name": "My Camera Path",
  "loop": false,
  "defaults": {
    "interp": "catmull:centripetal",
    "ease": "cubicInOut", 
    "speedMode": "duration",
    "banking": true,
    "bankingStrength": 0.5,
    "rollMix": 0.5
  },
  "speed": {
    "durationSec": 10.0,
    "blocksPerSec": null
  },
  "segments": [
    {
      "p": [0, 70, 0],
      "rot": {
        "yaw": 0, "pitch": 0, "roll": 0,
        "q": [0, 0, 0, 1]
      },
      "roll": 5.0,
      "fov": 90.0,
      "durationSec": 2.0,
      "interp": "bezier",
      "ease": "sineInOut",
      "lookAt": {
        "type": "block",
        "pos": [10, 72, 5],
        "followSpeed": 1.0
      },
      "tcb": {"t": 0.0, "c": 0.0, "b": 0.0},
      "bezier": {
        "in": [0, 0, 0],
        "out": [2, 1, 0]
      }
    }
  ]
}
```

## Usage Examples

### Basic Path Creation

```java
// Simple linear path
PathModel path = PathEvaluator.createSimplePath("test", 
    new Vector3f(0, 70, 0), new Vector3f(10, 75, 5), 5.0f);

// Complex test path with multiple segments
PathModel complexPath = PathEvaluator.createTestPath();
```

### Path Evaluation

```java
// Evaluate at specific time
PathEvaluator.EvaluationResult result = PathEvaluator.evaluateAtTime(path, 2.5f);
Vector3f position = result.position;
Quaternionf orientation = result.orientation;
float roll = result.roll;
float fov = result.fov;

// Get velocity vector
Vector3f velocity = PathEvaluator.getVelocityAtTime(path, 2.5f);

// Debug information
String debug = PathEvaluator.getDebugInfo(path, 2.5f);
```

### Path Customization

```java
PathModel path = new PathModel("custom", "Custom Path");

// Set defaults
path.getDefaults().interpolationType = InterpolationType.CATMULL_CENTRIPETAL;
path.getDefaults().easingType = EasingType.CUBIC_IN_OUT;
path.getDefaults().banking = true;

// Add segments with per-segment overrides
PathModel.Segment segment1 = new PathModel.Segment(pos1, yaw1, pitch1, roll1);
segment1.interpolationType = InterpolationType.BEZIER; // Override default
segment1.easingType = EasingType.BACK_OUT; // Override default

PathModel.Segment segment2 = new PathModel.Segment(pos2, yaw2, pitch2, roll2);
segment2.followTarget = new PathModel.FollowTarget();
segment2.followTarget.type = PathModel.FollowTarget.FollowType.ENTITY;
segment2.followTarget.entityId = playerUUID;
segment2.followTarget.followSpeed = 0.8f;

path.addSegment(segment1);
path.addSegment(segment2);
```

### Command Usage

```
# Set interpolation for a path
/camera interpolation my_path catmull:centripetal

# Set constant speed (5 blocks per second)
/camera speed my_path blocks 5.0

# Set duration mode (10 seconds total)
/camera speed my_path duration 10.0

# Create and test
/camera test
/camera debug my_path 2.5
```

## Performance Considerations

### Arc-Length Lookup Tables
- Built once per path modification using adaptive sampling
- Cached until path geometry changes
- Binary search for O(log n) parameter conversion
- Memory usage: ~8KB per 1000 sample points

### Interpolation Performance
- Catmull-Rom: 4 vector operations per evaluation
- Bezier: 4 vector operations per evaluation  
- Linear: 1 vector lerp per evaluation
- Quaternion slerp: ~10 operations per evaluation

### Recommended Settings
- **Simple paths (2-4 segments)**: Any interpolation type
- **Complex paths (10+ segments)**: Catmull-Rom or Linear for performance
- **High-precision cinematics**: Bezier with custom handles
- **Real-time preview**: Linear or Cosine for responsive feedback

## Migration from v1

The system automatically migrates v1 JSON:

```java
// Detect and migrate v1 format
PathModel path = PathModel.fromJson(jsonObject, gson);
// Automatically applies sensible defaults for new features

// Manual migration available via commands
/camera migrate old_path_name
```

**Migration Defaults**:
- Interpolation: `catmull:centripetal` 
- Easing: `cubicInOut`
- Banking: `false` (preserves existing behavior)
- Speed mode: `duration` based on original speed multiplier

## Integration Points

### Client Rendering
- Real-time path visualization via `PathDebugRenderer`
- Frustum culling for performance
- Distance-based level-of-detail for gizmos

### Server Commands
- `CameraCommandsV2` provides `/camera` command tree
- Integration with existing `/aperture` commands
- Permission checks and validation

### Data Storage
- NBT serialization for world save data
- JSON export/import for sharing
- Datapack integration for server-side paths

## Testing

Comprehensive unit tests cover:
- Basic path creation and evaluation
- Arc-length parameterization accuracy
- JSON serialization round-trip
- Interpolation type behavior
- Easing function correctness
- Edge cases and error conditions

Run tests with:
```bash
./gradlew test --tests "net.tysontheember.apertureapi.path.*"
```

## Troubleshooting

### Common Issues

**Path appears jerky despite smooth interpolation**:
- Check if using speed mode with appropriate blocks/second value
- Verify arc-length LUT is being built (check for path modifications)
- Consider switching from `duration` to `speed` mode for constant velocity

**Gimbal lock in rotations**:
- Ensure quaternions are being used (check JSON has both Euler and "q" fields)
- Avoid rapid 180° rotation changes
- Use `banking` to reduce manual roll keyframing

**Poor performance with complex paths**:
- Reduce arc-length sampling density (modify `calculateOptimalSamples`)
- Switch to Linear interpolation for preview/editing
- Use LOD system for distant camera paths

**Unexpected banking behavior**:
- Check `bankingStrength` and `rollMix` values
- Disable banking for paths that should maintain level orientation
- Banking requires at least 3 segments to calculate curvature

### Debug Tools

```java
// Path statistics
float duration = PathEvaluator.getTotalDuration(path);
float length = PathEvaluator.getTotalLength(path);
PathModel.ArcLengthLUT lut = path.getArcLengthLUT();

// Sample path for analysis
PathEvaluator.EvaluationResult[] samples = PathEvaluator.samplePath(path, 100);

// Detailed debug at specific time
String debugInfo = PathEvaluator.getDebugInfo(path, timeSeconds);
```

## Conclusion

The new Path Engine v2 provides professional-grade camera movement with the stability and smoothness of CMDCam while maintaining full backward compatibility with existing Aperture-API content. The combination of arc-length parameterization, centripetal Catmull-Rom interpolation, and quaternion orientations ensures buttery-smooth camera movement without the common artifacts of traditional keyframe systems.

For questions or issues, see the project's GitHub repository or join the community discussions.