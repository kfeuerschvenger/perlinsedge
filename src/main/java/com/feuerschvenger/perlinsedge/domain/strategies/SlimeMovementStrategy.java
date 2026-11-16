package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.pathfinding.PathNode;
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
    private static final double JUMP_SPEED_AGGRO = 0.5; // Duration of jump
    private static final double JUMP_SPEED_IDLE = 0.8;  // Duration of jump
    public static final double JUMP_HEIGHT_FACTOR = 50.0; // Public for rendering
    /** Wait before searching again after the next tile of the path turned out to be the player's. */
    private static final double PLAYER_BLOCK_WAIT_SECONDS = 0.15;

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
    private double secondsOfPlayerBlockWait;

    private final Random random;
    private final PathPlanner pathPlanner = new PathPlanner();
    private final ChaseLeash leash = new ChaseLeash(AppConfig.getInstance().enemies().slime());

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
        pathPlanner.tick(deltaTime);
        secondsOfPlayerBlockWait = Math.max(0, secondsOfPlayerBlockWait - deltaTime);

        // Next to the player the slime holds its ground and strikes instead of hopping: a hop in the air finishes
        // first, then no new one starts until the player is out of reach again
        final boolean inReach = SlimeAttackStrategy.isInReach(enemy, player);
        // The leash advances every update, in reach or not, so the hold of an alert runs out while the slime strikes
        final boolean chasing = leash.update(distanceToPlayer(enemy, player), player.isDead(), deltaTime);
        if (inReach) {
            currentPath.clear();
        } else if (chasing) {
            handleAggroBehavior(enemy, map, player);
        } else {
            // Without a chase the hops along a path to the player stop once the one in the air has landed
            currentPath.clear();
            handleIdleBehavior(enemy, map, player, deltaTime);
        }

        if (isJumping) {
            updateJump(enemy, deltaTime, map, player);
        } else if (!inReach && shouldStartJump()) {
            startNextJump(enemy, map, player);
        }
    }

    /** @return true while the slime is in the air during a hop */
    public boolean isJumping() { return isJumping; }

    /** @return true while the slime is chasing the player */
    public boolean isChasing() { return leash.isChasing(); }

    /** Starts a chase that holds for the lose time whatever the distance, for a slime hurt from beyond its range. */
    public void alert() { leash.alert(); }

    /** @return how far the hop in the air has gone, from 0.0 at take-off to 1.0 at landing, 0.0 on the ground */
    public double getJumpPhase() { return jumpPhase; }

    /** @return how many path searches the slime has run, for the tests */
    int pathSearchCount() { return pathPlanner.searchCount(); }

    private void resetIdleTimer() {
        idleTimer = IDLE_JUMP_INTERVAL_MIN +
                (IDLE_JUMP_INTERVAL_MAX - IDLE_JUMP_INTERVAL_MIN) * random.nextDouble();
    }

    private static double distanceToPlayer(Enemy enemy, Player player) {
        return Math.hypot(player.getCurrentTileX() - enemy.getCurrentTileX(),
                player.getCurrentTileY() - enemy.getCurrentTileY());
    }

    private void handleAggroBehavior(Enemy enemy, TileMap map, Player player) {
        totalJumpTime = JUMP_SPEED_AGGRO;

        if (secondsOfPlayerBlockWait <= 0 && needsNewPath(enemy)) {
            // Capped, and skipped for a while after a failed search (the player may be unreachable)
            List<PathNode> pathToPlayer = pathPlanner.findPath(
                    map,
                    enemy.getCurrentTileX(), enemy.getCurrentTileY(),
                    player.getCurrentTileX(), player.getCurrentTileY()
            );

            if (pathToPlayer != null && !pathToPlayer.isEmpty()) {
                currentPath = new LinkedList<>(pathToPlayer);
                removeCurrentPositionFromPath(enemy);
            } else {
                moveRandomly(enemy, map, player); // Fallback to random move if no path
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

    private void handleIdleBehavior(Enemy enemy, TileMap map, Player player, double deltaTime) {
        totalJumpTime = JUMP_SPEED_IDLE;
        idleTimer -= deltaTime;

        if (idleTimer <= 0) {
            moveRandomly(enemy, map, player);
            resetIdleTimer();
        }
    }

    private void moveRandomly(Enemy enemy, TileMap map, Player player) {
        if (isJumping) return;

        List<int[]> possibleMoves = findPossibleMoves(enemy, map, player);
        if (!possibleMoves.isEmpty()) {
            int[] chosenMove = possibleMoves.get(random.nextInt(possibleMoves.size()));
            enemy.setTargetTileX(chosenMove[0]); // Update enemy's target
            enemy.setTargetTileY(chosenMove[1]); // Update enemy's target
            startJump();
        }
    }

    private List<int[]> findPossibleMoves(Enemy enemy, TileMap map, Player player) {
        List<int[]> moves = new ArrayList<>();

        for (int i = 0; i < DX.length; i++) {
            int nextX = enemy.getCurrentTileX() + DX[i];
            int nextY = enemy.getCurrentTileY() + DY[i];

            if (!isPlayerTile(nextX, nextY, player)
                    && canHop(map, enemy.getCurrentTileX(), enemy.getCurrentTileY(), nextX, nextY)) {
                moves.add(new int[]{nextX, nextY});
            }
        }
        return moves;
    }

    /** A hop lands on a free tile, and a diagonal hop may not cut the corner of a blocked tile. */
    private boolean canHop(TileMap map, int fromX, int fromY, int toX, int toY) {
        if (!isValidMove(map, toX, toY)) {
            return false;
        }
        final boolean diagonal = toX != fromX && toY != fromY;
        return !diagonal || (isValidMove(map, toX, fromY) && isValidMove(map, fromX, toY));
    }

    private boolean isValidMove(TileMap map, int x, int y) {
        return x >= 0 && x < map.getWidth() &&
                y >= 0 && y < map.getHeight() &&
                map.getTile(x, y).isWalkable(); // The rule the pathfinder uses: water, resources and solid buildings block
    }

    /** Takes off. The path is kept: a hop along a path to the player is one step of it. */
    private void startJump() {
        isJumping = true;
        currentJumpTime = 0;
    }

    private void updateJump(Enemy enemy, double deltaTime, TileMap map, Player player) {
        currentJumpTime += deltaTime;
        jumpPhase = Math.min(1.0, currentJumpTime / totalJumpTime);

        // The player may choose the tile the hop is going to after it began: the hop bends away in the air instead of
        // being undone on landing
        if (isPlayerTile(enemy.getTargetTileX(), enemy.getTargetTileY(), player)
                && (enemy.getTargetTileX() != enemy.getCurrentTileX()
                || enemy.getTargetTileY() != enemy.getCurrentTileY())) {
            reaimHop(enemy, map, player);
        }

        // Interpolate enemy's posX/posY for smooth jump animation
        enemy.setPosX(enemy.getPrevX() + (enemy.getTargetTileX() - enemy.getPrevX()) * jumpPhase);
        enemy.setPosY(enemy.getPrevY() + (enemy.getTargetTileY() - enemy.getPrevY()) * jumpPhase);

        if (jumpPhase >= 1.0) {
            completeJump(enemy, player);
        }
    }

    /**
     * Aims the hop in the air at the free tile nearest to the slime among those around the tile it is over, so the
     * interpolation carries on from where the slime is. When no such tile exists the hop returns to the tile it took
     * off from over the time it has left. The path to the player is searched again after the landing.
     */
    private void reaimHop(Enemy enemy, TileMap map, Player player) {
        final int fromX = (int) Math.round(enemy.getPosX());
        final int fromY = (int) Math.round(enemy.getPosY());
        int bestX = enemy.getCurrentTileX();
        int bestY = enemy.getCurrentTileY();
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < DX.length; i++) {
            final int x = fromX + DX[i];
            final int y = fromY + DY[i];
            if (isPlayerTile(x, y, player) || !canHop(map, fromX, fromY, x, y)) {
                continue;
            }
            final double distance = Math.hypot(x - enemy.getPosX(), y - enemy.getPosY());
            if (distance < bestDistance) {
                bestDistance = distance;
                bestX = x;
                bestY = y;
            }
        }
        enemy.setTargetTileX(bestX);
        enemy.setTargetTileY(bestY);
        currentPath.clear();
    }

    /**
     * Lands the hop. The tile the hop was re-aimed to is never the player's, so the check here only covers the slime
     * that took off from a tile the player has since stepped onto: it stays where it is and the path to the player is
     * searched again.
     */
    private void completeJump(Enemy enemy, Player player) {
        isJumping = false;
        currentJumpTime = 0;
        jumpPhase = 0;
        if (isPlayerTile(enemy.getTargetTileX(), enemy.getTargetTileY(), player)) {
            enemy.setTargetTileX(enemy.getCurrentTileX());
            enemy.setTargetTileY(enemy.getCurrentTileY());
            currentPath.clear();
        }
        enemy.setCurrentTileX(enemy.getTargetTileX()); // Update enemy's current tile position
        enemy.setCurrentTileY(enemy.getTargetTileY()); // Update enemy's current tile position
        enemy.setPosX(enemy.getCurrentTileX()); // Ensure interpolated matches current tile at end of jump
        enemy.setPosY(enemy.getCurrentTileY());
    }

    /** True for the tile the player stands on and the tile the player is stepping to. */
    private static boolean isPlayerTile(int x, int y, Player player) {
        return (x == player.getCurrentTileX() && y == player.getCurrentTileY())
                || (x == player.getTargetTileX() && y == player.getTargetTileY());
    }

    private boolean shouldStartJump() {
        return !currentPath.isEmpty() && !isJumping;
    }

    private void startNextJump(Enemy enemy, TileMap map, Player player) {
        PathNode nextStep = currentPath.removeFirst();
        // Something may have been built on the tile since the path was searched, or the player may have stepped on it
        if (!canHop(map, enemy.getCurrentTileX(), enemy.getCurrentTileY(), nextStep.x, nextStep.y)
                || isPlayerTile(nextStep.x, nextStep.y, player)) {
            currentPath.clear();
            // Wait for the player's step to end instead of searching again on every update meanwhile
            secondsOfPlayerBlockWait = PLAYER_BLOCK_WAIT_SECONDS;
            return;
        }
        enemy.setTargetTileX(nextStep.x);
        enemy.setTargetTileY(nextStep.y);

        if (!enemy.reachedPathTarget()) {
            startJump();
        } else {
            // If the path just has the current tile (e.g., path of 1), just complete it immediately
            completeJump(enemy, player);
        }
    }

}