package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.entities.Player;

/**
 * Defines the contract for enemy attack behavior.
 * Each specific enemy type will implement its own attack strategy.
 */
public interface EnemyAttackStrategy {
    /**
     * Updates the attack logic for an enemy.
     * @param enemy The enemy whose attack is being updated.
     * @param deltaTime Time since last update in seconds.
     * @param player The player character.
     */
    void updateAttack(Enemy enemy, double deltaTime, Player player);

    /**
     * Checks if the enemy can attack the player.
     * @param enemy The enemy to check
     * @param player The player to check
     * @return true if attack conditions are met
     */
    boolean canAttack(Enemy enemy, Player player);

}