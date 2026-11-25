package com.feuerschvenger.perlinsedge.domain.entities.enemies;

import com.feuerschvenger.perlinsedge.domain.entities.creatures.Creature;
import com.feuerschvenger.perlinsedge.domain.strategies.CreatureAttackStrategy;
import com.feuerschvenger.perlinsedge.domain.strategies.CreatureMovementStrategy;

/**
 * The branch of hostile creatures: the monsters. It adds nothing to {@link Creature} but the answer to whether the
 * creature is hostile, so code that means "monster" keeps asking for an enemy.
 */
public abstract class Enemy extends Creature {

    /**
     * Creates a new enemy instance.
     *
     * @param startX           initial x-coordinate (tile position)
     * @param startY           initial y-coordinate (tile position)
     * @param maxHealth        maximum health points
     * @param attackDamage     base damage per attack
     * @param attackCooldown   time between attacks (seconds)
     * @param movementStrategy movement behavior implementation
     * @param attackStrategy   attack behavior implementation
     */
    protected Enemy(int startX, int startY, double maxHealth, double attackDamage, double attackCooldown,
                    CreatureMovementStrategy movementStrategy, CreatureAttackStrategy attackStrategy) {
        super(startX, startY, maxHealth, attackDamage, attackCooldown, movementStrategy, attackStrategy);
    }

    /**
     * Enemies are hostile.
     *
     * @return always true
     */
    @Override
    public boolean isHostile() {
        return true;
    }
}
