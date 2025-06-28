package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

/**
 * Defines the contract for enemy movement behavior.
 * Each specific enemy type will implement its own movement strategy.
 */
public interface EnemyMovementStrategy {
    /**
     * Updates the movement logic for an enemy.
     * @param enemy The enemy whose movement is being updated.
     * @param deltaTime Time since last update in seconds.
     * @param map The game map.
     * @param player The player character.
     */
    void updateMovement(Enemy enemy, double deltaTime, TileMap map, Player player);
}