package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import com.feuerschvenger.perlinsedge.domain.crafting.CraftingStation;
import com.feuerschvenger.perlinsedge.domain.utils.Rgba;

import java.util.Locale;
import java.util.Optional;

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
    BRIDGE(
            "A bridge block, connecting distant lands and making travel a breeze.",
            Rgba.DARK_GOLDENROD
    ),
    FENCE_GATE(
            "A gate in your fence: click it to swing it open for yourself or shut against the critters.",
            Rgba.BROWN
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
    ),
    SLEEPING_BAG(
            "A straw sleeping bag. Not much, but it beats waking up in the wild.",
            Rgba.PERU.brighter()
    );

    /** Rotation count of a piece whose front turns through the four edges of its tile. */
    private static final int FOUR_WAY_ROTATIONS = 4;
    /** Rotation count of a piece that runs along one of the two diagonals of its tile. */
    private static final int TWO_WAY_ROTATIONS = 2;

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
    //  Rotation
    // ==================================================================

    /**
     * Gives how many ways a piece of this type can face. A four way piece has a front that turns through the four edges
     * of its tile, a two way piece runs along one of the two diagonals of its tile, and the others look the same
     * whichever way they are placed.
     * <p>
     * A four way piece has rotation 0 when its front faces the lower right edge of the tile on the screen, 1 for the
     * lower left, 2 for the upper left and 3 for the upper right, so one turn of the rotation key is a quarter turn
     * clockwise seen from above. A two way piece has rotation 0 when it runs between the upper left and the lower right
     * edge, which is along the tile X axis, and 1 when it runs between the upper right and the lower left edge, which
     * is along the tile Y axis.
     *
     * @return 4, 2 or 1
     */
    public int rotationCount() {
        return switch (this) {
            case CHEST, FURNACE, ANVIL, WORKBENCH, BED, SLEEPING_BAG -> FOUR_WAY_ROTATIONS;
            case BRIDGE, FENCE_GATE -> TWO_WAY_ROTATIONS;
            default -> 1;
        };
    }

    /**
     * Tells whether a piece of this type is a floor, which lies in its own layer of the tile under the other pieces. A
     * bridge is a floor, it makes water walkable, and of the other pieces only the standing torch can be put on one.
     *
     * @return true for the bridge
     */
    public boolean isFloor() {
        return this == BRIDGE;
    }

    /** @return true when the rotation key changes how a piece of this type faces */
    public boolean isRotatable() {
        return rotationCount() > 1;
    }

    // ==================================================================
    //  Crafting
    // ==================================================================

    /**
     * Gives the place of crafting a piece of this type opens when it is clicked.
     *
     * @return the workbench or the anvil station, empty for a piece that is no crafting station
     */
    public Optional<CraftingStation> craftingStation() {
        return switch (this) {
            case WORKBENCH -> Optional.of(CraftingStation.WORKBENCH);
            case ANVIL -> Optional.of(CraftingStation.ANVIL);
            default -> Optional.empty();
        };
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
        var builder = new StringBuilder();

        for (var i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) {
                continue;
            }

            // Capitalize first letter, lowercase the rest
            builder.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1).toLowerCase(Locale.ROOT));

            if (i < parts.length - 1) {
                builder.append(" ");
            }
        }

        return builder.toString();
    }
}
