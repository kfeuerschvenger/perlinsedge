package com.feuerschvenger.perlinsedge.domain.entities.enemies;

import com.feuerschvenger.perlinsedge.domain.entities.Entity;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.strategies.EnemyAttackStrategy;
import com.feuerschvenger.perlinsedge.domain.strategies.EnemyMovementStrategy;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import javafx.scene.paint.Color;

/**
 * Abstract base class for all enemy entities in the game.
 * Implements the Strategy pattern for customizable movement and attack behaviors.
 * Manages core enemy state including combat cooldowns and visual representation.
 */
public abstract class Enemy extends Entity {
    // ==================================================================
    //  Configuration Defaults
    // ==================================================================
    private static final Color DEFAULT_ENEMY_COLOR = Color.MAGENTA;

    // ==================================================================
    //  Instance Properties
    // ==================================================================
    protected Color color;
    protected double attackDamage;
    protected double attackCooldown;
    protected double attackTimer;
    protected final EnemyMovementStrategy movementStrategy;
    protected final EnemyAttackStrategy attackStrategy;

    // ==================================================================
    //  Constructor
    // ==================================================================

    /**
     * Creates a new enemy instance.
     *
     * @param startX            Initial x-coordinate (tile position)
     * @param startY            Initial y-coordinate (tile position)
     * @param maxHealth         Maximum health points
     * @param attackDamage      Base damage per attack
     * @param attackCooldown    Time between attacks (seconds)
     * @param movementStrategy  Movement behavior implementation
     * @param attackStrategy    Attack behavior implementation
     */
    public Enemy(int startX, int startY, double maxHealth,
                 double attackDamage, double attackCooldown,
                 EnemyMovementStrategy movementStrategy,
                 EnemyAttackStrategy attackStrategy) {
        super(startX, startY, maxHealth);
        this.attackDamage = attackDamage;
        this.attackCooldown = attackCooldown;
        this.attackTimer = 0.0;
        this.movementStrategy = movementStrategy;
        this.attackStrategy = attackStrategy;
        this.color = DEFAULT_ENEMY_COLOR;
    }

    // ==================================================================
    //  Core Game Loop Methods
    // ==================================================================

    /**
     * Updates enemy state including movement, attack, and cooldowns.
     * Delegates behavior to configured strategies.
     *
     * @param deltaTime Time since last update (seconds)
     * @param map       Current game map
     * @param player    Player instance
     */
    public void update(double deltaTime, TileMap map, Player player) {
        updateAttackCooldown(deltaTime);
        movementStrategy.updateMovement(this, deltaTime, map, player);
        attackStrategy.updateAttack(this, deltaTime, player);
    }

    // ==================================================================
    //  Combat Management
    // ==================================================================

    /**
     * Determines if this enemy can currently attack the player.
     *
     * @param player Player instance to evaluate
     * @return True if attack conditions are satisfied
     */
    public boolean canAttack(Player player) {
        return attackStrategy.canAttack(this, player);
    }

    /**
     * Updates the attack cooldown timer.
     *
     * @param deltaTime Time since last update (seconds)
     */
    public void updateAttackCooldown(double deltaTime) {
        attackTimer = Math.max(0, attackTimer - deltaTime);
    }

    /**
     * Checks if attack cooldown has expired.
     *
     * @return True if enemy can perform an attack
     */
    public boolean isReadyToAttack() {
        return attackTimer <= 0;
    }

    /**
     * Resets attack cooldown to maximum value.
     */
    public void resetAttackCooldown() {
        attackTimer = attackCooldown;
    }

    // ==================================================================
    //  Accessors
    // ==================================================================

    /** @return Visual representation color */
    public Color getColor() {
        return color;
    }

    /** @return Base damage per attack */
    public double getAttackDamage() {
        return attackDamage;
    }

    /** @return Time between attacks (seconds) */
    public double getAttackCooldown() {
        return attackCooldown;
    }

    /** @return Remaining attack cooldown time (seconds) */
    public double getAttackTimer() {
        return attackTimer;
    }

    /** @return Configured movement strategy */
    public EnemyMovementStrategy getMovementStrategy() {
        return movementStrategy;
    }

    /** @return Configured attack strategy */
    public EnemyAttackStrategy getAttackStrategy() {
        return attackStrategy;
    }

    // ==================================================================
    //  Mutators
    // ==================================================================

    /**
     * Sets the visual representation color.
     *
     * @param color New color value
     */
    public void setColor(Color color) {
        this.color = color;
    }

    /**
     * Sets base damage per attack.
     *
     * @param attackDamage New damage value
     */
    public void setAttackDamage(double attackDamage) {
        this.attackDamage = attackDamage;
    }

    /**
     * Sets time between attacks.
     *
     * @param attackCooldown New cooldown duration (seconds)
     */
    public void setAttackCooldown(double attackCooldown) {
        this.attackCooldown = attackCooldown;
    }

    /**
     * Directly sets attack cooldown timer.
     *
     * @param attackTimer New cooldown value (seconds)
     */
    protected void setAttackTimer(double attackTimer) {
        this.attackTimer = attackTimer;
    }

}