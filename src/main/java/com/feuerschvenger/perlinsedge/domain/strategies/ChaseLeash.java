package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.config.AppConfig.EnemyConfig.ChaseRange;

/**
 * Decides whether an enemy is chasing the player, with the same rules for every enemy type.
 * A chase starts when the player is within the aggro range. Once started it goes on until the player passes the
 * leash range, which is larger than the aggro range so the edge does not flicker, or until the player has stayed
 * outside the aggro range for the lose time. A dead player ends the chase.
 */
final class ChaseLeash {

    private final ChaseRange range;
    private boolean chasing;
    private double secondsOutsideAggro;

    /**
     * Creates a leash that is not chasing.
     *
     * @param range the aggro, leash and lose distances of the enemy type
     */
    ChaseLeash(ChaseRange range) {
        this.range = range;
    }

    /**
     * Advances the chase decision by one update.
     *
     * @param distanceTiles distance from the enemy to the player in tiles
     * @param playerDead    true when there is nothing left to chase
     * @param deltaTime     seconds since the previous update
     * @return true when the enemy chases the player in this update
     */
    boolean update(double distanceTiles, boolean playerDead, double deltaTime) {
        if (playerDead) {
            release();
            return false;
        }
        if (!chasing) {
            if (distanceTiles <= range.aggroTiles()) {
                alert();
            }
            return chasing;
        }
        if (distanceTiles > range.leashTiles()) {
            release();
        } else if (distanceTiles > range.aggroTiles()) {
            secondsOutsideAggro += deltaTime;
            if (secondsOutsideAggro >= range.loseSeconds()) {
                release();
            }
        } else {
            secondsOutsideAggro = 0;
        }
        return chasing;
    }

    /** Starts a chase regardless of distance, for an enemy that is hurt from beyond its aggro range. */
    void alert() {
        chasing = true;
        secondsOutsideAggro = 0;
    }

    /** Ends the chase. */
    void release() {
        chasing = false;
        secondsOutsideAggro = 0;
    }

    /** @return true while a chase is on */
    boolean isChasing() {
        return chasing;
    }
}
