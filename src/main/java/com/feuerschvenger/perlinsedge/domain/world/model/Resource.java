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

    private boolean flashing = false;
    private long flashStartTime; // Cambiado a long
    private static final long FLASH_DURATION_NS = 200_000_000;

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

    public void startFlashing() {
        this.flashing = true;
        this.flashStartTime = System.nanoTime();
    }

    public boolean isFlashing() {
        if (!flashing) return false;
        long elapsed = System.nanoTime() - flashStartTime;
        if (elapsed >= FLASH_DURATION_NS) {
            flashing = false;
            return false;
        }
        return true;
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