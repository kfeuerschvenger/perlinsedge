package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.domain.entities.creatures.Creature;
import com.feuerschvenger.perlinsedge.domain.entities.creatures.CreatureContext;

/**
 * Attack of a skeleton: a melee strike on a player within {@link #ATTACK_RANGE_TILES}, once per cooldown.
 */
public class SkeletonAttackStrategy implements CreatureAttackStrategy {
    /** Distance in tiles from which a skeleton hits. */
    public static final double ATTACK_RANGE_TILES = 1.5;

    /**
     * Strikes the player when the skeleton can attack, and starts its cooldown.
     *
     * @param self      the skeleton
     * @param deltaTime time since last update in seconds
     * @param context   what the skeleton can see of the world: the map, the player and the occupancy
     */
    @Override
    public void updateAttack(Creature self, double deltaTime, CreatureContext context) {
        final var player = context.player();
        if (canAttack(self, context)) {
            player.takeDamage(self.getAttackDamage());
            self.resetAttackCooldown();
        }
    }

    /**
     * Tells whether the skeleton may strike now: its cooldown is over and a living player is within reach.
     *
     * @param self    the skeleton
     * @param context what the skeleton can see of the world: the map, the player and the occupancy
     * @return true when the player is alive and within {@link #ATTACK_RANGE_TILES}
     */
    @Override
    public boolean canAttack(Creature self, CreatureContext context) {
        final var player = context.player();
        if (!self.isReadyToAttack() || player.isDead()) {
            return false;
        }
        double distance = Math.hypot((double) player.getCurrentTileX() - self.getCurrentTileX(),
                (double) player.getCurrentTileY() - self.getCurrentTileY());
        return distance <= ATTACK_RANGE_TILES;
    }
}
