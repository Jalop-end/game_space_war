package com.spacewar.galaxy;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class GalaxyGenerator {
    private static final float BLACK_HOLE_CLEARANCE = 520f;
    private static final float ARM_TWIST = 3.4f;
    private static final float[] STAR_DIAMETERS = {96f, 76f, 60f, 46f};
    private static final String[] ROMAN = {"I", "II", "III", "IV", "V"};

    // 7 hlavnich typu planet (odpovida 7 planetam v CloseUpPlanetGenerator a planets_major.png)
    private static final PlanetType[] MAJOR_PLANET_TYPES = {
        PlanetType.TERRAN,
        PlanetType.VOLCANIC,
        PlanetType.RINGED_GIANT,
        PlanetType.GAS_GIANT,
        PlanetType.ICE,
        PlanetType.DESERT,
        PlanetType.TOXIC
    };

    private GalaxyGenerator() {}

    public static Galaxy generate(GalaxySize size, long seed, Faction playerFaction) {
        Random rnd = new Random(seed);
        NameGenerator names = new NameGenerator(rnd);
        Galaxy galaxy = new Galaxy(size, seed, rnd.nextInt(2), playerFaction);

        float spacing = 1f;
        int failures = 0;
        StarSystem pending = createSystem(rnd, names);
        while (galaxy.stars.size() < size.starCount) {
            Vector2 p = randomPosition(rnd, size);
            if (fits(galaxy, pending, p.x, p.y, spacing)) {
                pending.x = p.x;
                pending.y = p.y;
                galaxy.stars.add(pending);
                pending = createSystem(rnd, names);
                failures = 0;
            } else if (++failures > 400) {
                spacing = Math.max(0.5f, spacing * 0.93f);
                failures = 0;
            }
        }

        // Prirazeni 4 frakci do 4 kvadrantu galaxie
        assignFactionHomeworlds(galaxy, rnd);

        galaxy.update(0f);
        return galaxy;
    }

    private static void assignFactionHomeworlds(Galaxy galaxy, Random rnd) {
        Faction[] factions = Faction.values(); // BLUE, GREEN, RED, YELLOW
        float minRadius = galaxy.size.radius * 0.40f;
        float maxRadius = galaxy.size.radius * 0.88f;

        for (int q = 0; q < 4; q++) {
            Faction faction = factions[q];
            List<StarSystem> candidates = new ArrayList<>();
            List<StarSystem> quadrantFallback = new ArrayList<>();

            for (StarSystem s : galaxy.stars) {
                // Urceni kvadrantu (0: +,+, 1: -,+, 2: -,-, 3: +,-)
                int starQuad;
                if (s.x >= 0 && s.y >= 0) starQuad = 0;
                else if (s.x < 0 && s.y >= 0) starQuad = 1;
                else if (s.x < 0 && s.y < 0) starQuad = 2;
                else starQuad = 3;

                if (starQuad == q) {
                    quadrantFallback.add(s);
                    float dist = (float) Math.sqrt(s.x * s.x + s.y * s.y);
                    if (dist >= minRadius && dist <= maxRadius) {
                        candidates.add(s);
                    }
                }
            }

            // Vyber soustavy v kvadrantu
            StarSystem chosen = null;
            if (!candidates.isEmpty()) {
                chosen = candidates.get(rnd.nextInt(candidates.size()));
            } else if (!quadrantFallback.isEmpty()) {
                quadrantFallback.sort((a, b) -> Float.compare(b.x * b.x + b.y * b.y, a.x * a.x + a.y * a.y));
                chosen = quadrantFallback.get(0);
            } else if (!galaxy.stars.isEmpty()) {
                chosen = galaxy.stars.get(rnd.nextInt(galaxy.stars.size()));
            }

            if (chosen != null) {
                chosen.ownerFaction = faction;
                setupHomeworldPlanet(chosen, faction, rnd);
            }
        }
    }

    private static void setupHomeworldPlanet(StarSystem system, Faction faction, Random rnd) {
        if (system.planets.isEmpty()) {
            Planet p = new Planet(system.name + " Prime", PlanetType.TERRAN, 26f, rnd.nextInt());
            p.orbitRadius = system.diameter / 2f + 45f;
            p.angle = rnd.nextFloat() * MathUtils.PI2;
            p.angularSpeed = 6f / p.orbitRadius;
            p.setAsHomeworld(faction);
            system.planets.add(p);
            system.radius = Math.max(system.radius, p.orbitRadius + 20f);
            return;
        }

        // Najdi nejvhodnejsi planetu (preferuj TERRAN, nebo vezmi obyvatelnou planetu)
        Planet chosenPlanet = null;
        for (Planet p : system.planets) {
            if (p.type == PlanetType.TERRAN) {
                chosenPlanet = p;
                break;
            }
        }

        if (chosenPlanet == null) {
            for (Planet p : system.planets) {
                if (p.type != PlanetType.GAS_GIANT && p.type != PlanetType.RINGED_GIANT) {
                    chosenPlanet = p;
                    break;
                }
            }
            if (chosenPlanet == null) {
                chosenPlanet = system.planets.get(rnd.nextInt(system.planets.size()));
            }
        }

        chosenPlanet.setAsHomeworld(faction);
    }

    private static Vector2 randomPosition(Random rnd, GalaxySize size) {
        float t = (float) Math.sqrt(rnd.nextFloat());
        float r = BLACK_HOLE_CLEARANCE + t * (size.radius - BLACK_HOLE_CLEARANCE);
        float angle;
        if (rnd.nextFloat() < 0.15f) {
            angle = rnd.nextFloat() * MathUtils.PI2;
        } else {
            int arm = rnd.nextInt(size.arms);
            angle = arm * MathUtils.PI2 / size.arms
                    + (r / size.radius) * ARM_TWIST
                    + (float) rnd.nextGaussian() * 0.25f;
        }
        return new Vector2(MathUtils.cos(angle) * r, MathUtils.sin(angle) * r);
    }

    private static boolean fits(Galaxy galaxy, StarSystem s, float x, float y, float spacing) {
        for (StarSystem o : galaxy.stars) {
            float min = (s.radius + o.radius) * spacing + 40f;
            if (Vector2.dst2(x, y, o.x, o.y) < min * min) return false;
        }
        return true;
    }

    private static StarSystem createSystem(Random rnd, NameGenerator names) {
        float roll = rnd.nextFloat();
        int sizeClass = roll < 0.10f ? 0 : roll < 0.40f ? 1 : roll < 0.75f ? 2 : 3;
        StarSystem s = new StarSystem(names.next(), sizeClass, STAR_DIAMETERS[sizeClass]);

        int planetCount = 2 + rnd.nextInt(4); // 2 az 5 planet v soustave
        float orbit = s.diameter / 2f + 26f;
        float prevReach = 0f;

        for (int i = 0; i < planetCount; i++) {
            PlanetType type = MAJOR_PLANET_TYPES[rnd.nextInt(MAJOR_PLANET_TYPES.length)];
            float diameter = MathUtils.lerp(type.minSize, type.maxSize, rnd.nextFloat());
            int planetSeed = rnd.nextInt();
            Planet planet = new Planet(s.name + " " + ROMAN[i], type, diameter, planetSeed);

            int moonCount = type.maxMoons == 0 ? 0 : rnd.nextInt(type.maxMoons + 1);
            float moonOrbit = diameter / 2f + 7f;
            for (int m = 0; m < moonCount; m++) {
                int moonRow = rnd.nextInt(4);
                int moonSeed = rnd.nextInt();
                planet.moons.add(new Moon(moonRow, 5f + rnd.nextFloat() * 2f, moonOrbit,
                        rnd.nextFloat() * MathUtils.PI2, 0.5f + rnd.nextFloat() * 0.7f, moonSeed));
                moonOrbit += 7f;
            }
            float reach = moonCount > 0 ? moonOrbit : diameter / 2f;

            orbit += (i == 0) ? reach : prevReach + reach + 14f + rnd.nextFloat() * 10f;
            planet.orbitRadius = orbit;
            planet.angle = rnd.nextFloat() * MathUtils.PI2;
            planet.angularSpeed = 6f / orbit;
            s.planets.add(planet);
            prevReach = reach;
        }
        s.radius = orbit + prevReach;
        return s;
    }
}
