package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;

/**
 * Implements the attack behavior for Slime enemies.
 * A slime strikes a player on a tile next to its own, after a squash that telegraphs the strike and followed by a wait,
 * see {@link SlimeStrikeCycle}. Both times come from {@code AppConfig}.
 */
public class SlimeAttackStrategy implements EnemyAttackStrategy {

    private final SlimeStrikeCycle cycle;

    public SlimeAttackStrategy() {
        AppConfig.GameMechanicsConfig mechanics = AppConfig.getInstance().gameMechanics();
        this.cycle = new SlimeStrikeCycle(mechanics.getSlimeWindupSeconds(), mechanics.getSlimeStrikeWaitSeconds());
    }

    /**
     * Tells whether a player is within the reach of an enemy: on the same tile or on one of the eight tiles around it.
     * The slime stops hopping and starts its strike cycle while this holds.
     *
     * @param enemy  the enemy
     * @param player the player
     * @return true when the player is alive and no more than one tile away in each direction
     */
    public static boolean isInReach(Enemy enemy, Player player) {
        return !player.isDead()
                && Math.abs(player.getCurrentTileX() - enemy.getCurrentTileX()) <= 1
                && Math.abs(player.getCurrentTileY() - enemy.getCurrentTileY()) <= 1;
    }

    @Override
    public void updateAttack(Enemy enemy, double deltaTime, Player player) {
        if (cycle.update(deltaTime, isInReach(enemy, player))) {
            player.takeDamage(enemy.getAttackDamage());
            enemy.resetAttackCooldown();
        }
    }

    /**
     * A slime never strikes through the attack cooldown of the combat manager: its own cycle times the strike and
     * applies the damage in {@link #updateAttack}.
     *
     * @return always false
     */
    @Override
    public boolean canAttack(Enemy enemy, Player player) {
        return false;
    }

    /**
     * Gives how far the squash before a strike has gone, which the renderer draws.
     *
     * @return 0 when the slime is not squashing, rising to 1 at the moment of the strike
     */
    public double getWindupProgress() {
        return cycle.getWindupProgress();
    }

    /**
     * Gives the phase of the strike cycle.
     *
     * @return the phase
     */
    public SlimeStrikeCycle.Phase getPhase() {
        return cycle.getPhase();
    }

}
