package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.Entity;
import com.feuerschvenger.perlinsedge.domain.entities.creatures.Creature;
import com.feuerschvenger.perlinsedge.domain.entities.creatures.CreatureContext;

/**
 * Movement of a mimic: it stays still until the player comes within the aggro range of
 * {@code AppConfig.enemies().mimic()} or hurts it, then it walks the shortest path to the player. It gives up under
 * the same leash rules as every walking enemy and goes back to being a still chest. See {@link WalkingChaser}.
 */
public class MimicMovementStrategy implements CreatureMovementStrategy {

    private static final double MOVE_SPEED_AGGRO = 3.0; // Tiles per second when aggroed

    private final WalkingChaser chaser;

    /**
     * Creates a mimic movement that waits as a chest and chases the player once it is woken by the aggro range or by
     * a hit.
     *
     * @param seed seed of its waits for a tile another enemy holds, such as {@link SpawnSeed#of(int, int)}
     */
    public MimicMovementStrategy(long seed) {
        this.chaser = new WalkingChaser(MOVE_SPEED_AGGRO, AppConfig.getInstance().enemies().mimic(), seed);
    }

    /**
     * Runs one update of the chase of the mimic.
     *
     * @param self      the mimic
     * @param deltaTime time since last update in seconds
     * @param context   what the mimic can see of the world: the map, the player and the occupancy
     */
    @Override
    public void updateMovement(Creature self, double deltaTime, CreatureContext context) {
        chaser.update(self, deltaTime, context.map(), context.player(), context.occupancy());
    }

    /**
     * A hit wakes the mimic even from outside its activation range, and every hit renews the alert so a mimic that
     * keeps being hurt from afar keeps chasing.
     *
     * @param self   the mimic that was hurt
     * @param source who dealt the damage, or null when the source is not known
     */
    @Override
    public void onHurt(Creature self, Entity source) {
        setAggroed(true);
    }

    /**
     * Wakes or calms the mimic.
     *
     * @param aggroed true to start a chase that holds for the lose time whatever the distance, see
     *                {@link ChaseLeash#alert()}, false to end it
     */
    public void setAggroed(boolean aggroed) {
        if (aggroed) {
            chaser.alert();
        } else {
            chaser.release();
        }
    }

    /** @return true while the mimic is chasing the player */
    public boolean isAggroed() {
        return chaser.isChasing();
    }
}
