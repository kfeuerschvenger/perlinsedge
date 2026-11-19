package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.pathfinding.EnemyOccupancy;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

/**
 * Movement of a mimic: it stays still until the player comes within the aggro range of
 * {@code AppConfig.enemies().mimic()} or hurts it, then it walks the shortest path to the player. It gives up under
 * the same leash rules as every walking enemy and goes back to being a still chest. See {@link WalkingChaser}.
 */
public class MimicMovementStrategy implements EnemyMovementStrategy {

    private static final double MOVE_SPEED_AGGRO = 3.0; // Tiles per second when aggroed

    private final WalkingChaser chaser;

    /**
     * Creates a mimic movement that stays still.
     *
     * @param seed seed of its waits for a tile another enemy holds, such as {@link SpawnSeed#of(int, int)}
     */
    public MimicMovementStrategy(long seed) {
        this.chaser = new WalkingChaser(MOVE_SPEED_AGGRO, AppConfig.getInstance().enemies().mimic(), seed);
    }

    @Override
    public void updateMovement(Enemy enemy, double deltaTime, TileMap map, Player player, EnemyOccupancy occupancy) {
        chaser.update(enemy, deltaTime, map, player, occupancy);
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
