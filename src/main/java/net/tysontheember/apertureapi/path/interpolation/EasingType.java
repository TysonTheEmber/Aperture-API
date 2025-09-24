package net.tysontheember.apertureapi.path.interpolation;

/**
 * Comprehensive easing types for smooth camera transitions.
 * Based on standard easing functions used in animation.
 */
public enum EasingType {
    // Linear
    LINEAR("linear"),
    
    // Quadratic
    QUAD_IN("quadIn"),
    QUAD_OUT("quadOut"), 
    QUAD_IN_OUT("quadInOut"),
    
    // Cubic (most commonly used for camera work)
    CUBIC_IN("cubicIn"),
    CUBIC_OUT("cubicOut"),
    CUBIC_IN_OUT("cubicInOut"),
    
    // Quartic
    QUART_IN("quartIn"),
    QUART_OUT("quartOut"),
    QUART_IN_OUT("quartInOut"),
    
    // Quintic
    QUINT_IN("quintIn"),
    QUINT_OUT("quintOut"),
    QUINT_IN_OUT("quintInOut"),
    
    // Sinusoidal
    SINE_IN("sineIn"),
    SINE_OUT("sineOut"),
    SINE_IN_OUT("sineInOut"),
    
    // Exponential
    EXPO_IN("expoIn"),
    EXPO_OUT("expoOut"),
    EXPO_IN_OUT("expoInOut"),
    
    // Circular
    CIRC_IN("circIn"),
    CIRC_OUT("circOut"),
    CIRC_IN_OUT("circInOut"),
    
    // Back (overshoot)
    BACK_IN("backIn"),
    BACK_OUT("backOut"),
    BACK_IN_OUT("backInOut"),
    
    // Elastic (spring-like)
    ELASTIC_IN("elasticIn"),
    ELASTIC_OUT("elasticOut"),
    ELASTIC_IN_OUT("elasticInOut"),
    
    // Bounce
    BOUNCE_IN("bounceIn"),
    BOUNCE_OUT("bounceOut"),
    BOUNCE_IN_OUT("bounceInOut");

    private final String name;
    
