package com.feuerschvenger.perlinsedge.domain.entities.items;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents an item instance that has been dropped onto the game world.
 * Manages position, lifetime, and pickup mechanics for items on the ground.
 * All times are game clock seconds, which stands still while the game is paused, so neither the pickup delay nor the
 * despawn timer runs under the pause menu.
 */
public class DroppedItem {
    // Configuration constants
    private static final AtomicLong ID_GENERATOR = new AtomicLong();
    private static final double PICKUP_RADIUS = 0.8; // Tile-based pickup radius
    private static final double PLAYER_DROP_PICKUP_DELAY_SECONDS = 2.0; // Gives the player time to walk away from a dropped stack
    /** Seconds a dropped item stays in the world before it despawns: five minutes of game time. */
    public static final double LIFETIME_SECONDS = 300.0;
    /** Seconds a thrown item takes to fly from the thrower to the tile it lands on. */
    public static final double THROW_SECONDS = 0.4;

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
    private final int originX;
    private final int originY;
    /** Game time of the throw: the flight is measured from it. */
    private final double dropTime;
    /** Game time the pickup delay and the lifetime are measured from: the drop, or the last merge into the pile. */
    private double timerStart;
    private final DropSource source;

    /**
     * Creates a new dropped item instance that appears on its tile without a flight.
     *
     * @param tileX     X-coordinate of drop location (tile position)
     * @param tileY     Y-coordinate of drop location (tile position)
     * @param item      The item being dropped (non-null, positive quantity)
     * @param dropTime  Game time when item was dropped (in seconds)
     * @param source    Origin of the dropped item
     * @throws IllegalArgumentException if item is null or has invalid quantity
     */
    public DroppedItem(int tileX, int tileY, Item item, double dropTime, DropSource source) {
        this(tileX, tileY, tileX, tileY, item, dropTime, source);
    }

    /**
     * Creates a new dropped item instance that flies from one tile to another.
     *
     * @param originX   X-coordinate the item is thrown from (tile position)
     * @param originY   Y-coordinate the item is thrown from (tile position)
     * @param tileX     X-coordinate of the landing tile
     * @param tileY     Y-coordinate of the landing tile
     * @param item      The item being dropped (non-null, positive quantity)
     * @param dropTime  Game time when item was thrown (in seconds)
     * @param source    Origin of the dropped item
     * @throws IllegalArgumentException if item is null or has invalid quantity
     */
    public DroppedItem(int originX, int originY, int tileX, int tileY, Item item, double dropTime, DropSource source) {
        if (item == null) {
            throw new IllegalArgumentException("Dropped item cannot be null");
        }
        if (item.getQuantity() <= 0) {
            throw new IllegalArgumentException("Dropped item quantity must be positive");
        }

        this.id = ID_GENERATOR.incrementAndGet();
        this.originX = originX;
        this.originY = originY;
        this.tileX = tileX;
        this.tileY = tileY;
        this.item = item;
        this.dropTime = dropTime;
        this.timerStart = dropTime;
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
        return gameTime - timerStart >= pickupDelaySeconds() && isInPickupRange(playerTileX, playerTileY);
    }

    /**
     * Tells how long a fresh drop of this source stays out of reach.
     *
     * @return Seconds after the drop during which this item cannot be picked up
     */
    public double pickupDelaySeconds() {
        return source == DropSource.PLAYER ? PLAYER_DROP_PICKUP_DELAY_SECONDS : 0.0;
    }

    /**
     * Tells whether this item has been on the ground longer than {@link #LIFETIME_SECONDS}.
     * The age is measured on the game clock, so time spent paused does not count.
     *
     * @param gameTime current game time in seconds
     * @return true once the item must despawn
     */
    public boolean isExpired(double gameTime) {
        return gameTime - timerStart > LIFETIME_SECONDS;
    }

    /**
     * Tells how long this item has been on the ground, on the same reference as {@link #isExpired(double)}: the last
     * time its despawn timer started.
     *
     * @param gameTime current game time in seconds
     * @return seconds since the despawn timer started
     */
    public double ageSeconds(double gameTime) {
        return gameTime - timerStart;
    }

    /**
     * Tells how much more this pile takes.
     *
     * @return how many more units this pile takes before it reaches the maximum stack size
     */
    public int roomLeft() {
        return Math.max(0, item.getMaxStackSize() - item.getQuantity());
    }

    /**
     * Adds units dropped by the player to this pile.
     * Its pickup delay and its despawn timer start again. The pile keeps its source, its tile and its flight: a pile
     * still in the air goes on flying to the same landing tile. The caller keeps the pile within the maximum stack
     * size, see {@link #roomLeft()}.
     *
     * @param amount   units to add, positive
     * @param gameTime current game time in seconds
     * @throws IllegalArgumentException if the amount is not positive or does not fit
     */
    public void absorb(int amount, double gameTime) {
        if (amount <= 0 || amount > roomLeft()) {
            throw new IllegalArgumentException("Cannot add " + amount + " to a pile with room for " + roomLeft());
        }
        item.addQuantity(amount);
        timerStart = gameTime;
    }

    // ==================================================================
    //  Flight
    // ==================================================================

    /**
     * Progress of the flight from the origin tile to the landing tile.
     *
     * @param gameTime current game time in seconds
     * @return 0 at the throw, 1 once landed, and 1 for an item that never flew
     */
    public double flightProgress(double gameTime) {
        if (originX == tileX && originY == tileY) {
            return 1.0;
        }
        double progress = (gameTime - dropTime) / THROW_SECONDS;
        return Math.max(0.0, Math.min(1.0, progress));
    }

    /**
     * Tile X coordinate of the item at a moment of its flight. The item slows down towards the landing tile.
     *
     * @param gameTime current game time in seconds
     * @return a coordinate between the origin and the landing tile
     */
    public double flightX(double gameTime) {
        return originX + (tileX - originX) * easeOut(flightProgress(gameTime));
    }

    /**
     * Tile Y coordinate of the item at a moment of its flight.
     *
     * @param gameTime current game time in seconds
     * @return a coordinate between the origin and the landing tile
     */
    public double flightY(double gameTime) {
        return originY + (tileY - originY) * easeOut(flightProgress(gameTime));
    }

    private static double easeOut(double progress) {
        double remaining = 1.0 - progress;
        return 1.0 - remaining * remaining;
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
