package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.domain.entities.creatures.Creature;
import com.feuerschvenger.perlinsedge.domain.entities.creatures.CreatureContext;

/**
 * The attack of a creature that never attacks: a null object, so such a creature needs no special case and no fake
 * attack. It holds no state, so one instance serves every creature.
 */
public final class NoAttack implements CreatureAttackStrategy {
    /** The one instance, since the strategy has no state. */
    public static final NoAttack INSTANCE = new NoAttack();

    /** Private so the only instance is {@link #INSTANCE}. */
    private NoAttack() {
        // Nothing to initialise: the strategy has no state
    }

    /**
     * Does nothing: the creature never attacks.
     *
     * @param self      the creature
     * @param deltaTime time since last update in seconds
     * @param context   what the creature can see of the world
     */
    @Override
    public void updateAttack(Creature self, double deltaTime, CreatureContext context) {
        // Never attacks
    }

    /**
     * Tells that the creature cannot attack.
     *
     * @param self    the creature
     * @param context what the creature can see of the world
     * @return always false
     */
    @Override
    public boolean canAttack(Creature self, CreatureContext context) {
        return false;
    }
}
