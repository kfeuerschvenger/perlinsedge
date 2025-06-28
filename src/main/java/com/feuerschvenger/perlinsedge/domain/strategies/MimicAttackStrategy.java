package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.entities.Player;

/**
 * Implements the attack behavior for Mimic enemies.
 * Mimics attack only when aggroed and player is in range.
 */
public class MimicAttackStrategy implements EnemyAttackStrategy {
    private static final double ATTACK_RANGE_TILES = 1.0; // Mimics attack when adjacent or on same tile

    public MimicAttackStrategy() {
        // Constructor
    }

    @Override
    public void updateAttack(Enemy enemy, double deltaTime, Player player) {
        MimicMovementStrategy movementStrategy = (MimicMovementStrategy) enemy.getMovementStrategy(); // Cast to access aggro state
        if (movementStrategy.isAggroed() && canAttackPlayer(enemy, player)) {
            player.takeDamage(enemy.getAttackDamage());
            enemy.setAttackTimer(enemy.getAttackCooldown());
            System.out.println("Mimic attacked player!");
        }
    }

    private boolean canAttackPlayer(Enemy enemy, Player player) {
        if (enemy.getAttackTimer() <= 0 && !player.isDead()) {
            double dx = player.getCurrentTileX() - enemy.getCurrentTileX();
            double dy = player.getCurrentTileY() - enemy.getCurrentTileY();
            double distance = Math.sqrt(dx * dx + dy * dy);
            return distance <= ATTACK_RANGE_TILES;
        }
        return false;
    }
}