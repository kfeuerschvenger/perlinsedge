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
        if (canAttack(enemy, player)) {
            player.takeDamage(enemy.getAttackDamage());
            enemy.resetAttackCooldown();
        }
    }

    @Override
    public boolean canAttack(Enemy enemy, Player player) {
        if (enemy.isReadyToAttack() && !player.isDead()) {
            double dx = player.getCurrentTileX() - enemy.getCurrentTileX();
            double dy = player.getCurrentTileY() - enemy.getCurrentTileY();
            // Slime attacks when it's on the same tile as the player
            return dx == 0 && dy == 0;
        }
        return false;
    }

}