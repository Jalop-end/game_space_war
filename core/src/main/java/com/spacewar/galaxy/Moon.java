package com.spacewar.galaxy;

public class Moon {
    public final int spriteRow;
    public final float diameter;
    public final float orbitRadius;
    public final float angularSpeed;
    public final int seed;
    public float angle, x, y;

    public Moon(int spriteRow, float diameter, float orbitRadius, float angle, float angularSpeed) {
        this(spriteRow, diameter, orbitRadius, angle, angularSpeed,
                CloseUpPlanetGenerator.stringToSeed(spriteRow + ":" + orbitRadius + ":" + angle));
    }

    public Moon(int spriteRow, float diameter, float orbitRadius, float angle, float angularSpeed, int seed) {
        this.spriteRow = spriteRow;
        this.diameter = diameter;
        this.orbitRadius = orbitRadius;
        this.angle = angle;
        this.angularSpeed = angularSpeed;
        this.seed = (seed == 0) ? 7331 : seed;
    }

    public CloseUpPlanetGenerator.BodyType closeUpType() {
        return CloseUpPlanetGenerator.fromMoonRow(spriteRow);
    }
}
