package com.feuerschvenger.perlinsedge.domain.entities.creatures;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.pathfinding.CreatureOccupancy;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

/**
 * What a creature may look at while it updates: the map, the player and which creature holds which tile. Strategies
 * read the world through it, so a strategy that needs more of the world gets it here and not through more parameters.
 *
 * @param map       the current game map
 * @param player    the player
 * @param occupancy which creature holds which tile, shared by every creature of the map
 */
public record CreatureContext(TileMap map, Player player, CreatureOccupancy occupancy) {
}
