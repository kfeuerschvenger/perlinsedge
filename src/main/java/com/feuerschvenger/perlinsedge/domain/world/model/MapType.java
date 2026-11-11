package com.feuerschvenger.perlinsedge.domain.world.model;

/**
 * Enum representing different types of maps in the game.
 * Each type corresponds to a specific biome or environment.
 */
public enum MapType {
    DENSE_FOREST("Dense Forest"),
    SWAMP("Swamp"),
    ISLANDS("Islands"),
    DESERT("Desert"),
    TUNDRA("Tundra"),
    RIVERS("Rivers"),
    MEDITERRANEAN("Mediterranean"),
    MOUNTAINOUS("Mountainous");

    private final String displayName;

    /** Creates a map type with the name shown to the player. */
    MapType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the name shown to the player.
     *
     * @return the title case name, for example Dense Forest
     */
    public String getDisplayName() {
        return displayName;
    }
}
