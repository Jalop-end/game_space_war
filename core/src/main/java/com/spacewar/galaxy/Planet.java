package com.spacewar.galaxy;

import java.util.ArrayList;
import java.util.List;

public class Planet {
    public final String name;
    public final PlanetType type;
    public final float diameter;
    public final int seed;
    public final List<Moon> moons = new ArrayList<>();
    public float orbitRadius, angularSpeed, angle, x, y;

    // Frakcni a domovske atributy
    public boolean isHomeworld;
    public Faction homeworldFaction;
    public int population = 0;
    public int maxPopulation = 10;

    public Planet(String name, PlanetType type, float diameter) {
        this(name, type, diameter, CloseUpPlanetGenerator.stringToSeed(name + ":" + type.name()));
    }

    public Planet(String name, PlanetType type, float diameter, int seed) {
        this.name = name;
        this.type = type;
        this.diameter = diameter;
        this.seed = (seed == 0) ? 1337 : seed;
    }

    public CloseUpPlanetGenerator.BodyType closeUpType() {
        return CloseUpPlanetGenerator.fromPlanetType(type);
    }

    public void setAsHomeworld(Faction faction) {
        this.isHomeworld = true;
        this.homeworldFaction = faction;
        this.population = 10;
        this.maxPopulation = 10;
    }
}
