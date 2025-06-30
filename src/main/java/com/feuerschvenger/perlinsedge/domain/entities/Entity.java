package com.feuerschvenger.perlinsedge.domain.entities;

/**
 * Base class for all game entities (Player, Enemies, NPCs, etc.).
 * Provides core functionality for positioning, health management, death handling,
 * and smooth rendering through position interpolation.
 */
public abstract class Entity {
    // ==================================================================
    //  Direction Constants (8-way movement)
    // ==================================================================

    public static final int DIR_UP = 0;
    public static final int DIR_UP_RIGHT = 1;
    public static final int DIR_RIGHT = 2;
    public static final int DIR_DOWN_RIGHT = 3;
    public static final int DIR_DOWN = 4;
    public static final int DIR_DOWN_LEFT = 5;
    public static final int DIR_LEFT = 6;
    public static final int DIR_UP_LEFT = 7;

    // ==================================================================
    //  Position State
    // ==================================================================

    // Tile-based coordinates
    protected int currentTileX, currentTileY;  // Current tile position
    protected int targetTileX, targetTileY;    // Movement target tile position

    // Interpolated positions for smooth rendering
    protected double prevX, prevY;  // Previous frame's position
    protected double posX, posY;    // Current frame's position

    // ==================================================================
    //  Health State
    // ==================================================================

    protected double health;
    protected final double maxHealth;  // Immutable max health
    protected boolean isDead = false;

    // ==================================================================
    //  Constructor
    // ==================================================================

    /**
     * Creates a new entity at the specified position.
     *
     * @param startX    Initial X tile coordinate
     * @param startY    Initial Y tile coordinate
     * @param maxHealth Maximum health for this entity
     */
    public Entity(int startX, int startY, double maxHealth) {
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        setPosition(startX, startY);
    }

    // ==================================================================
    //  Public API - Position Management
    // ==================================================================

    /**
     * Immediately sets the entity's position, resetting all movement and interpolation.
     *
     * @param x New X tile coordinate
     * @param y New Y tile coordinate
     */
    public void setPosition(int x, int y) {
        this.currentTileX = x;
        this.currentTileY = y;
        this.targetTileX = x;
        this.targetTileY = y;
        this.posX = x;
        this.posY = y;
        this.prevX = x;
        this.prevY = y;
    }

    /**
     * Stores the current position as the previous position for interpolation.
     * Should be called at the beginning of each frame update.
     */
    public void snapshot() {
        prevX = posX;
        prevY = posY;
    }

    /**
     * Calculates an interpolated position for smooth rendering.
     *
     * @param alpha Interpolation factor (0.0 = previous frame, 1.0 = current frame)
     * @return Array containing [interpolatedX, interpolatedY]
     */
    public double[] getInterpolatedPosition(double alpha) {
        double x = prevX + (posX - prevX) * alpha;
        double y = prevY + (posY - prevY) * alpha;
        return new double[]{x, y};
    }

    /**
     * Checks if the entity has reached its movement target.
     */
    public boolean reachedPathTarget() {
        return targetTileX == currentTileX && targetTileY == currentTileY;
    }

    // ==================================================================
    //  Public API - Health Management
    // ==================================================================

    /**
     * Applies damage to the entity and triggers death if health reaches zero.
     *
     * @param amount Damage amount to apply
     */
    public void takeDamage(double amount) {
        if (isDead) return;

        health = Math.max(0, health - amount);

        if (health <= 0) {
            handleDeath();
        }
    }

    /**
     * Handles death logic. Can be overridden by subclasses for custom behavior.
     */
    public void handleDeath() {
        if (isDead) return;

        isDead = true;
        System.out.println(this.getClass().getSimpleName() + " has died!");
    }

    /**
     * Revives the entity at a new position with full health.
     *
     * @param x X tile coordinate for respawn
     * @param y Y tile coordinate for respawn
     */
    public void revive(int x, int y) {
        isDead = false;
        health = maxHealth;
        setPosition(x, y);
        System.out.println(this.getClass().getSimpleName() + " respawned at (" + x + ", " + y + ")");
    }

    // ==================================================================
    //  Abstract Methods
    // ==================================================================

    /**
     * Gets the vertical render offset for this entity.
     * Implementations should return the appropriate offset for their sprite.
     */
    public abstract double getRenderOffsetY();

    // ==================================================================
    //  Getters
    // ==================================================================

    // Position accessors
    public double getPosX() { return posX; }
    public double getPosY() { return posY; }
    public double getPrevX() { return prevX; }
    public double getPrevY() { return prevY; }
    public int getCurrentTileX() { return currentTileX; }
    public int getCurrentTileY() { return currentTileY; }
    public int getTargetTileX() { return targetTileX; }
    public int getTargetTileY() { return targetTileY; }

    // Health accessors
    public double getHealth() { return health; }
    public double getMaxHealth() { return maxHealth; }
    public boolean isDead() { return isDead; }

    // ==================================================================
    //  Setters
    // ==================================================================

    public void setPosX(double posX) { this.posX = posX; }
    public void setPosY(double posY) { this.posY = posY; }
    public void setCurrentTileX(int currentTileX) { this.currentTileX = currentTileX; }
    public void setCurrentTileY(int currentTileY) { this.currentTileY = currentTileY; }
    public void setTargetTileX(int targetTileX) { this.targetTileX = targetTileX; }
    public void setTargetTileY(int targetTileY) { this.targetTileY = targetTileY; }

}