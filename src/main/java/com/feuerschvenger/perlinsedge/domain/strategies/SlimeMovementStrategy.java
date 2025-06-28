package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.pathfinding.PathNode;
import com.feuerschvenger.perlinsedge.domain.pathfinding.Pathfinder;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Implements the movement behavior for Slime enemies.
 * Slimes jump towards the player when aggroed or jump randomly when idle.
 */
public class SlimeMovementStrategy implements EnemyMovementStrategy {
    // Configuration constants (specific to Slime movement)
    private static final double IDLE_JUMP_INTERVAL_MIN = 2.0;
    private static final double IDLE_JUMP_INTERVAL_MAX = 4.0;
    private static final double AGGRO_RANGE = 8.0;
    private static final double JUMP_SPEED_AGGRO = 0.5; // Duration of jump
    private static final double JUMP_SPEED_IDLE = 0.8;  // Duration of jump
    public static final double JUMP_HEIGHT_FACTOR = 50.0; // Public for rendering

    // Movement directions
    private static final int[] DX = {0, 1, 0, -1, 1, -1, 1, -1};
    private static final int[] DY = {1, 0, -1, 0, 1, 1, -1, -1};

    // Movement state (now part of the strategy instance)
    private LinkedList<PathNode> currentPath;
    private double currentJumpTime;
    private double totalJumpTime;
    private double idleTimer;
    private boolean isJumping;
    private double jumpPhase; // 0.0 to 1.0 representing jump progress

    private final Random random;

    public SlimeMovementStrategy() {
        this.currentPath = new LinkedList<>();
        this.random = new Random();
        resetIdleTimer();
        this.isJumping = false;
        this.jumpPhase = 0.0;
    }

    @Override
    public void updateMovement(Enemy enemy, double deltaTime, TileMap map, Player player) {
        // Slime-specific movement logic
        enemy.snapshot(); // Snapshot entity's position for interpolation

        if (shouldAggro(enemy, player)) {
            handleAggroBehavior(enemy, map, player);
        } else {
            handleIdleBehavior(enemy, map, deltaTime);
        }

        if (isJumping) {
            updateJump(enemy, deltaTime);
        } else if (shouldStartJump(enemy)) {
            startNextJump(enemy);
        }
    }

    public boolean isJumping() { return isJumping; }
    public double getJumpPhase() { return jumpPhase; }

    private void resetIdleTimer() {
        idleTimer = IDLE_JUMP_INTERVAL_MIN +
                (IDLE_JUMP_INTERVAL_MAX - IDLE_JUMP_INTERVAL_MIN) * random.nextDouble();
    }

    private boolean shouldAggro(Enemy enemy, Player player) {
        if (player.isDead()) { // Use isDead() from Entity
            return false;
        }
        double dx = player.getCurrentTileX() - enemy.getCurrentTileX(); // Use inherited getters
        double dy = player.getCurrentTileY() - enemy.getCurrentTileY();
        return Math.sqrt(dx * dx + dy * dy) <= AGGRO_RANGE;
    }

    private void handleAggroBehavior(Enemy enemy, TileMap map, Player player) {
        totalJumpTime = JUMP_SPEED_AGGRO;

        if (needsNewPath(enemy)) {
            Pathfinder pathfinder = new Pathfinder(map);
            List<PathNode> pathToPlayer = pathfinder.findPath(
                    enemy.getCurrentTileX(), enemy.getCurrentTileY(),
                    player.getCurrentTileX(), player.getCurrentTileY()
            );

            if (pathToPlayer != null && !pathToPlayer.isEmpty()) {
                currentPath = new LinkedList<>(pathToPlayer);
                removeCurrentPositionFromPath(enemy);
            } else {
                moveRandomly(enemy, map); // Fallback to random move if no path
            }
        }
    }

    private boolean needsNewPath(Enemy enemy) {
        return currentPath.isEmpty() || enemy.reachedPathTarget();
    }

