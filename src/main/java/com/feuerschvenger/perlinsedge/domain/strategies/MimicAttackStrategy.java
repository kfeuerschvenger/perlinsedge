package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
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

    public MimicAttackStrategy() {
        // Constructor
    }

    @Override
    public void updateAttack(Creature self, double deltaTime, CreatureContext context) {
        final Player player = context.player();
        if (canAttack(self, player)) {
            player.takeDamage(self.getAttackDamage());
            self.resetAttackCooldown();
            log.debug("Mimic attacked player");
        }
    }

    @Override
    public boolean canAttack(Creature creature, Player player) {
        if (creature.isReadyToAttack() && !player.isDead()) {
            // Check aggro state for Mimic
            if (creature.getMovementStrategy() instanceof MimicMovementStrategy movementStrategy
                    && !movementStrategy.isAggroed()) {
                return false;
            }

            double dx = (double) player.getCurrentTileX() - creature.getCurrentTileX();
            double dy = (double) player.getCurrentTileY() - creature.getCurrentTileY();
            double distance = Math.sqrt(dx * dx + dy * dy);
            return distance <= ATTACK_RANGE_TILES;
        }
        return false;
    }

}
