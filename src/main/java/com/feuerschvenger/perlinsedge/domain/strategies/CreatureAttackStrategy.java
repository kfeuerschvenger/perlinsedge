package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.domain.entities.creatures.Creature;
import com.feuerschvenger.perlinsedge.domain.entities.creatures.CreatureContext;

/**
 * Defines the contract for creature attack behavior.
 * Each species implements its own attack strategy.
 */
public interface CreatureAttackStrategy {
    /**
     * Updates the attack logic for a creature.
     *
     * @param self      the creature whose attack is being updated
     * @param deltaTime time since last update in seconds
     * @param context   what the creature can see of the world: the map, the player and the occupancy
     */
    void updateAttack(Creature self, double deltaTime, CreatureContext context);

    /**
     * Checks if the creature can attack the player.
     *
     * @param self    the creature to check
     * @param context what the creature can see of the world: the map, the player and the occupancy
     * @return true if attack conditions are met
     */
    boolean canAttack(Creature self, CreatureContext context);

}
