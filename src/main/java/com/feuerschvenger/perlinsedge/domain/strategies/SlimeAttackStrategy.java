package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.creatures.Creature;
import com.feuerschvenger.perlinsedge.domain.entities.creatures.CreatureContext;

/**
 * Implements the attack behavior for Slime enemies.
 * A slime strikes a player on a tile next to its own, after a squash that telegraphs the strike and followed by a wait,
 * see {@link SlimeStrikeCycle}. Both times come from {@code AppConfig}.
 */
public class SlimeAttackStrategy implements CreatureAttackStrategy {

    private final SlimeStrikeCycle cycle;
    private final SlimeMovementStrategy movement;

    /**
     * Creates the attack of a slime, which does not squash while the movement of that slime has it in the air.
     *
     * @param movement the movement strategy of the same slime, which tells whether it is jumping
     */
    public SlimeAttackStrategy(SlimeMovementStrategy movement) {
        final var mechanics = AppConfig.getInstance().gameMechanics();
        this.cycle = new SlimeStrikeCycle(mechanics.getSlimeWindupSeconds(), mechanics.getSlimeStrikeWaitSeconds());
        this.movement = movement;
    }

    /**
     * Tells whether a player is within the reach of a creature: on the same tile or on one of the eight tiles around
     * it.
     * The slime stops hopping and starts its strike cycle while this holds.
     *
     * @param creature the creature
     * @param player   the player
     * @return true when the player is alive and no more than one tile away in each direction
     */
    public static boolean isInReach(Creature creature, Player player) {
        return !player.isDead()
                && Math.abs(player.getCurrentTileX() - creature.getCurrentTileX()) <= 1
                && Math.abs(player.getCurrentTileY() - creature.getCurrentTileY()) <= 1;
    }

    /**
     * Advances the strike cycle and strikes the player when it comes to the strike.
     *
     * @param self      the slime
     * @param deltaTime time since last update in seconds
     * @param context   what the slime can see of the world: the map, the player and the occupancy
     */
    @Override
    public void updateAttack(Creature self, double deltaTime, CreatureContext context) {
        final var player = context.player();
        // A slime in the air does not squash: the telegraph starts on the tile where it lands, so every strike shows it
        final boolean landed = !movement.isJumping();
        if (cycle.update(deltaTime, landed && isInReach(self, player))) {
            player.takeDamage(self.getAttackDamage());
            self.resetAttackCooldown();
        }
    }

    /**
     * A slime never strikes through the attack cooldown of the combat manager: its own cycle times the strike and
     * applies the damage in {@link #updateAttack}.
     *
     * @param self    the slime
     * @param context what the slime can see of the world
     * @return always false
     */
    @Override
    public boolean canAttack(Creature self, CreatureContext context) {
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
