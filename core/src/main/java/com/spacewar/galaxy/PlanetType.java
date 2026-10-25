package com.spacewar.galaxy;

public enum PlanetType {
    TERRAN("Terran", Sheet.MAJOR, 0, 20, 26, 2),
    VOLCANIC("Volcanic", Sheet.MAJOR, 1, 18, 24, 1),
    RINGED_GIANT("Ringed Gas Giant", Sheet.MAJOR, 2, 30, 36, 3),
    GAS_GIANT("Gas Giant", Sheet.MAJOR, 3, 30, 36, 3),
    ICE("Ice", Sheet.MAJOR, 4, 18, 24, 2),
    DESERT("Desert", Sheet.MAJOR, 5, 18, 24, 2),
    TOXIC("Toxic", Sheet.MAJOR, 6, 18, 24, 1),
    BARREN("Barren", Sheet.ROCKY, 0, 14, 18, 1),
    FROZEN("Frozen", Sheet.ROCKY, 1, 14, 18, 1),
    SULFUR("Sulfur", Sheet.ROCKY, 2, 14, 18, 0),
    CRYSTAL("Crystal", Sheet.ROCKY, 3, 14, 18, 0);

    public enum Sheet { MAJOR, ROCKY }

    public final String displayName;
    public final Sheet sheet;
    public final int spriteRow;
    public final float minSize, maxSize;
    public final int maxMoons;

    PlanetType(String displayName, Sheet sheet, int spriteRow, float minSize, float maxSize, int maxMoons) {
        this.displayName = displayName;
        this.sheet = sheet;
        this.spriteRow = spriteRow;
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.maxMoons = maxMoons;
    }
}
