package com.feuerschvenger.perlinsedge.domain.entities.enemies;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.strategies.EnemyAttackStrategy;
import com.feuerschvenger.perlinsedge.domain.strategies.EnemyMovementStrategy;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

/**
 * Represents a Skeleton enemy with ranged attack capabilities.
 * Skeletons can shoot arrows at players from a distance, implementing unique combat behavior.
 */
public class Skeleton extends Enemy {
    // ==================================================================
    //  Default Configuration Constants
    // ==================================================================
    private static final double MAX_HEALTH = 30.0;
    private static final double ATTACK_DAMAGE = 8.0;
    private static final double ATTACK_COOLDOWN = 3.0;

    // ==================================================================
    //  Constructors
    // ==================================================================

    /**
     * Creates a skeleton with default attributes using custom strategies.
     *
     * @param startX            Initial x-coordinate (tile position)
     * @param startY            Initial y-coordinate (tile position)
     * @param movementStrategy  Custom movement behavior implementation
     * @param attackStrategy    Custom attack behavior implementation
     */
    public Skeleton(int startX, int startY,
                    EnemyMovementStrategy movementStrategy,
                    EnemyAttackStrategy attackStrategy) {
        this(startX, startY, MAX_HEALTH, ATTACK_DAMAGE,
                ATTACK_COOLDOWN, movementStrategy, attackStrategy);
    }

    /**
     * Creates a fully customizable skeleton instance.
     *
     * @param startX            Initial x-coordinate (tile position)
     * @param startY            Initial y-coordinate (tile position)
     * @param maxHealth         Maximum health points
     * @param attackDamage      Base damage per attack
     * @param attackCooldown    Time between attacks (seconds)
     * @param movementStrategy  Movement behavior implementation
     * @param attackStrategy    Attack behavior implementation
     */
    public Skeleton(int startX, int startY, double maxHealth, double attackDamage, double attackCooldown,
                    EnemyMovementStrategy movementStrategy, EnemyAttackStrategy attackStrategy) {
        super(startX, startY, maxHealth, attackDamage, attackCooldown, movementStrategy, attackStrategy);
    }

    // ==================================================================
    //  Core Game Loop Methods
    // ==================================================================

    /**
     * Updates skeleton state including ranged attack behavior.
     * Skeletons attempt to maintain distance while attacking with arrows.
     *
     * @param deltaTime Time since last update (seconds)
     * @param map       Current game map
     * @param player    Player instance
     */
    @Override
    public void update(double deltaTime, TileMap map, Player player) {
        super.update(deltaTime, map, player);
        rangedBehavior(player);
    }

    // ==================================================================
    //  Rendering Properties
    // ==================================================================

    @Override
    public double getRenderOffsetY() {
        return 0; // Skeletons render at standard height
    }

    // ==================================================================
    //  Combat Behavior Methods
    // ==================================================================

    /**
     * Implements skeleton-specific ranged combat behavior.
     * Skeletons will attempt to maintain optimal archery distance.
     *
     * @param player Player instance to target
     */
    private void rangedBehavior(Player player) {
        // TODO: Implement ranged combat logic
        // 1. Calculate distance to player
        // 2. If too close, retreat to optimal archery range
        // 3. If in range, fire arrow projectiles
        // 4. Implement arrow cooldown mechanics
    }
}