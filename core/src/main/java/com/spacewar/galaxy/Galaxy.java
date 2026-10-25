package com.spacewar.galaxy;

import com.badlogic.gdx.math.MathUtils;

import java.util.ArrayList;
import java.util.List;

public class Galaxy {
    public final GalaxySize size;
    public final long seed;
    public final int blackHoleVariant;
    public final Faction playerFaction;
    public final List<StarSystem> stars = new ArrayList<>();

    public Galaxy(GalaxySize size, long seed, int blackHoleVariant, Faction playerFaction) {
        this.size = size;
        this.seed = seed;
        this.blackHoleVariant = blackHoleVariant;
        this.playerFaction = playerFaction;
    }

    public StarSystem getHomeworldSystem(Faction faction) {
        for (StarSystem s : stars) {
            if (s.ownerFaction == faction) return s;
        }
        return null;
    }

    public void update(float delta) {
        for (StarSystem s : stars) {
            for (Planet p : s.planets) {
                p.angle = (p.angle + p.angularSpeed * delta) % MathUtils.PI2;
                p.x = s.x + MathUtils.cos(p.angle) * p.orbitRadius;
                p.y = s.y + MathUtils.sin(p.angle) * p.orbitRadius;
                for (Moon m : p.moons) {
                    m.angle = (m.angle + m.angularSpeed * delta) % MathUtils.PI2;
                    m.x = p.x + MathUtils.cos(m.angle) * m.orbitRadius;
                    m.y = p.y + MathUtils.sin(m.angle) * m.orbitRadius;
                }
            }
        }
    }
}
