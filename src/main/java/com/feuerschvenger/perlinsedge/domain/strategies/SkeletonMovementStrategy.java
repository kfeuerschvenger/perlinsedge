package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.Entity;
import com.feuerschvenger.perlinsedge.domain.entities.creatures.Creature;
import com.feuerschvenger.perlinsedge.domain.entities.creatures.CreatureContext;

/**
 * Movement of a skeleton: it stands still until the player comes within the aggro range of
 * {@code AppConfig.enemies().skeleton()}, then walks the shortest path towards the player and stops on a tile next
 * to them, diagonals included. It gives up when the player passes the leash range or stays out of the aggro range
 * for the lose time, and then stands still again. See {@link WalkingChaser} for the walk.
 */
public class SkeletonMovementStrategy implements CreatureMovementStrategy {

    private static final double MOVE_SPEED = 2.5; // Tiles per second

    private final WalkingChaser chaser;

    /**
     * Creates a skeleton movement that waits where it stands and chases the player once it is woken by the aggro
     * range or by a hit.
     *
     * @param seed seed of its waits for a tile another enemy holds, such as {@link SpawnSeed#of(int, int)}
     */
    public SkeletonMovementStrategy(long seed) {
        this.chaser = new WalkingChaser(MOVE_SPEED, AppConfig.getInstance().enemies().skeleton(), seed);
    }

    /**
     * Runs one update of the chase of the skeleton.
     *
     * @param self      the skeleton
     * @param deltaTime time since last update in seconds
     * @param context   what the skeleton can see of the world: the map, the player and the occupancy
     */
    @Override
    public void updateMovement(Creature self, double deltaTime, CreatureContext context) {
        chaser.update(self, deltaTime, context.map(), context.player(), context.occupancy());
    }

    /**
     * A hit makes the skeleton chase the attacker even from outside its aggro range.
     *
     * @param self   the skeleton that was hurt
     * @param source who dealt the damage, or null when the source is not known
     */
    @Override
    public void onHurt(Creature self, Entity source) {
        alert();
    }

    /** Starts a chase that holds for the lose time whatever the distance, for a skeleton hurt from beyond its range. */
    public void alert() {
        chaser.alert();
    }

    /** @return how many path searches the skeleton has run, for the tests */
    int pathSearchCount() {
        return chaser.searchCount();
    }

    /** @return true while the skeleton is chasing the player */
    public boolean isChasing() {
        return chaser.isChasing();
    }
}
