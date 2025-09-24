package net.tysontheember.apertureapi.common.animation;

import net.minecraft.network.chat.Component;

public enum PathInterpolator {
    LINEAR(0),
    SMOOTH(1),
    BEZIER(2),
    STEP(3),
    // New modes appended to preserve backward compatibility with saved indices
    COSINE(4),
    CATMULL_UNIFORM(5),
    CATMULL_CENTRIPETAL(6),
    CATMULL_CHORDAL(7);

    public final int index;

    PathInterpolator(int index) {
        this.index = index;
    }

    public Component getDisplayName() {
        return Component.translatable(getDisplayNameKey());
    }

    public String getDisplayNameKey() {
        return "camera_anim.motion_interpolator." + name().toLowerCase();
    }

    public static PathInterpolator fromIndex(int index) {
        return switch (index) {
            case 0 -> LINEAR;
            case 1 -> SMOOTH;
            case 2 -> BEZIER;
            case 3 -> STEP;
            case 4 -> COSINE;
            case 5 -> CATMULL_UNIFORM;
            case 6 -> CATMULL_CENTRIPETAL;
            case 7 -> CATMULL_CHORDAL;
            default -> LINEAR;
        };
    }
}

