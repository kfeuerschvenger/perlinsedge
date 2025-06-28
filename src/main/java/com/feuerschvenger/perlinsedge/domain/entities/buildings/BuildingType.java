package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import javafx.scene.paint.Color;

/**
 * Enum representing different types of buildings in the game.
 */
public enum BuildingType {
    WALL("A sturdy wall block, keeping out the baddies and making your fortress strong.", Color.GRAY),
    DOOR("A door that swings open to new adventures, or slams shut on pesky intruders.", Color.BROWN),
    BRIDGE("A bridge block, connecting distant lands and making travel a breeze.", Color.DARKGOLDENROD),
    DOCK("A dock for fishing, docking boats, or just hanging out by the water.", Color.DARKBLUE),
    FENCE("A fence to keep the critters out and your garden safe from nibblers.", Color.BROWN.brighter()),
    CHEST("A reliable chest, ideal for stashing treasures and hoarding snacks.", Color.BROWN.darker()),
    FURNACE("A blazing furnace, ready to melt down ore or cook a midnight snack.", Color.DARKRED),
    STANDING_TORCH("A beacon in the dark, lighting the way for weary adventurers.", Color.ORANGE),
    CAMPFIRE("A cozy campfire, perfect for warmth and swapping survival stories.", Color.RED),
    ANVIL("The trusty anvil for forging weapons, tools, and heroic dreams.", Color.DIMGRAY),
    WORKBENCH("A humble workbench, where ideas take shape and stuff gets crafted.", Color.PERU),
    BED("A soft bed that guarantees rest after a long day of exploration.", Color.LIGHTPINK);

    private final String displayName;
    private final String description;
    private final Color color;

    BuildingType(String description, Color color) {
        this.description = description;
        this.displayName = name().charAt(0) + name().substring(1).toLowerCase().replace('_', ' ');
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public Color getColor() {
        return color;
    }

}
