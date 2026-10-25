package com.spacewar.galaxy;

import com.badlogic.gdx.graphics.Color;

public enum Faction {
    BLUE(
            "TERRANS",
            "Solar Federation",
            new Color(0.23f, 0.51f, 0.96f, 1f),
            new Color(0.65f, 0.82f, 1.0f, 1f),
            "MODRA RASA: Prumysl, silne stity a vyrovnane flotily.",
            0,
            0
    ),
    GREEN(
            "KRAK'TOR",
            "Bio-Chitin Hive",
            new Color(0.13f, 0.77f, 0.37f, 1f),
            new Color(0.60f, 0.95f, 0.70f, 1f),
            "ZELENA RASA: Rychly rust populace a organicka regenerace.",
            2,
            1
    ),
    RED(
            "VANDALS",
            "Void Crusade",
            new Color(0.94f, 0.27f, 0.27f, 1f),
            new Color(1.0f, 0.65f, 0.65f, 1f),
            "CERVENA RASA: Tezke railguny, agresivni pancerovani a zbrane.",
            3,
            2
    ),
    YELLOW(
            "SOLARIANS",
            "Solar Ascendancy",
            new Color(0.95f, 0.75f, 0.10f, 1f),
            new Color(1.0f, 0.90f, 0.45f, 1f),
            "ZLUTA RASA: Cista energie, laserove paprsky a rychle motory.",
            1,
            3
    );

    public final String raceName;
    public final String factionName;
    public final Color color;
    public final Color lightColor;
    public final String trait;
    public final int shipRow;
    public final int quadrant;

    Faction(String raceName, String factionName, Color color, Color lightColor, String trait, int shipRow, int quadrant) {
        this.raceName = raceName;
        this.factionName = factionName;
        this.color = color;
        this.lightColor = lightColor;
        this.trait = trait;
        this.shipRow = shipRow;
        this.quadrant = quadrant;
    }
}
