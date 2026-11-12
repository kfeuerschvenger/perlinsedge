package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

/**
 * Movement of a skeleton: it stands still until the player comes within the aggro range of
 * {@code AppConfig.enemies().skeleton()}, then walks the shortest path towards the player and stops on a tile next
 * to them, diagonals included. It gives up when the player passes the leash range or stays out of the aggro range
 * for the lose time, and then stands still again. See {@link WalkingChaser} for the walk.
 */
public class SkeletonMovementStrategy implements EnemyMovementStrategy {

    private static final double MOVE_SPEED = 2.5; // Tiles per second

    private final WalkingChaser chaser =
            new WalkingChaser(MOVE_SPEED, AppConfig.getInstance().enemies().skeleton());

    @Override
    public void updateMovement(Enemy enemy, double deltaTime, TileMap map, Player player) {
        chaser.update(enemy, deltaTime, map, player);
    }

    /** @return true while the skeleton is chasing the player */
    public boolean isChasing() {
        return chaser.isChasing();
    }
}
