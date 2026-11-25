package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.domain.entities.creatures.Creature;
import com.feuerschvenger.perlinsedge.domain.entities.creatures.CreatureContext;
import lombok.extern.slf4j.Slf4j;

/**
 * Implements the attack behavior for Mimic enemies.
 * Mimics attack only when aggroed and player is in range.
 */
@Slf4j
public class MimicAttackStrategy implements CreatureAttackStrategy {
    /** Distance in tiles from which a mimic hits: the eight tiles around it, where its chase ends. */
    private static final double ATTACK_RANGE_TILES = 1.5;

    private final MimicMovementStrategy movement;

    /**
     * Creates the attack of a mimic, which strikes only while the movement of that mimic is chasing.
     *
     * @param movement the movement strategy of the same mimic, which tells whether it is aggroed
     */
    public MimicAttackStrategy(MimicMovementStrategy movement) {
        this.movement = movement;
    }

    /**
     * Strikes the player when the mimic can attack, and starts its cooldown.
     *
     * @param self      the mimic
     * @param deltaTime time since last update in seconds
     * @param context   what the mimic can see of the world: the map, the player and the occupancy
     */
    @Override
    public void updateAttack(Creature self, double deltaTime, CreatureContext context) {
        final var player = context.player();
        if (canAttack(self, context)) {
            player.takeDamage(self.getAttackDamage());
            self.resetAttackCooldown();
            log.debug("Mimic attacked player");
        }
    }

    /**
     * Tells whether the mimic may strike now: its cooldown is over, it is aggroed and a living player is within reach.
     *
     * @param self    the mimic
     * @param context what the mimic can see of the world: the map, the player and the occupancy
     * @return true when the mimic is aggroed and the player is alive and within {@link #ATTACK_RANGE_TILES}
     */
    @Override
    public boolean canAttack(Creature self, CreatureContext context) {
        final var player = context.player();
        if (self.isReadyToAttack() && !player.isDead()) {
            if (!movement.isAggroed()) {
                return false;
            }

            double dx = (double) player.getCurrentTileX() - self.getCurrentTileX();
            double dy = (double) player.getCurrentTileY() - self.getCurrentTileY();
            double distance = Math.sqrt(dx * dx + dy * dy);
            return distance <= ATTACK_RANGE_TILES;
        }
        return false;
    }

}
