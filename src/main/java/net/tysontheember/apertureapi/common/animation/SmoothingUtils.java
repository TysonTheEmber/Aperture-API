package net.tysontheember.apertureapi.common.animation;

import org.joml.Vector3f;

/**
 * Utility class for smoothing keyframe transitions to eliminate camera jumps
 * and create cinematic camera animations.
 */
public class SmoothingUtils {
    
    /**
     * Apply cinematic smoothing to a keyframe to eliminate jumps.
     * This sets the keyframe to use SMOOTH path interpolation with BEZIER time curves.
     * 
     * @param keyframe The keyframe to smooth
     * @param easeType The type of easing to apply (IN, OUT, IN_OUT, or CUSTOM)
     */
    public static void applyCinematicSmoothing(CameraKeyframe keyframe, EaseType easeType) {
        // Set path interpolation to SMOOTH for natural camera movement
        keyframe.setPathInterpolator(PathInterpolator.SMOOTH);
        
        // Apply BEZIER time interpolation with appropriate curves
        keyframe.setPosTimeInterpolator(TimeInterpolator.BEZIER);
        keyframe.setRotTimeInterpolator(TimeInterpolator.BEZIER);
        keyframe.setFovTimeInterpolator(TimeInterpolator.BEZIER);
        
        // Configure bezier curves based on ease type
        switch (easeType) {
            case EASE_IN:
                keyframe.getPosBezier().easyIn();
                keyframe.getRotBezier().easyIn();
                keyframe.getFovBezier().easyIn();
                break;
            case EASE_OUT:
                keyframe.getPosBezier().easyOut();
                keyframe.getRotBezier().easyOut();
                keyframe.getFovBezier().easyOut();
                break;
            case EASE_IN_OUT:
                keyframe.getPosBezier().easyInOut();
                keyframe.getRotBezier().easyInOut();
                keyframe.getFovBezier().easyInOut();
                break;
            case EASE:
                keyframe.getPosBezier().easy();
                keyframe.getRotBezier().easy();
                keyframe.getFovBezier().easy();
                break;
        }
    }
    
    /**
     * Apply bezier path smoothing to a keyframe for maximum control over movement curves.
     * This provides the smoothest possible transitions but requires more setup.
     * 
     * @param keyframe The keyframe to smooth
     * @param easeType The type of easing to apply
     */
    public static void applyBezierSmoothing(CameraKeyframe keyframe, EaseType easeType) {
        // Set path interpolation to BEZIER for maximum smoothness
        keyframe.setPathInterpolator(PathInterpolator.BEZIER);
        
        // Apply BEZIER time interpolation
        keyframe.setPosTimeInterpolator(TimeInterpolator.BEZIER);
        keyframe.setRotTimeInterpolator(TimeInterpolator.BEZIER);
        keyframe.setFovTimeInterpolator(TimeInterpolator.BEZIER);
        
        // Configure curves
        switch (easeType) {
            case EASE_IN:
                keyframe.getPosBezier().easyIn();
                keyframe.getRotBezier().easyIn();
                keyframe.getFovBezier().easyIn();
                break;
            case EASE_OUT:
                keyframe.getPosBezier().easyOut();
                keyframe.getRotBezier().easyOut();
                keyframe.getFovBezier().easyOut();
                break;
            case EASE_IN_OUT:
                keyframe.getPosBezier().easyInOut();
                keyframe.getRotBezier().easyInOut();
                keyframe.getFovBezier().easyInOut();
                break;
            case EASE:
                keyframe.getPosBezier().easy();
                keyframe.getRotBezier().easy();
                keyframe.getFovBezier().easy();
                break;
        }
    }
    
    /**
     * Auto-smooth an entire camera path to eliminate all keyframe jumps.
     * This analyzes the path and applies appropriate smoothing to each segment.
     * 
     * @param path The camera path to smooth
     */
    public static void autoSmoothPath(GlobalCameraPath path) {
        var keyframes = path.getEntries();
        CameraKeyframe previousKeyframe = null;
        
        for (var entry : keyframes) {
            CameraKeyframe keyframe = entry.getValue();
            
            if (previousKeyframe != null) {
                // Calculate movement characteristics between keyframes
                Vector3f posDiff = new Vector3f(keyframe.getPos()).sub(previousKeyframe.getPos());
                Vector3f rotDiff = new Vector3f(keyframe.getRot()).sub(previousKeyframe.getRot());
                float fovDiff = Math.abs(keyframe.getFov() - previousKeyframe.getFov());
                
                // Determine appropriate smoothing based on movement intensity
                EaseType easeType = determineOptimalEasing(posDiff, rotDiff, fovDiff);
                
                // Apply smoothing
                if (shouldUseBezierPath(posDiff, rotDiff)) {
                    applyBezierSmoothing(keyframe, easeType);
                } else {
                    applyCinematicSmoothing(keyframe, easeType);
                }
            }
            
            previousKeyframe = keyframe;
        }
        
        // Update bezier control points for all keyframes
        for (var entry : keyframes) {
            path.updateBezier(entry.getIntKey());
        }
    }
    
