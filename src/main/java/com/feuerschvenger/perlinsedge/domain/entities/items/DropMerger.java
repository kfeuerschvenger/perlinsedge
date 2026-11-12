package com.feuerschvenger.perlinsedge.domain.entities.items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Merges items dropped by the player into the piles of the same type that the player dropped earlier and that still
 * lie nearby. Loot from resources and enemies is never merged into, so its source and timers stay as they were.
 * A pile is nearby when it is on the landing tile or on one of the eight tiles around it. Piles take items up to the
 * maximum stack size of the type, the closest pile first, so a long row of drops does not leave a scatter of small
 * stacks behind.
 */
public final class DropMerger {
    /** Largest distance in tiles, counted in whole tiles in every direction, at which two drops merge. */
    public static final int MERGE_RADIUS_TILES = 1;

    private DropMerger() {}

    /**
     * Adds as much of a drop as fits into the nearby piles of the same type.
     *
     * @param drops    the dropped items in the world
     * @param type     type of the dropped items
     * @param quantity how many units are dropped, positive
     * @param tileX    landing tile X
     * @param tileY    landing tile Y
     * @param gameTime current game time in seconds
     * @return the units that did not fit into any nearby pile, 0 when everything merged
     */
    public static int mergeInto(List<DroppedItem> drops, ItemType type, int quantity, int tileX, int tileY,
                                double gameTime) {
        List<DroppedItem> nearby = new ArrayList<>();
        for (DroppedItem pile : drops) {
            if (pile.getSource() == DroppedItem.DropSource.PLAYER && pile.getItemType() == type
                    && pile.roomLeft() > 0 && isNear(pile, tileX, tileY)) {
                nearby.add(pile);
            }
        }
        nearby.sort(Comparator.comparingInt(pile -> squaredDistance(pile, tileX, tileY)));

        int remaining = quantity;
        for (DroppedItem pile : nearby) {
            if (remaining <= 0) {
                break;
            }
            int moved = Math.min(remaining, pile.roomLeft());
            pile.absorb(moved, gameTime);
            remaining -= moved;
        }
        return remaining;
    }

    private static boolean isNear(DroppedItem pile, int tileX, int tileY) {
        return Math.abs(pile.getTileX() - tileX) <= MERGE_RADIUS_TILES
                && Math.abs(pile.getTileY() - tileY) <= MERGE_RADIUS_TILES;
    }

    private static int squaredDistance(DroppedItem pile, int tileX, int tileY) {
        int dx = pile.getTileX() - tileX;
        int dy = pile.getTileY() - tileY;
        return dx * dx + dy * dy;
    }
}
