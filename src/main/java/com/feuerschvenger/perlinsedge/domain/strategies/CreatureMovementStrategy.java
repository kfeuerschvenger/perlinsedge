package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.creatures.Creature;
import com.feuerschvenger.perlinsedge.domain.pathfinding.CreatureOccupancy;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

/**
 * Defines the contract for creature movement behavior.
 * Each species implements its own movement strategy.
 */
public interface CreatureMovementStrategy {
    /**
     * Updates the movement logic for a creature. A creature never starts a move to a tile another creature holds, and
     * reserves the tile it moves to before it starts.
     *
     * @param creature  the creature whose movement is being updated
     * @param deltaTime time since last update in seconds
     * @param map       the game map
     * @param player    the player character
     * @param occupancy which creature holds which tile
     */
    void updateMovement(Creature creature, double deltaTime, TileMap map, Player player, CreatureOccupancy occupancy);
}
