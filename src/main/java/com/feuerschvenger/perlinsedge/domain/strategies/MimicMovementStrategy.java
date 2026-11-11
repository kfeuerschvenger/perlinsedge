package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

/**
 * Implements the movement behavior for Mimic enemies.
 * Mimics are stationary until the player comes within {@link #ACTIVATION_RANGE_TILES} or hurts them, then they
 * chase the player in a straight line.
 */
public class MimicMovementStrategy implements EnemyMovementStrategy {
    /** Distance in tiles at which a still mimic notices the player and springs to life. */
    public static final double ACTIVATION_RANGE_TILES = 2.5;

    private boolean aggroed = false;
    private static final double MOVE_SPEED_AGGRO = 3.0; // Tiles per second when aggroed

    public MimicMovementStrategy() {
        // Constructor
    }

    @Override
    public void updateMovement(Enemy enemy, double deltaTime, TileMap map, Player player) {
        // Mimics are usually stationary, or move slowly when aggroed
        enemy.snapshot(); // Snapshot for interpolation

        if (!aggroed && !player.isDead() && isPlayerWithinActivationRange(enemy, player)) {
            aggroed = true;
        }

        if (aggroed) {
            // Simple direct movement towards player when aggroed
            double dx = player.getCurrentTileX() - enemy.getPosX();
            double dy = player.getCurrentTileY() - enemy.getPosY();
            double dist = Math.hypot(dx, dy);

            if (dist > 0.1) { // If not very close to target tile center
                double maxStep = MOVE_SPEED_AGGRO * deltaTime;
                double px = enemy.getPosX();
                double py = enemy.getPosY();
                px += (dx / dist) * maxStep;
                py += (dy / dist) * maxStep;
                enemy.setPosX(px);
                enemy.setPosY(py);

                // Update currentTileX/Y once target tile is reached
                if (Math.hypot(player.getCurrentTileX() - enemy.getPosX(), player.getCurrentTileY() - enemy.getPosY()) < 0.5) {
                    enemy.setCurrentTileX(player.getCurrentTileX());
                    enemy.setCurrentTileY(player.getCurrentTileY());
                }
            } else {
                enemy.setCurrentTileX(player.getCurrentTileX());
                enemy.setCurrentTileY(player.getCurrentTileY());
                enemy.setPosX(enemy.getCurrentTileX());
                enemy.setPosY(enemy.getCurrentTileY());
            }
        }
    }

    private static boolean isPlayerWithinActivationRange(Enemy enemy, Player player) {
        return Math.hypot(player.getCurrentTileX() - enemy.getCurrentTileX(),
                player.getCurrentTileY() - enemy.getCurrentTileY()) <= ACTIVATION_RANGE_TILES;
    }

    public void setAggroed(boolean aggroed) {
        this.aggroed = aggroed;
    }

    public boolean isAggroed() {
        return aggroed;
    }

}