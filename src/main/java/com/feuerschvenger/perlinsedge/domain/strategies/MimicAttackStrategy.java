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
        if (canAttack(enemy, player)) {
            player.takeDamage(enemy.getAttackDamage());
            enemy.resetAttackCooldown();
            System.out.println("Mimic attacked player!");
        }
    }

    @Override
    public boolean canAttack(Enemy enemy, Player player) {
        if (enemy.isReadyToAttack() && !player.isDead()) {
            // Check aggro state for Mimic
            if (enemy.getMovementStrategy() instanceof MimicMovementStrategy movementStrategy) {
                if (!movementStrategy.isAggroed()) return false;
            }

            double dx = player.getCurrentTileX() - enemy.getCurrentTileX();
            double dy = player.getCurrentTileY() - enemy.getCurrentTileY();
            double distance = Math.sqrt(dx * dx + dy * dy);
            return distance <= ATTACK_RANGE_TILES;
        }
        return false;
    }

}