    EasingType(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    /**
     * Apply the easing function to a normalized time value [0,1]
     */
    public float apply(float t) {
        // Clamp input
        t = Math.max(0f, Math.min(1f, t));
        
        return switch (this) {
            case LINEAR -> t;
            
            // Quadratic
            case QUAD_IN -> t * t;
            case QUAD_OUT -> 1f - (1f - t) * (1f - t);
            case QUAD_IN_OUT -> t < 0.5f ? 2f * t * t : 1f - 2f * (1f - t) * (1f - t);
            
            // Cubic
            case CUBIC_IN -> t * t * t;
            case CUBIC_OUT -> 1f - (float) Math.pow(1f - t, 3);
            case CUBIC_IN_OUT -> t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
            
            // Quartic
            case QUART_IN -> t * t * t * t;
            case QUART_OUT -> 1f - (float) Math.pow(1f - t, 4);
            case QUART_IN_OUT -> t < 0.5f ? 8f * t * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 4) / 2f;
            
            // Quintic
            case QUINT_IN -> t * t * t * t * t;
            case QUINT_OUT -> 1f - (float) Math.pow(1f - t, 5);
            case QUINT_IN_OUT -> t < 0.5f ? 16f * t * t * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 5) / 2f;
            
            // Sinusoidal
            case SINE_IN -> 1f - (float) Math.cos((t * Math.PI) / 2f);
            case SINE_OUT -> (float) Math.sin((t * Math.PI) / 2f);
            case SINE_IN_OUT -> -(float)(Math.cos(Math.PI * t) - 1f) / 2f;
            
            // Exponential
            case EXPO_IN -> t == 0f ? 0f : (float) Math.pow(2f, 10f * (t - 1f));
            case EXPO_OUT -> t == 1f ? 1f : 1f - (float) Math.pow(2f, -10f * t);
            case EXPO_IN_OUT -> {
                if (t == 0f) yield 0f;
                if (t == 1f) yield 1f;
                if (t < 0.5f) yield (float) Math.pow(2f, 20f * t - 10f) / 2f;
                yield (2f - (float) Math.pow(2f, -20f * t + 10f)) / 2f;
            }
            
            // Circular
            case CIRC_IN -> 1f - (float) Math.sqrt(1f - t * t);
            case CIRC_OUT -> (float) Math.sqrt(1f - (t - 1f) * (t - 1f));
            case CIRC_IN_OUT -> t < 0.5f ? 
                (1f - (float) Math.sqrt(1f - 4f * t * t)) / 2f :
                ((float) Math.sqrt(1f - (-2f * t + 2f) * (-2f * t + 2f)) + 1f) / 2f;
            
            // Back (c1 = 1.70158, c3 = c1 + 1)
            case BACK_IN -> {
                float c1 = 1.70158f;
                float c3 = c1 + 1f;
                yield c3 * t * t * t - c1 * t * t;
            }
            case BACK_OUT -> {
                float c1 = 1.70158f;
                float c3 = c1 + 1f;
                yield 1f + c3 * (float) Math.pow(t - 1f, 3) + c1 * (float) Math.pow(t - 1f, 2);
            }
            case BACK_IN_OUT -> {
                float c1 = 1.70158f;
                float c2 = c1 * 1.525f;
                if (t < 0.5f) {
                    yield ((float) Math.pow(2f * t, 2) * ((c2 + 1f) * 2f * t - c2)) / 2f;
                } else {
                    yield ((float) Math.pow(2f * t - 2f, 2) * ((c2 + 1f) * (t * 2f - 2f) + c2) + 2f) / 2f;
                }
            }
            
            // Elastic
            case ELASTIC_IN -> {
                if (t == 0f) yield 0f;
                if (t == 1f) yield 1f;
                float c4 = (float)(2f * Math.PI / 3f);
                yield -(float)(Math.pow(2f, 10f * (t - 1f)) * Math.sin((t - 1f) * c4));
            }
            case ELASTIC_OUT -> {
                if (t == 0f) yield 0f;
                if (t == 1f) yield 1f;
                float c4 = (float)(2f * Math.PI / 3f);
                yield (float)(Math.pow(2f, -10f * t) * Math.sin(t * c4)) + 1f;
            }
            case ELASTIC_IN_OUT -> {
                if (t == 0f) yield 0f;
                if (t == 1f) yield 1f;
                float c5 = (float)(2f * Math.PI / 4.5f);
                if (t < 0.5f) {
                    yield -(float)(Math.pow(2f, 20f * t - 10f) * Math.sin((20f * t - 11.125f) * c5)) / 2f;
                } else {
                    yield (float)(Math.pow(2f, -20f * t + 10f) * Math.sin((20f * t - 11.125f) * c5)) / 2f + 1f;
                }
            }
            
            // Bounce
            case BOUNCE_IN -> 1f - bounceOut(1f - t);
            case BOUNCE_OUT -> bounceOut(t);
            case BOUNCE_IN_OUT -> t < 0.5f ? (1f - bounceOut(1f - 2f * t)) / 2f : (1f + bounceOut(2f * t - 1f)) / 2f;
        };
    }
    
    private static float bounceOut(float t) {
        float n1 = 7.5625f;
        float d1 = 2.75f;
        
        if (t < 1f / d1) {
            return n1 * t * t;
        } else if (t < 2f / d1) {
            t -= 1.5f / d1;
            return n1 * t * t + 0.75f;
        } else if (t < 2.5f / d1) {
            t -= 2.25f / d1;
            return n1 * t * t + 0.9375f;
        } else {
            t -= 2.625f / d1;
            return n1 * t * t + 0.984375f;
        }
    }
    
    /**
     * Parse from string (case insensitive)
     */
    public static EasingType fromString(String str) {
        if (str == null) return CUBIC_IN_OUT;
        
        String lower = str.toLowerCase().trim();
        for (EasingType type : values()) {
            if (type.name.toLowerCase().equals(lower)) {
                return type;
            }
        }
        
        // Handle common aliases
        return switch (lower) {
            case "ease" -> CUBIC_IN_OUT;
            case "easein" -> CUBIC_IN;
            case "easeout" -> CUBIC_OUT;
            case "easeinout" -> CUBIC_IN_OUT;
            case "quad" -> QUAD_IN_OUT;
            case "cubic" -> CUBIC_IN_OUT;
            case "sine" -> SINE_IN_OUT;
            default -> CUBIC_IN_OUT;
        };
    }
    
    @Override
    public String toString() {
        return name;
    }
}