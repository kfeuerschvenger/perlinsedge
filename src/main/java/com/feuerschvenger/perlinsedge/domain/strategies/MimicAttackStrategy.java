package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import lombok.extern.slf4j.Slf4j;

/**
 * Implements the attack behavior for Mimic enemies.
 * Mimics attack only when aggroed and player is in range.
 */
@Slf4j
public class MimicAttackStrategy implements EnemyAttackStrategy {
    /** Distance in tiles from which a mimic hits: the eight tiles around it, where its chase ends. */
    private static final double ATTACK_RANGE_TILES = 1.5;

    public MimicAttackStrategy() {
        // Constructor
    }

    @Override
    public void updateAttack(Enemy enemy, double deltaTime, Player player) {
        if (canAttack(enemy, player)) {
            player.takeDamage(enemy.getAttackDamage());
            enemy.resetAttackCooldown();
            log.debug("Mimic attacked player");
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