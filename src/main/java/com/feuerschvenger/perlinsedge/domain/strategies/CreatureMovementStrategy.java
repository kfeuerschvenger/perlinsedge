package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.domain.entities.Entity;
import com.feuerschvenger.perlinsedge.domain.entities.creatures.Creature;
import com.feuerschvenger.perlinsedge.domain.entities.creatures.CreatureContext;

/**
 * Defines the contract for creature movement behavior.
 * Each species implements its own movement strategy.
 */
public interface CreatureMovementStrategy {
    /**
     * Updates the movement logic for a creature. A creature never starts a move to a tile another creature holds, and
     * reserves the tile it moves to before it starts.
     *
     * @param self      the creature whose movement is being updated
     * @param deltaTime time since last update in seconds
     * @param context   what the creature can see of the world: the map, the player and the occupancy
     */
    void updateMovement(Creature self, double deltaTime, CreatureContext context);

    /**
     * Reacts to the creature being hurt, before the damage is applied: a strategy that wakes up, chases or flees
     * does it here. It does nothing by default, for a strategy whose creature takes the damage without a reaction.
     *
     * @param self   the creature that was hurt
     * @param source who dealt the damage, or null when the source is not known
     */
    default void onHurt(Creature self, Entity source) {
        // No reaction by default
    }
}
