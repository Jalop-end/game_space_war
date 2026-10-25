package com.spacewar.galaxy;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class NameGenerator {
    private static final String[] START = {"Al", "Be", "Ca", "Dra", "El", "Fo", "Ga", "He", "Ix", "Jo", "Ka", "Lu",
            "Me", "No", "Or", "Pra", "Qu", "Ri", "Sa", "Te", "Ul", "Ve", "Xa", "Ze", "Vor", "Thal"};
    private static final String[] MID = {"ra", "ri", "to", "ne", "la", "va", "mi", "ro", "the", "li", "da", "so"};
    private static final String[] END = {"x", "n", "s", "r", "th", "on", "ar", "is", "us", "ion", "a", "e"};

    private final Random random;
    private final Set<String> used = new HashSet<>();

    public NameGenerator(Random random) { this.random = random; }

    public String next() {
        String name;
        do {
            name = START[random.nextInt(START.length)]
                    + (random.nextFloat() < 0.6f ? MID[random.nextInt(MID.length)] : "")
                    + END[random.nextInt(END.length)];
        } while (!used.add(name));
        return name;
    }
}
