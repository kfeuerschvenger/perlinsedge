package com.feuerschvenger.perlinsedge.domain.entities.creatures;

import com.feuerschvenger.perlinsedge.domain.strategies.CreatureAttackStrategy;
import com.feuerschvenger.perlinsedge.domain.strategies.CreatureMovementStrategy;

/**
 * The branch of creatures that live in the world without hunting the player: they are not hostile. It adds nothing to
 * {@link Creature} yet, the species of animals come with their own movement strategies.
 */
public abstract class Animal extends Creature {

    /**
     * Creates an animal.
     *
     * @param startX           initial x-coordinate (tile position)
     * @param startY           initial y-coordinate (tile position)
     * @param maxHealth        maximum health points
     * @param attackDamage     base damage per attack
     * @param attackCooldown   time between attacks (seconds)
     * @param movementStrategy movement behaviour
     * @param attackStrategy   attack behaviour
     */
    protected Animal(int startX, int startY, double maxHealth, double attackDamage, double attackCooldown,
                     CreatureMovementStrategy movementStrategy, CreatureAttackStrategy attackStrategy) {
        super(startX, startY, maxHealth, attackDamage, attackCooldown, movementStrategy, attackStrategy);
    }

    /**
     * Animals are not hostile.
     *
     * @return always false
     */
    @Override
    public boolean isHostile() {
        return false;
    }
}
