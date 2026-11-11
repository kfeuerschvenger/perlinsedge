package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import com.feuerschvenger.perlinsedge.domain.utils.Rgba;

/**
 * Defines all building types available in the game with their properties.
 * Each building type specifies:
 * - Display name (human-readable)
 * - Descriptive text explaining its purpose
 * - Base color for visual representation
 */
public enum BuildingType {
    // ==================================================================
    //  Structural Buildings
    // ==================================================================
    WALL(
            "A sturdy wall block, keeping out the baddies and making your fortress strong.",
            Rgba.GRAY
    ),
    DOOR(
            "A door that swings open to new adventures, or slams shut on pesky intruders.",
            Rgba.BROWN
    ),
    BRIDGE(
            "A bridge block, connecting distant lands and making travel a breeze.",
            Rgba.DARK_GOLDENROD
    ),
    DOCK(
            "A dock for fishing, docking boats, or just hanging out by the water.",
            Rgba.DARK_BLUE
    ),
    FENCE(
            "A fence to keep the critters out and your garden safe from nibblers.",
            Rgba.BROWN.brighter()
    ),

    // ==================================================================
    //  Functional Buildings
    // ==================================================================
    CHEST(
            "A reliable chest, ideal for stashing treasures and hoarding snacks.",
            Rgba.BROWN.darker()
    ),
    FURNACE(
            "A blazing furnace, ready to melt down ore or cook a midnight snack.",
            Rgba.DARK_RED
    ),
    ANVIL(
            "The trusty anvil for forging weapons, tools, and heroic dreams.",
            Rgba.DIM_GRAY
    ),
    WORKBENCH(
            "A humble workbench, where ideas take shape and stuff gets crafted.",
            Rgba.PERU
    ),

    // ==================================================================
    //  Light Sources
    // ==================================================================
    STANDING_TORCH(
            "A beacon in the dark, lighting the way for weary adventurers.",
            Rgba.ORANGE
    ),
    CAMPFIRE(
            "A cozy campfire, perfect for warmth and swapping survival stories.",
            Rgba.RED
    ),

    // ==================================================================
    //  Comfort Buildings
    // ==================================================================
    BED(
            "A soft bed that guarantees rest after a long day of exploration.",
            Rgba.LIGHT_PINK
    );

    // ==================================================================
    //  Instance Properties
    // ==================================================================
    private final String displayName;
    private final String description;
    private final Rgba baseColor;

    // ==================================================================
    //  Constructor
    // ==================================================================

    /**
     * Creates a new building type definition.
     *
     * @param description Flavor text explaining the building's purpose
     * @param baseColor   Primary color for visual representation
     */
    BuildingType(String description, Rgba baseColor) {
        this.description = description;
        this.baseColor = baseColor;
        this.displayName = generateDisplayName(name());
    }

    // ==================================================================
    //  Accessor Methods
    // ==================================================================

    /** @return Human-readable name for UI display */
    public String getDisplayName() {
        return displayName;
    }

    /** @return Descriptive text explaining the building's purpose */
    public String getDescription() {
        return description;
    }

    /** @return Base color for visual representation */
    public Rgba getBaseColor() {
        return baseColor;
    }

    // ==================================================================
    //  Helper Methods
    // ==================================================================

    /**
     * Generates a human-readable display name from enum constant.
     * Converts SNAKE_CASE to "Title Case".
     *
     * @param enumName Raw enum constant name
     * @return Formatted display name
     */
    private static String generateDisplayName(String enumName) {
        // Split by underscores and process each part
        String[] parts = enumName.split("_");
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) continue;

            // Capitalize first letter, lowercase the rest
            builder.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1).toLowerCase());

            if (i < parts.length - 1) {
                builder.append(" ");
            }
        }

        return builder.toString();
    }
}