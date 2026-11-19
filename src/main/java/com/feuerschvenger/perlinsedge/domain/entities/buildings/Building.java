package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import com.feuerschvenger.perlinsedge.domain.world.light.LightSource;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Abstract base class for all constructible structures in the game world.
 * Defines core properties and behaviors common to all buildings, including position,
 * type, physical properties, and interaction mechanics. Drawing lives in the rendering layer.
 */
@Slf4j
@Getter
public abstract class Building {
    /** The tile offset of the front of the building for each rotation, see {@link #frontOffset()}. */
    private static final int[][] FRONT_OFFSETS = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};

    /** X-coordinate in tile units. */
    protected final int tileX;
    /** Y-coordinate in tile units. */
    protected final int tileY;
    /** Classification of the building. */
    protected final BuildingType type;
    /** Whether the building blocks movement. Not final to allow toggling (e.g., for gates). */
    protected boolean isSolid;
    /** Whether the building can be destroyed. */
    protected final boolean isDestructible;
    /** Health the building starts with and is repaired to, from {@link BuildingStats}. */
    protected final int maxHealth;
    /** Health left, from {@code maxHealth} down to 0. */
    protected int health;
    /** The way the building faces, from 0 up to but excluding {@link BuildingType#rotationCount()}, see there. */
    protected int rotation;
    /** The light the building gives, or null for a building that gives none. It is registered while the building stands. */
    protected LightSource light;

    /**
     * Creates a new building instance at the specified tile position.
     *
     * @param tileX          X-coordinate in tile units
     * @param tileY          Y-coordinate in tile units
     * @param type           Classification of the building
     * @param isSolid        Whether the building blocks movement
     * @param isDestructible Whether the building can be destroyed
     */
    protected Building(int tileX, int tileY, BuildingType type, boolean isSolid, boolean isDestructible) {
        this.tileX = tileX;
        this.tileY = tileY;
        this.type = type;
        this.isSolid = isSolid;
        this.isDestructible = isDestructible;
        this.maxHealth = BuildingStats.maxHealth(type);
        this.health = maxHealth;
    }

    /**
     * Sets the light the building gives.
     *
     * @param light the light, or null for a building that gives none
     */
    public void setLight(LightSource light) {
        this.light = light;
    }

    /**
     * Sets the way the building faces. The value wraps around the number of ways its type can face, so any whole number
     * is a valid rotation and a type that cannot turn always keeps 0.
     *
     * @param rotation the rotation, see {@link BuildingType#rotationCount()}
     */
    public void setRotation(int rotation) {
        this.rotation = Math.floorMod(rotation, type.rotationCount());
    }

    /**
     * Gives the tile offset of the tile the building faces, which is where a station puts what it makes. Rotation 0
     * faces the lower right edge of the tile, and each turn goes a quarter turn clockwise seen from above, see
     * {@link BuildingType#rotationCount()}. A piece that cannot turn faces the lower right edge.
     *
     * @return {dx, dy} in tiles, a fresh array
     */
    public int[] frontOffset() {
        return frontOffset(0);
    }

    /**
     * Gives the tile offset of the edge neighbour a number of quarter turns clockwise, seen from above, from the tile
     * the building faces.
     *
     * @param quarterTurns the quarter turns, any whole number
     * @return {dx, dy} in tiles, a fresh array
     */
    public int[] frontOffset(int quarterTurns) {
        return FRONT_OFFSETS[Math.floorMod(rotation + quarterTurns, FRONT_OFFSETS.length)].clone();
    }

    /**
     * Takes health off the building. This is the entry point for every future source of damage, such as enemy attacks:
     * the health bar and the partial refund of a demolition already follow the health, so a source only has to call
     * it. It decides what a hit does to the health, which never goes below 0.
     *
     * @param amount health to remove, a negative amount removes nothing
     */
    public void damage(int amount) {
        health = Math.max(0, health - Math.max(0, amount));
    }

    /** @return true when the building has lost some health, which is when its health bar shows */
    public boolean isDamaged() {
        return health < maxHealth;
    }

    /** @return true when no health is left */
    public boolean isDestroyed() {
        return health <= 0;
    }

    /** @return the share of the health left, from 0 to 1 */
    public double getHealthFraction() {
        return (double) health / maxHealth;
    }

    /**
     * Tells how many items of a recipe ingredient a demolition gives back: the share of the quantity that matches the
     * share of the health left, rounded down, so a full health building returns every item and a wrecked one almost none.
     *
     * @param quantity the quantity the recipe asked for
     * @return {@code floor(quantity * health / maxHealth)}
     */
    public int refundOf(int quantity) {
        return (int) ((long) quantity * health / maxHealth);
    }

    /**
     * Tells whether clicking the building does something, such as opening a gate or a chest.
     * The player walks up to an interactable building before the interaction happens.
     *
     * @return true when {@link #onInteract(Tile)} handles the interaction
     */
    public boolean isInteractable() {
        return false;
    }

    /**
     * Handles player interaction with this building.
     *
     * @param tile The tile where the building is located
     * @return True if the interaction was handled, false to propagate
     */
    public boolean onInteract(Tile tile) {
        log.debug("Interacted with {} at ({}, {})", type.getDisplayName(), tileX, tileY);
        return false;
    }
}
