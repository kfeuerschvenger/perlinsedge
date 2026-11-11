package com.feuerschvenger.perlinsedge.domain.world.model;

import com.feuerschvenger.perlinsedge.config.AppConfig;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a harvestable resource in the game world with health management,
 * visual variation, and loot generation capabilities. Resources can be damaged
 * and depleted, dropping items when fully harvested.
 */
public class Resource {
    // Identification and Configuration
    private static final AtomicLong ID_COUNTER = new AtomicLong();
    private static final AppConfig.GameMechanicsConfig GAME_MECHANICS = AppConfig.getInstance().gameMechanics();

    private final long id;
    private final ResourceType type;
    private final long visualSeed;  // For sprite variations

    private boolean struck = false;
    private double hitGameTime;
    private static final double FLASH_DURATION_SECONDS = 0.2;

    // State Management
    private int currentHealth;
    private boolean isDepleted;
    private long lastHitTime;

    public Resource(ResourceType type, long visualSeed) {
        this.id = ID_COUNTER.incrementAndGet();
        this.type = type;
        this.visualSeed = visualSeed;
        this.currentHealth = GAME_MECHANICS.getResourceBaseHealth(type);
        this.isDepleted = false;
        this.lastHitTime = 0;
    }

    // ==================================================================
    //  GETTERS
    // ==================================================================

    public ResourceType getType() {
        return type;
    }

    public long getVisualSeed() {
        return visualSeed;
    }

    public boolean isDepleted() {
        return isDepleted;
    }

    public long getLastHitTime() {
        return lastHitTime;
    }

    // ==================================================================
    //  VISUAL AND FLASHING
    // ==================================================================

    /**
     * Starts the hit flash and stamps the hit on the game clock, so the shake pauses with the game.
     *
     * @param gameTime the game time of the hit in seconds
     */
    public void startFlashing(double gameTime) {
        this.struck = true;
        this.hitGameTime = gameTime;
    }

    /**
     * Returns how long ago the resource was last struck on the game clock, so the renderer can shake it. A hit stamped
     * ahead of the clock belongs to a game that was reset, so it counts as never struck.
     *
     * @param gameTime the current game time in seconds
     * @return the elapsed game seconds, or infinity when it was never struck or the clock is behind the hit
     */
    public double secondsSinceHit(double gameTime) {
        return struck && gameTime >= hitGameTime ? gameTime - hitGameTime : Double.POSITIVE_INFINITY;
    }

    /**
     * Tells whether the hit flash is still showing, measured on the game clock so it pauses together with the shake.
     *
     * @param gameTime the current game time in seconds
     * @return true while the flash lasts
     */
    public boolean isFlashing(double gameTime) {
        return secondsSinceHit(gameTime) < FLASH_DURATION_SECONDS;
    }

    // ==================================================================
    //  CORE FUNCTIONALITY
    // ==================================================================

    /**
     * Applies damage to the resource.
     *
     * @param damage Amount of damage to apply
     * @return true if the resource was depleted by this damage
     */
    public boolean takeDamage(int damage) {
        if (isDepleted) return false;

        currentHealth = Math.max(0, currentHealth - damage);
        lastHitTime = System.currentTimeMillis();

        if (currentHealth <= 0) {
            isDepleted = true;
            return true;
        }
        return false;
    }

    // ==================================================================
    //  IDENTITY OPERATIONS
    // ==================================================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Resource resource = (Resource) o;
        return id == resource.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}