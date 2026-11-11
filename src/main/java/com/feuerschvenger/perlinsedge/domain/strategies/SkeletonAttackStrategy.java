package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;

/**
 * Attack of a skeleton: a melee strike on a player within {@link #ATTACK_RANGE_TILES}, once per cooldown.
 */
public class SkeletonAttackStrategy implements EnemyAttackStrategy {
    /** Distance in tiles from which a skeleton hits. */
    public static final double ATTACK_RANGE_TILES = 1.5;

    @Override
    public void updateAttack(Enemy enemy, double deltaTime, Player player) {
        if (canAttack(enemy, player)) {
            player.takeDamage(enemy.getAttackDamage());
            enemy.resetAttackCooldown();
        }
    }

    @Override
    public boolean canAttack(Enemy enemy, Player player) {
        if (!enemy.isReadyToAttack() || player.isDead()) {
            return false;
        }
        double distance = Math.hypot(player.getCurrentTileX() - enemy.getCurrentTileX(),
                player.getCurrentTileY() - enemy.getCurrentTileY());
        return distance <= ATTACK_RANGE_TILES;
    }
}
