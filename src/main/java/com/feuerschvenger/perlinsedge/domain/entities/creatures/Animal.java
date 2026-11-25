package com.feuerschvenger.perlinsedge.domain.entities.creatures;

import com.feuerschvenger.perlinsedge.domain.strategies.CreatureMovementStrategy;
import com.feuerschvenger.perlinsedge.domain.strategies.NoAttack;

/**
 * The branch of creatures that live in the world without hunting the player: they are not hostile. An animal has no
 * attack of its own: it is built with {@link NoAttack} and no attack numbers, so it never needs a fake attack to
 * stand in the hierarchy. The species of animals come with their own movement strategies.
 */
public abstract class Animal extends Creature {
    private static final double NO_DAMAGE = 0.0;
    private static final double NO_COOLDOWN = 0.0;

    /**
     * Creates an animal that does not attack.
     *
     * @param startX           initial x-coordinate (tile position)
     * @param startY           initial y-coordinate (tile position)
     * @param maxHealth        maximum health points
     * @param movementStrategy movement behaviour
     */
    protected Animal(int startX, int startY, double maxHealth, CreatureMovementStrategy movementStrategy) {
        super(startX, startY, maxHealth, NO_DAMAGE, NO_COOLDOWN, movementStrategy, NoAttack.INSTANCE);
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
