package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.creatures.Creature;
import com.feuerschvenger.perlinsedge.domain.entities.creatures.CreatureContext;

/**
 * Attack of a skeleton: a melee strike on a player within {@link #ATTACK_RANGE_TILES}, once per cooldown.
 */
public class SkeletonAttackStrategy implements CreatureAttackStrategy {
    /** Distance in tiles from which a skeleton hits. */
    public static final double ATTACK_RANGE_TILES = 1.5;

    @Override
    public void updateAttack(Creature self, double deltaTime, CreatureContext context) {
        final Player player = context.player();
        if (canAttack(self, player)) {
            player.takeDamage(self.getAttackDamage());
            self.resetAttackCooldown();
        }
    }

    @Override
    public boolean canAttack(Creature creature, Player player) {
        if (!creature.isReadyToAttack() || player.isDead()) {
            return false;
        }
        double distance = Math.hypot((double) player.getCurrentTileX() - creature.getCurrentTileX(),
                (double) player.getCurrentTileY() - creature.getCurrentTileY());
        return distance <= ATTACK_RANGE_TILES;
    }
}
