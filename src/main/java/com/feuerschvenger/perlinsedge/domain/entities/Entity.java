package com.feuerschvenger.perlinsedge.domain.entities;

import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;

/**
 * Entity class represents a base game entity like Player, Enemies, NPCs.
 * It provides methods for setting position, taking damage, handling death, and smooth rendering.
 */
public abstract class Entity {
    // Direction constants (8-way movement) - Common for all moving entities
    public static final int DIR_UP = 0;
    public static final int DIR_UP_RIGHT = 1;
    public static final int DIR_RIGHT = 2;
    public static final int DIR_DOWN_RIGHT = 3;
    public static final int DIR_DOWN = 4;
    public static final int DIR_DOWN_LEFT = 5;
    public static final int DIR_LEFT = 6;
    public static final int DIR_UP_LEFT = 7;

    // Position state (tile-based coordinates)
    protected int currentTileX, currentTileY; // Current tile position
    protected int targetTileX, targetTileY;   // Target tile position (for movement)

    // Position interpolation state (for smooth rendering)
    protected double prevX, prevY; // Previous interpolated position
    protected double posX, posY;   // Current interpolated position

    // Health and combat
    protected double health;
    protected double maxHealth;
    protected boolean isDead = false;

    /**
     * Constructor for Entity.
     * @param startX Initial X tile coordinate.
     * @param startY Initial Y tile coordinate.
     * @param maxHealth Maximum health for this entity.
     */
    public Entity(int startX, int startY, double maxHealth) {
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        setPosition(startX, startY);
    }

    /**
     * Sets the entity's position immediately, resetting interpolation and movement targets.
     * @param x New X tile coordinate.
     * @param y New Y tile coordinate.
     */
    public void setPosition(int x, int y) {
        this.currentTileX = x;
        this.currentTileY = y;
        this.targetTileX = x; // Target is initially current position
        this.targetTileY = y;
        this.posX = x;       // Interpolated position is also current tile position
        this.posY = y;
        this.prevX = x;      // Previous interpolated position is also current tile position
        this.prevY = y;
    }

    /**
     * Takes damage, reducing the entity's health.
     * @param amount The amount of damage to take.
     */
    public void takeDamage(double amount) {
        health = Math.max(0, health - amount);
        if (health <= 0) {
            handleDeath();
        }
    }

    /**
     * Handles the entity's death logic. Subclasses can override this for specific behaviors.
     */
    public void handleDeath() {
        if (isDead) return; // Prevent multiple death handling
        System.out.println(this.getClass().getSimpleName() + " has died!");
        isDead = true;
    }

    /**
     * Revives the entity at a new position, restoring full health.
     * @param x New X tile coordinate.
     * @param y New Y tile coordinate.
     */
    public void revive(int x, int y) {
        isDead = false;
        health = maxHealth;
        setPosition(x, y);
        System.out.println(this.getClass().getSimpleName() + " respawned at (" + x + ", " + y + ")");
    }

    /**
     * Stores the current interpolated position as the previous one for the next frame's interpolation.
     */
    public void snapshot() {
        prevX = posX;
        prevY = posY;
    }

    /**
     * Calculates the interpolated rendering position for smooth movement.
     * @param alpha Interpolation factor (0.0 to 1.0).
     * @return An array containing [interpolatedX, interpolatedY].
     */
    public double[] getInterpolatedPosition(double alpha) {
        double x = prevX + (posX - prevX) * alpha;
        double y = prevY + (posY - prevY) * alpha;
        return new double[]{x, y};
    }

    public boolean reachedPathTarget() {
        return targetTileX == currentTileX && targetTileY == currentTileY;
    }

    // Getters
    public double getPosX() { return posX; }
    public double getPosY() { return posY; }
    public double getPrevX() { return prevX; }
    public double getPrevY() { return prevY; }
    public int getCurrentTileX() { return currentTileX; }
    public int getCurrentTileY() { return currentTileY; }
    public int getTargetTileX() { return targetTileX; }
    public int getTargetTileY() { return targetTileY; }
    public double getHealth() { return health; }
    public double getMaxHealth() { return maxHealth; }
    public boolean isDead() { return isDead; }

    // Abstract method for render offset, as it might vary per enemy type
    public abstract double getRenderOffsetY();

    // Setters
    public void setPosX(double posX) { this.posX = posX; }
    public void setPosY(double posY) { this.posY = posY; }
    public void setPrevY(double prevY) { this.prevY = prevY; }
    public void setPrevX(double prevX) { this.prevX = prevX; }
    public void setCurrentTileY(int currentTileY) { this.currentTileY = currentTileY; }
    public void setCurrentTileX(int currentTileX) { this.currentTileX = currentTileX; }
    public void setTargetTileX(int targetTileX) { this.targetTileX = targetTileX; }
    public void setTargetTileY(int targetTileY) { this.targetTileY = targetTileY; }
    public void setHealth(double health) { this.health = health; }
    public void setIsDead(boolean isDead) { this.isDead = isDead; }
}