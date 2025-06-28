package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.entities.Player;

/**
 * Implements the attack behavior for Slime enemies.
 * Slimes attack the player if they land on the same tile and their attack cooldown is ready.
 */
public class SlimeAttackStrategy implements EnemyAttackStrategy {
    private static final double ATTACK_RANGE_TILES = 0.5; // Slimes attack when on the same tile

    public SlimeAttackStrategy() {
        // Constructor, if needed
    }

    @Override
    public void updateAttack(Enemy enemy, double deltaTime, Player player) {
        if (canAttackPlayer(enemy, player)) {
            player.takeDamage(enemy.getAttackDamage()); // Use inherited takeDamage
            enemy.setAttackTimer(enemy.getAttackCooldown()); // Reset cooldown
        }
    }

    private boolean canAttackPlayer(Enemy enemy, Player player) {
        // Ensure the enemy's movement strategy reports it's not mid-air if that's a requirement
        // For Slime, they attack on landing, so `isJumping` should be false.
        // We'll need a way to check jump state from the movement strategy.
        // For now, let's assume they attack if on the same tile and not jumping
        // (the jump state is managed by SlimeMovementStrategy, needs to be exposed or checked directly)
        // Let's assume for now the `updateAttack` is called *after* `updateMovement` and jump is resolved.
        if (enemy.getAttackTimer() <= 0 && !player.isDead()) {
            double dx = player.getCurrentTileX() - enemy.getCurrentTileX();
            double dy = player.getCurrentTileY() - enemy.getCurrentTileY();
            // Slime attacks when it's on the same tile as the player
            return dx == 0 && dy == 0;
        }
        return false;
    }
}