    /**
     * Detect potential keyframe jumps in a camera path.
     * 
     * @param path The camera path to analyze
     * @return Array of time indices where jumps are detected
     */
    public static int[] detectKeyframeJumps(GlobalCameraPath path) {
        var keyframes = path.getEntries();
        var jumpTimes = new java.util.ArrayList<Integer>();
        CameraKeyframe previousKeyframe = null;
        int previousTime = 0;
        
        for (var entry : keyframes) {
            int time = entry.getIntKey();
            CameraKeyframe keyframe = entry.getValue();
            
            if (previousKeyframe != null) {
                float timeDelta = time - previousTime;
                
                // Calculate movement rates
                Vector3f posDiff = new Vector3f(keyframe.getPos()).sub(previousKeyframe.getPos());
                Vector3f rotDiff = new Vector3f(keyframe.getRot()).sub(previousKeyframe.getRot());
                float fovDiff = Math.abs(keyframe.getFov() - previousKeyframe.getFov());
                
                float posRate = posDiff.length() / timeDelta;
                float rotRate = rotDiff.length() / timeDelta;
                float fovRate = fovDiff / timeDelta;
                
                // Check for sudden changes that would cause jumps
                boolean hasJump = false;
                
                // Position jump threshold (blocks per tick)
                if (posRate > 0.5f && keyframe.getPathInterpolator() == PathInterpolator.LINEAR) {
                    hasJump = true;
                }
                
                // Rotation jump threshold (degrees per tick)
                if (rotRate > 5.0f && keyframe.getRotTimeInterpolator() == TimeInterpolator.LINEAR) {
                    hasJump = true;
                }
                
                // FOV jump threshold (degrees per tick)  
                if (fovRate > 1.0f && keyframe.getFovTimeInterpolator() == TimeInterpolator.LINEAR) {
                    hasJump = true;
                }
                
                // STEP interpolation always causes jumps
                if (keyframe.getPathInterpolator() == PathInterpolator.STEP) {
                    hasJump = true;
                }
                
                if (hasJump) {
                    jumpTimes.add(time);
                }
            }
            
            previousKeyframe = keyframe;
            previousTime = time;
        }
        
        return jumpTimes.stream().mapToInt(Integer::intValue).toArray();
    }
    
    /**
     * Apply targeted smoothing to specific keyframes that have jumps.
     * 
     * @param path The camera path
     * @param jumpTimes Array of time indices with detected jumps
     */
    public static void fixDetectedJumps(GlobalCameraPath path, int[] jumpTimes) {
        for (int time : jumpTimes) {
            CameraKeyframe keyframe = path.getPoint(time);
            if (keyframe != null) {
                // Apply appropriate smoothing based on the type of jump
                CameraKeyframe previous = path.getPrePoint(time);
                if (previous != null) {
                    Vector3f posDiff = new Vector3f(keyframe.getPos()).sub(previous.getPos());
                    Vector3f rotDiff = new Vector3f(keyframe.getRot()).sub(previous.getRot());
                    float fovDiff = Math.abs(keyframe.getFov() - previous.getFov());
                    
                    EaseType easeType = determineOptimalEasing(posDiff, rotDiff, fovDiff);
                    applyCinematicSmoothing(keyframe, easeType);
                }
            }
        }
        
        // Update bezier control points
        for (int time : jumpTimes) {
            path.updateBezier(time);
        }
    }
    
    private static EaseType determineOptimalEasing(Vector3f posDiff, Vector3f rotDiff, float fovDiff) {
        float totalMovement = posDiff.length() + rotDiff.length() + fovDiff;
        
        if (totalMovement < 2.0f) {
            return EaseType.EASE_IN_OUT; // Small movements benefit from smooth in-out
        } else if (totalMovement < 10.0f) {
            return EaseType.EASE; // Medium movements use general ease
        } else {
            return EaseType.EASE_IN_OUT; // Large movements need careful in-out easing
        }
    }
    
    private static boolean shouldUseBezierPath(Vector3f posDiff, Vector3f rotDiff) {
        // Use Bezier paths for complex movements that benefit from custom curves
        float complexity = posDiff.length() + rotDiff.length();
        return complexity > 15.0f;
    }
    
    /**
     * Enum for different types of easing curves
     */
    public enum EaseType {
        EASE_IN,        // Slow start, fast end
        EASE_OUT,       // Fast start, slow end  
        EASE_IN_OUT,    // Slow start and end, fast middle
        EASE            // General smooth easing
    }
}