    private void removeCurrentPositionFromPath(Enemy enemy) {
        if (!currentPath.isEmpty() &&
                currentPath.getFirst().x == enemy.getCurrentTileX() &&
                currentPath.getFirst().y == enemy.getCurrentTileY()) {
            currentPath.removeFirst();
        }
    }

    private void handleIdleBehavior(Enemy enemy, TileMap map, double deltaTime) {
        totalJumpTime = JUMP_SPEED_IDLE;
        idleTimer -= deltaTime;

        if (idleTimer <= 0) {
            moveRandomly(enemy, map);
            resetIdleTimer();
        }
    }

    private void moveRandomly(Enemy enemy, TileMap map) {
        if (isJumping) return;

        List<int[]> possibleMoves = findPossibleMoves(enemy, map);
        if (!possibleMoves.isEmpty()) {
            int[] chosenMove = possibleMoves.get(random.nextInt(possibleMoves.size()));
            enemy.setTargetTileX(chosenMove[0]); // Update enemy's target
            enemy.setTargetTileY(chosenMove[1]); // Update enemy's target
            startJump(enemy);
        }
    }

    private List<int[]> findPossibleMoves(Enemy enemy, TileMap map) {
        List<int[]> moves = new ArrayList<>();

        for (int i = 0; i < DX.length; i++) {
            int nextX = enemy.getCurrentTileX() + DX[i];
            int nextY = enemy.getCurrentTileY() + DY[i];

            if (isValidMove(map, nextX, nextY)) {
                moves.add(new int[]{nextX, nextY});
            }
        }
        return moves;
    }

    private boolean isValidMove(TileMap map, int x, int y) {
        return x >= 0 && x < map.getWidth() &&
                y >= 0 && y < map.getHeight() &&
                map.getTile(x, y).getType().isWalkable() && !map.getTile(x, y).hasResource() && !map.getTile(x,y).hasBuilding(); // Also check for buildings
    }

    private void startJump(Enemy enemy) {
        isJumping = true;
        currentJumpTime = 0;
        // currentPath.clear(); // Clear path only if this is a random jump, not part of pathfinding
        // For pathfinding, we're jumping *along* the path, so don't clear it.
    }

    private void updateJump(Enemy enemy, double deltaTime) {
        currentJumpTime += deltaTime;
        jumpPhase = Math.min(1.0, currentJumpTime / totalJumpTime);

        // Interpolate enemy's posX/posY for smooth jump animation
        enemy.setPosX(enemy.getPrevX() + (enemy.getTargetTileX() - enemy.getPrevX()) * jumpPhase);
        enemy.setPosY(enemy.getPrevY() + (enemy.getTargetTileY() - enemy.getPrevY()) * jumpPhase);

        if (jumpPhase >= 1.0) {
            completeJump(enemy);
        }
    }

    private void completeJump(Enemy enemy) {
        isJumping = false;
        currentJumpTime = 0;
        jumpPhase = 0;
        enemy.setCurrentTileX(enemy.getTargetTileX()); // Update enemy's current tile position
        enemy.setCurrentTileY(enemy.getTargetTileY()); // Update enemy's current tile position
        enemy.setPosX(enemy.getCurrentTileX()); // Ensure interpolated matches current tile at end of jump
        enemy.setPosY(enemy.getCurrentTileY());
    }

    private boolean shouldStartJump(Enemy enemy) {
        return !currentPath.isEmpty() && !isJumping;
    }

    private void startNextJump(Enemy enemy) {
        PathNode nextStep = currentPath.removeFirst();
        enemy.setTargetTileX(nextStep.x);
        enemy.setTargetTileY(nextStep.y);

        if (!enemy.reachedPathTarget()) {
            startJump(enemy);
        } else {
            // If the path just has the current tile (e.g., path of 1), just complete it immediately
            completeJump(enemy);
        }
    }

}