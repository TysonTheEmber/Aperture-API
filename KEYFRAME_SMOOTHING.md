# Keyframe Smoothing Guide

This guide explains how to eliminate keyframe jumps and create smooth, cinematic camera animations in apetureapi.

## The Problem

When you reach a keyframe, you can easily tell because there is a movement/rotation jump that breaks the smooth cinematic feel. This happens due to:

1. **Linear interpolation defaults** - Sharp transitions between keyframes
2. **Mismatched interpolation types** - Different curves for position/rotation/FOV
3. **STEP interpolation** - Instant jumps without transition
4. **Poor bezier control points** - Incorrectly configured curves

## Fixed Issues

### Critical Bug Fixes

The following interpolation bugs have been fixed in `Animator.java` and `PreviewAnimator.java`:
- Rotation interpolation was incorrectly using position interpolation settings
- FOV interpolation was incorrectly using rotation bezier curves instead of FOV curves

These fixes alone should significantly improve your camera transitions.

## Solutions

### 1. Automatic Smoothing (Recommended)

The easiest way to fix keyframe jumps is to use the automatic smoothing system:

```java
// For any GlobalCameraPath object
GlobalCameraPath path = CameraAnimIdeCache.getPath();

// Auto-detect and fix all jumps in one call
path.autoSmoothAndFixJumps();

// Or do it step by step:
int[] jumps = path.detectJumps();
if (jumps.length > 0) {
    path.fixJumps(jumps);
}
path.autoSmooth();
```

### 2. Manual Smoothing

For specific keyframes, you can apply manual smoothing:

```java
CameraKeyframe keyframe = path.getPoint(time);

// Apply cinematic smoothing (SMOOTH path + BEZIER time curves)
SmoothingUtils.applyCinematicSmoothing(keyframe, SmoothingUtils.EaseType.EASE_IN_OUT);

// Or apply bezier path smoothing for maximum control
SmoothingUtils.applyBezierSmoothing(keyframe, SmoothingUtils.EaseType.EASE_IN_OUT);

// Update bezier control points
path.updateBezier(time);
```

### 3. Command Line Tools

Use in-game commands to smooth your camera paths:

```
/apertureapi smooth validate  - Analyze path and detect issues
/apertureapi smooth detect   - Find keyframe jumps
/apertureapi smooth fix      - Fix detected jumps
/apertureapi smooth auto     - Apply overall smoothing
```

## Easing Types

Choose the right easing type based on your animation needs:

- **EASE_IN_OUT**: Smooth start and end, fast middle (best for most cases)
- **EASE_IN**: Slow start, fast end (good for starting movements)  
- **EASE_OUT**: Fast start, slow end (good for ending movements)
- **EASE**: General smooth easing (balanced option)

## Interpolation Types Explained

### Path Interpolation
Controls how the camera moves between keyframe positions:
- **LINEAR**: Straight line movement (can cause sharp direction changes)
- **SMOOTH**: Catmull-Rom splines using neighboring keyframes (natural curves)
- **BEZIER**: Custom bezier curves with manual control points (maximum control)
- **STEP**: Instant jump (always causes jumps - avoid for smooth animation)

### Time Interpolation  
Controls the speed curve over time:
- **LINEAR**: Constant speed (can cause sudden speed changes)
- **BEZIER**: Custom speed curves (eliminates sudden acceleration/deceleration)

## Best Practices

### For Cinematic Animations:
1. Use **SMOOTH** path interpolation for natural camera movement
2. Use **BEZIER** time interpolation with **EASE_IN_OUT** curves
3. Avoid **STEP** and **LINEAR** interpolation for smooth results
4. Run validation regularly: `/apertureapi smooth validate`

### For Complex Movements:
1. Use **BEZIER** path interpolation for full control
2. Manually adjust bezier control points in the editor
3. Use different easing types for different movement phases

### Quick Fix Workflow:
1. Record your keyframes normally
2. Run `/apertureapi smooth detect` to find issues
3. Run `/apertureapi smooth auto` to apply automatic smoothing
4. Fine-tune specific keyframes if needed

## Technical Details

### Jump Detection Criteria

The system detects potential jumps when:
- Position changes > 0.5 blocks/tick with LINEAR interpolation
- Rotation changes > 5°/tick with LINEAR time interpolation  
- FOV changes > 1°/tick with LINEAR time interpolation
- Any keyframe uses STEP interpolation

### Smoothing Algorithm

The auto-smoothing system:
1. Analyzes movement intensity between keyframes
2. Chooses appropriate interpolation types (SMOOTH vs BEZIER paths)
3. Selects optimal easing curves based on movement characteristics
4. Updates bezier control points for seamless transitions

### Performance Impact

Smoothing operations are:
- **One-time cost**: Applied when you run smoothing commands
- **Runtime optimized**: Uses the same interpolation math as before
- **Memory efficient**: No significant additional memory usage

## Troubleshooting

### Still seeing jumps after smoothing?
- Check if you have very short time intervals between keyframes
- Verify all three components (position, rotation, FOV) are properly smoothed
- Try manual bezier curves for problematic segments

### Animation feels too slow/fast?
- Adjust time interpolation curves using the interpolation settings screen
- Use EASE_IN for faster starts, EASE_OUT for faster ends

### Lost manual keyframe adjustments?
- Smoothing preserves keyframe positions and rotations
- Only interpolation settings and bezier control points are modified
- You can always undo by manually setting interpolation back to LINEAR

## Examples

### Simple Smoothing
```java
// Get current path and smooth it
GlobalCameraPath path = CameraAnimIdeCache.getPath();
path.autoSmoothAndFixJumps();
```

### Targeted Smoothing
```java
// Smooth only specific keyframes
CameraKeyframe problematicKeyframe = path.getPoint(100);
SmoothingUtils.applyCinematicSmoothing(problematicKeyframe, SmoothingUtils.EaseType.EASE_IN_OUT);
path.updateBezier(100);
```

### Validation and Analysis
```java
// Check path quality
int[] jumps = path.detectJumps();
System.out.println("Found " + jumps.length + " potential jumps");

// Fix only detected issues
if (jumps.length > 0) {
    path.fixJumps(jumps);
}
```

This smoothing system should eliminate the keyframe jumps you're experiencing and give you smooth, professional-looking camera animations!