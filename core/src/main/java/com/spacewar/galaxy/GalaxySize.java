package com.spacewar.galaxy;

public enum GalaxySize {
    SMALL("Small", 25, 2100f, 2),
    MEDIUM("Medium", 45, 2800f, 3),
    LARGE("Large", 75, 3600f, 4);

    public final String displayName;
    public final int starCount;
    public final float radius;
    public final int arms;

    GalaxySize(String displayName, int starCount, float radius, int arms) {
        this.displayName = displayName;
        this.starCount = starCount;
        this.radius = radius;
        this.arms = arms;
    }
}
