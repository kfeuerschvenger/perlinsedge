package com.feuerschvenger.perlinsedge.domain.entities.items;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents an item that has been dropped onto the game map.
 * It has a position in the world and can be picked up by the player.
 */
public class DroppedItem {
    private static final AtomicLong ID_COUNTER = new AtomicLong(); // Contador para IDs únicos

    private static final double PICKUP_RADIUS = 0.8; // Default pickup radius in tiles

    public enum DropSource {
        PLAYER, RESOURCE, ENEMY, OTHER
    }

    private final long id;
    private final Item item;
    private final int tileX;
    private final int tileY;
    private final double dropTime;

    // Flag to track if the player has moved away from the item
    private boolean playerHasLeftProximity = false;
    // Source of the drop, e.g., PLAYER, RESOURCE, ENEMY, OTHER
    private final DropSource source;

    /**
     * Creates a new dropped item at the specified tile coordinates.
     * @param tileX X coordinate of the tile where the item was dropped.
     * @param tileY Y coordinate of the tile where the item was dropped.
     * @param item The item that was dropped.
     * @param dropTime The time when the item was dropped.
     */
    public DroppedItem(int tileX, int tileY, Item item, double dropTime, DropSource source) {
        if (item == null) {
            throw new IllegalArgumentException("Dropped item cannot be null.");
        }
        if (item.getQuantity() <= 0) {
            throw new IllegalArgumentException("Dropped item quantity must be positive.");
        }
        this.id = ID_COUNTER.incrementAndGet();
        this.tileX = tileX;
        this.tileY = tileY;
        this.item = item;
        this.dropTime = dropTime;
        this.source = source;
    }

    public long getId() {
        return id;
    }

    public ItemType getItemType() {
        return item.getType();
    }

    public int getQuantity() {
        return item.getQuantity();
    }

    public void setQuantity(int quantity) {
        this.item.setQuantity(quantity);
    }

    public void addQuantity(int amount) {
        this.item.setQuantity(this.item.getQuantity() + amount);
    }

    public Item getItem() {
        return item;
    }

    public int getTileX() { return tileX; }

    public int getTileY() {
        return tileY;
    }

    public double getDropTime() {
        return dropTime;
    }

    /**
     * Checks if a player is close enough to pick up this item.
     * @param playerTileX Player's current tile X.
     * @param playerTileY Player's current tile Y.
     * @return true if the player is within pickup range.
     */
    public boolean isInPickupRange(int playerTileX, int playerTileY) {
        double dx = playerTileX - this.tileX;
        double dy = playerTileY - this.tileY;
        return Math.sqrt(dx * dx + dy * dy) <= PICKUP_RADIUS;
    }

    public boolean canBePickedUp(int playerTileX, int playerTileY) {
        // Resource drops can always be picked up inmediately
        if (source == DropSource.RESOURCE || source == DropSource.ENEMY) {
            return isInPickupRange(playerTileX, playerTileY);
        }

        if (!playerHasLeftProximity) {
            double dx = playerTileX - this.tileX;
            double dy = playerTileY - this.tileY;
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance > 3.0) {
                playerHasLeftProximity = true;
            }
            return false;
        }
        return isInPickupRange(playerTileX, playerTileY);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DroppedItem that = (DroppedItem) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}