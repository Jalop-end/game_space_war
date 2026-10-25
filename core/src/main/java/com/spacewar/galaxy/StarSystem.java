package com.spacewar.galaxy;

import java.util.ArrayList;
import java.util.List;

public class StarSystem {
    private static final String[] CLASS_NAMES = {"Giant", "Large", "Medium", "Dwarf"};

    public final String name;
    public final int sizeClass;
    public final float diameter;
    public final List<Planet> planets = new ArrayList<>();
    public float x, y;
    public float radius;

    // Frakční vlastnictví a domovský systém
    public Faction ownerFaction;

    public StarSystem(String name, int sizeClass, float diameter) {
        this.name = name;
        this.sizeClass = sizeClass;
        this.diameter = diameter;
    }

    public String className() { return CLASS_NAMES[sizeClass]; }

    public boolean isHomeworldSystem() {
        return ownerFaction != null;
    }

    public Planet getHomeworldPlanet() {
        for (Planet p : planets) {
            if (p.isHomeworld) return p;
        }
        return null;
    }
}
