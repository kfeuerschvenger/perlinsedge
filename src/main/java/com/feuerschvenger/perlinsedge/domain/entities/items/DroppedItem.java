package com.feuerschvenger.perlinsedge.domain.entities.items;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents an item instance that has been dropped onto the game world.
 * Manages position, lifetime, and pickup mechanics for items on the ground.
 */
public class DroppedItem {
    // Configuration constants
    private static final AtomicLong ID_GENERATOR = new AtomicLong();
    private static final double PICKUP_RADIUS = 0.8; // Tile-based pickup radius
    private static final double PLAYER_DROP_PICKUP_DELAY_SECONDS = 2.0; // Gives the player time to walk away from a dropped stack

    /**
     * Identifies the original source of the dropped item.
     */
    public enum DropSource {
        PLAYER,      // Item dropped by the player
        RESOURCE,    // Item from harvested resources
        ENEMY,       // Item from defeated enemies
        OTHER        // Other sources
    }

    // Instance fields
    private final long id;
    private final Item item;
    private final int tileX;
    private final int tileY;
    private final double dropTime;
    private final DropSource source;

    /**
     * Creates a new dropped item instance.
     *
     * @param tileX     X-coordinate of drop location (tile position)
     * @param tileY     Y-coordinate of drop location (tile position)
     * @param item      The item being dropped (non-null, positive quantity)
     * @param dropTime  Game time when item was dropped (in seconds)
     * @param source    Origin of the dropped item
     * @throws IllegalArgumentException if item is null or has invalid quantity
     */
    public DroppedItem(int tileX, int tileY, Item item, double dropTime, DropSource source) {
        if (item == null) {
            throw new IllegalArgumentException("Dropped item cannot be null");
        }
        if (item.getQuantity() <= 0) {
            throw new IllegalArgumentException("Dropped item quantity must be positive");
        }

        this.id = ID_GENERATOR.incrementAndGet();
        this.tileX = tileX;
        this.tileY = tileY;
        this.item = item;
        this.dropTime = dropTime;
        this.source = source;
    }

    // ==================================================================
    //  Accessor Methods
    // ==================================================================

    /** @return Unique identifier for this dropped item instance */
    public long getId() {
        return id;
    }

    /** @return The underlying item being dropped */
    public Item getItem() {
        return item;
    }

    /** @return X-coordinate of drop location (tile position) */
    public int getTileX() {
        return tileX;
    }

    /** @return Y-coordinate of drop location (tile position) */
    public int getTileY() {
        return tileY;
    }

    /** @return Game time when item was dropped (in seconds) */
    public double getDropTime() {
        return dropTime;
    }

    /** @return Source/origin of the dropped item */
    public DropSource getSource() {
        return source;
    }

    /** @return Current quantity of the dropped item */
    public int getQuantity() {
        return item.getQuantity();
    }

    /** @return Type of the item being dropped */
    public ItemType getItemType() {
        return item.getType();
    }

    // ==================================================================
    //  Mutator Methods
    // ==================================================================

    /**
     * Updates the quantity of the dropped item.
     *
     * @param quantity New quantity value (must be positive)
     * @throws IllegalArgumentException if quantity is not positive
     */
    public void setQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        item.setQuantity(quantity);
    }

    // ==================================================================
    //  Gameplay Logic Methods
    // ==================================================================

    /**
     * Determines if a player is within pickup range of this item.
     *
     * @param playerTileX Player's current X position (tile coordinates)
     * @param playerTileY Player's current Y position (tile coordinates)
     * @return True if player is within pickup radius
     */
    public boolean isInPickupRange(int playerTileX, int playerTileY) {
        double dx = playerTileX - tileX;
        double dy = playerTileY - tileY;
        return Math.hypot(dx, dy) <= PICKUP_RADIUS;
    }

    /**
     * Determines if this item can be picked up at a given moment.
     * An item dropped by the player has its own two second delay, so the player does not collect the stack again while still standing next to it; the other sources are available at once. The delay is per item, it never holds up other drops.
     *
     * @param playerTileX Player's current X position
     * @param playerTileY Player's current Y position
     * @param gameTime    Current game time in seconds
     * @return True if the player is in range and the item's own delay has passed
     */
    public boolean canBePickedUp(int playerTileX, int playerTileY, double gameTime) {
        return gameTime - dropTime >= pickupDelaySeconds() && isInPickupRange(playerTileX, playerTileY);
    }

    /** @return Seconds after the drop during which this item cannot be picked up */
    public double pickupDelaySeconds() {
        return source == DropSource.PLAYER ? PLAYER_DROP_PICKUP_DELAY_SECONDS : 0.0;
    }

    // ==================================================================
    //  Object Overrides
    // ==================================================================

    /**
     * Compares dropped items by unique identifier.
     *
     * @param obj Object to compare
     * @return True if objects represent the same dropped item instance
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DroppedItem other = (DroppedItem) obj;
        return id == other.id;
    }

    /** @return Hash code based on unique identifier */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}