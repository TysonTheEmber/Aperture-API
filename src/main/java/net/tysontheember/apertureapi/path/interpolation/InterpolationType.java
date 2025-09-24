package net.tysontheember.apertureapi.path.interpolation;

/**
 * Interpolation types for CMDCam parity, including centripetal Catmull-Rom
 * as the default for smooth, stable interpolation.
 */
public enum InterpolationType {
    // Linear - straight lines between points
    LINEAR("linear"),
    
    // Cosine - smooth transitions using cosine interpolation
    COSINE("cosine"),
    
    // Hermite - cubic interpolation with tension/continuity/bias controls
    HERMITE("hermite"),
    
    // Cubic Bezier - full control via handles
    BEZIER("bezier"),
    
    // Catmull-Rom variants (the gold standard for smooth camera paths)
    CATMULL_UNIFORM("catmull:uniform"),         // α = 0.0
    CATMULL_CENTRIPETAL("catmull:centripetal"), // α = 0.5 (DEFAULT - prevents loops and cusps)
    CATMULL_CHORDAL("catmull:chordal");         // α = 1.0

    private final String name;
    
    InterpolationType(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    /**
     * Get alpha parameter for Catmull-Rom variants
     */
    public float getCatmullAlpha() {
        return switch (this) {
            case CATMULL_UNIFORM -> 0.0f;
            case CATMULL_CENTRIPETAL -> 0.5f;
            case CATMULL_CHORDAL -> 1.0f;
            default -> 0.5f; // Default to centripetal
        };
    }
    
    /**
     * Check if this is a Catmull-Rom variant
     */
    public boolean isCatmullRom() {
        return this == CATMULL_UNIFORM || this == CATMULL_CENTRIPETAL || this == CATMULL_CHORDAL;
    }
    
    /**
     * Parse from string (case insensitive)
     */
    public static InterpolationType fromString(String str) {
        if (str == null) return CATMULL_CENTRIPETAL;
        
        String lower = str.toLowerCase().trim();
        for (InterpolationType type : values()) {
            if (type.name.equals(lower)) {
                return type;
            }
        }
        
        // Handle legacy names
        return switch (lower) {
            case "smooth" -> CATMULL_CENTRIPETAL;
            case "step" -> LINEAR; // Step becomes linear for now
            case "catmull", "catmullrom" -> CATMULL_CENTRIPETAL;
            default -> CATMULL_CENTRIPETAL;
        };
    }
    
    @Override
    public String toString() {
        return name;
    }
}