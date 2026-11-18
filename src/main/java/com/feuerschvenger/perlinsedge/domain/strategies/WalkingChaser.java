package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.config.AppConfig.EnemyConfig.ChaseRange;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.pathfinding.EnemyOccupancy;
import com.feuerschvenger.perlinsedge.domain.pathfinding.PathNode;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

import java.util.LinkedList;
import java.util.Random;

/**
 * The walking chase shared by the enemies that follow the player on foot: it walks the shortest path to the player
 * and stops on a tile next to them, diagonals included, and it gives up under the rules of {@link ChaseLeash}.
 * The enemy only ever walks tile centre to tile centre along a path that the pathfinder built from walkable tiles
 * without cutting a corner, so it never enters water, a resource or a building.
 * The path is searched again every {@link #REPATH_INTERVAL_SECONDS}, and sooner when the player has moved to another
 * tile, so the enemy follows a moving player. Between two tile centres the search starts from the tile the enemy is
 * walking to, which keeps the step in progress as the first step of the new path: starting from the tile it left
 * could send it back across the corner of a blocked tile.
 * The enemy never enters the tile of the player.
 */
final class WalkingChaser {

    private static final double REPATH_INTERVAL_SECONDS = 0.5;
    /**
     * Shortest time between two searches caused by the player changing tile, however often that happens. A player
     * running across the map would otherwise start a search per enemy every few frames.
     */
    private static final double MIN_REPATH_SPACING_SECONDS = 0.4;
    /** Wait before searching again after the next tile of the path turned out to be the player's. */
    private static final double PLAYER_BLOCK_WAIT_SECONDS = 0.15;
    private static final double AT_TILE_CENTRE_EPSILON = 1e-9;
    /** Extra cost of a tile another enemy holds in a search: a detour up to this long is taken instead. */
    private static final double HELD_TILE_PENALTY = 6.0;
    /** Shortest and longest first wait for a next tile that another enemy holds, so a crowd does not act in step. */
    private static final double HELD_WAIT_MIN_SECONDS = 0.15;
    private static final double HELD_WAIT_MAX_SECONDS = 0.45;
    /** Longest wait for a held tile: the wait doubles while the same tile stays held, up to this. */
    private static final double MAX_HELD_WAIT_SECONDS = 1.5;
    /** How much longer each repeated wait for the same held tile is. */
    private static final double HELD_WAIT_GROWTH_FACTOR = 2.0;
    /** Marks that no step of the path is blocked. */
    private static final int NO_STEP = Integer.MIN_VALUE;

    private final double speedTilesPerSecond;
    private final ChaseLeash leash;
    private final LinkedList<PathNode> path = new LinkedList<>();
    private final PathPlanner pathPlanner = new PathPlanner();
    private double secondsUntilRepath;
    private double secondsOfPlayerBlockWait;
    /** The step of the path another enemy holds, the wait the chaser last gave it and the time left of it. */
    private int blockedStepX = NO_STEP;
    private int blockedStepY = NO_STEP;
    private double blockedWaitSeconds;
    private double secondsOfStepWait;
    private int plannedPlayerX = Integer.MIN_VALUE;
    private int plannedPlayerY = Integer.MIN_VALUE;
    private final Random random = new Random();
    /** Which enemy holds which tile, set on every update. */
    private EnemyOccupancy occupancy;

    /**
     * Creates a chaser that is not chasing.
     *
     * @param speedTilesPerSecond walking speed
     * @param range               aggro, leash and lose distances
     */
    WalkingChaser(double speedTilesPerSecond, ChaseRange range) {
        this.speedTilesPerSecond = speedTilesPerSecond;
        this.leash = new ChaseLeash(range);
    }

    /**
     * Runs one update of the chase and walks the enemy along its path.
     *
     * @param enemy     the enemy to move
     * @param deltaTime seconds since the previous update
     * @param map       the map to search and walk
     * @param player    the player being chased
     * @param occupancy which enemy holds which tile: the enemy never steps onto a tile another enemy holds
     */
    void update(Enemy enemy, double deltaTime, TileMap map, Player player, EnemyOccupancy occupancy) {
        this.occupancy = occupancy;
        pathPlanner.tick(deltaTime);
        secondsUntilRepath -= deltaTime;
        secondsOfPlayerBlockWait = Math.max(0, secondsOfPlayerBlockWait - deltaTime);
        secondsOfStepWait = Math.max(0, secondsOfStepWait - deltaTime);

        final boolean chasing = leash.update(distanceToPlayer(enemy, player), player.isDead(), deltaTime);
        if (!chasing || isNextToPlayer(enemy, player)) {
            finishCurrentStepOnly(enemy);
        } else if (isWaitingOnHeldStep()) {
            // The path is kept and nothing is searched while the step waits to be let go, whatever the repath timer
            // says: only a player who changed tile, once the wait is over, makes the path worth searching again
            if (playerMovedSincePlan(player)) {
                replan(enemy, map, player);
            }
        } else if ((path.isEmpty() && secondsOfPlayerBlockWait <= 0)
                || secondsUntilRepath <= 0 || playerMovedSincePlan(player)) {
            replan(enemy, map, player);
        }
        follow(enemy, deltaTime, map, player);
    }

    /** Starts a chase that holds for the lose time whatever the distance, see {@link ChaseLeash#alert()}. */
    void alert() {
        leash.alert();
    }

    /** Ends the chase, the enemy finishes the step it is on. */
    void release() {
        leash.release();
    }

    /** @return how many path searches the chaser has run, for the tests */
    int searchCount() {
        return pathPlanner.searchCount();
    }

    /** @return true while the enemy is chasing the player */
    boolean isChasing() {
        return leash.isChasing();
    }

    /**
     * Tells whether the player changed tile since the last search and a new one is allowed: the spacing between two
     * searches has passed and, for an enemy that waits on a tile another enemy holds, so has the wait.
     */
    private boolean playerMovedSincePlan(Player player) {
        return secondsOfStepWait <= 0
                && secondsUntilRepath <= REPATH_INTERVAL_SECONDS - MIN_REPATH_SPACING_SECONDS
                && (player.getCurrentTileX() != plannedPlayerX || player.getCurrentTileY() != plannedPlayerY);
    }

    /** @return true while the path is kept because its next step is held by another enemy */
    private boolean isWaitingOnHeldStep() {
        return blockedStepX != NO_STEP && !path.isEmpty();
    }

    private void clearBlockedStep() {
        blockedStepX = NO_STEP;
        blockedStepY = NO_STEP;
        blockedWaitSeconds = 0;
        secondsOfStepWait = 0;
    }

    /**
     * Waits for a tile another enemy holds. The first wait is random, so a crowd does not act in step, and it doubles,
     * up to {@link #MAX_HELD_WAIT_SECONDS}, each time the same tile is still held when it is tried again.
     *
     * @return true when the tile was already held after an earlier wait, which means its holder is not passing through
     */
    private boolean waitForHeldStep(PathNode step) {
        final boolean repeated = step.x == blockedStepX && step.y == blockedStepY;
        if (repeated) {
            blockedWaitSeconds = Math.min(MAX_HELD_WAIT_SECONDS, blockedWaitSeconds * HELD_WAIT_GROWTH_FACTOR);
        } else {
            blockedStepX = step.x;
            blockedStepY = step.y;
            blockedWaitSeconds = HELD_WAIT_MIN_SECONDS
                    + (HELD_WAIT_MAX_SECONDS - HELD_WAIT_MIN_SECONDS) * random.nextDouble();
        }
        secondsOfStepWait = blockedWaitSeconds;
        return repeated;
    }

    private void replan(Enemy enemy, TileMap map, Player player) {
        secondsUntilRepath = REPATH_INTERVAL_SECONDS;
        plannedPlayerX = player.getCurrentTileX();
        plannedPlayerY = player.getCurrentTileY();
        final boolean midStep = isMidStep(enemy);
        final int originX = midStep ? enemy.getTargetTileX() : enemy.getCurrentTileX();
        final int originY = midStep ? enemy.getTargetTileY() : enemy.getCurrentTileY();
        final var result = pathPlanner.findPath(map, originX, originY, plannedPlayerX,
                plannedPlayerY, occupancy.penaltyFor(enemy, HELD_TILE_PENALTY));
        if (!result.reachable()) {
            return;
        }
        final var found = result.nodes();
        path.clear();
        if (midStep) {
            // The search leaves its start out: the tile being walked to stays the first step
            path.add(new PathNode(originX, originY));
        }
        path.addAll(found);
        forgetBlockedStepOffThePath();
        stopOneTileShortOfThePlayer(player, midStep);
    }

    /** Forgets the step that was blocked when the new path no longer starts with it. */
    private void forgetBlockedStepOffThePath() {
        if (path.isEmpty() || path.peek().x != blockedStepX || path.peek().y != blockedStepY) {
            clearBlockedStep();
        }
    }

    /**
     * The path ends on the tile of the player, the enemy stops one tile short of it. A step in progress that happens
     * to be that tile stays, and follow() turns the enemy back.
     */
    private void stopOneTileShortOfThePlayer(Player player, boolean midStep) {
        if (path.size() > (midStep ? 1 : 0) && isPlayerTile(path.getLast(), player)) {
            path.removeLast();
        }
    }

    private void follow(Enemy enemy, double deltaTime, TileMap map, Player player) {
        enemy.snapshot();
        double remaining = speedTilesPerSecond * deltaTime;
        while (remaining > 0 && !path.isEmpty()) {
            final PathNode next = path.peek();
            if (mustStopBeforeStep(enemy, next, map, player)) {
                return;
            }
            remaining = advanceTowards(enemy, next, remaining);
        }
    }

    /**
     * Checks the next step of the path before the enemy walks it, and reacts when it cannot be walked.
     *
     * @return true when the enemy must stop for this update, with the path already adjusted to the reason
     */
    private boolean mustStopBeforeStep(Enemy enemy, PathNode next, TileMap map, Player player) {
        if (isPlayerTile(next, player)) {
            // The player stands on or is stepping to the tile the enemy is walking to: go back to the tile it left
            // and wait, without a new search on every update meanwhile
            turnBackToTileCentre(enemy);
            secondsOfPlayerBlockWait = PLAYER_BLOCK_WAIT_SECONDS;
            return true;
        }
        if (!isWalkable(map, next)) {
            // Something was built on the path after it was searched: go back to the tile centre the enemy left,
            // or stay on the one it stands on, and search again from there
            turnBackToTileCentre(enemy);
            secondsUntilRepath = 0;
            return true;
        }
        return !isStepHeldBy(enemy, next) && !claimStep(enemy, next);
    }

    /**
     * Asks for a tile that is not held by this enemy yet.
     *
     * @return true when the tile was reserved and can be walked, false when the enemy has to stand still
     */
    private boolean claimStep(Enemy enemy, PathNode next) {
        if (secondsOfStepWait > 0) {
            // Another enemy held this tile a moment ago: stand still until the wait is over
            return false;
        }
        if (occupancy.tryReserve(enemy, next.x, next.y)) {
            clearBlockedStep();
            return true;
        }
        if (isMidStep(enemy)) {
            // The tile being walked to was taken by another enemy: go back to the tile centre instead of
            // standing between two tiles, and search from there
            turnBackToTileCentre(enemy);
            return false;
        }
        // Another enemy holds the tile: stand still with the path kept and try again after a wait that
        // grows while the tile stays held
        if (waitForHeldStep(next)) {
            // Still held after a wait: the holder stays, so the path is dropped and searched again once
            // the longer wait is over, priced to go around the held tiles when a detour is cheap
            path.clear();
            secondsOfPlayerBlockWait = secondsOfStepWait;
            secondsUntilRepath = Math.max(secondsUntilRepath, secondsOfStepWait);
        }
        return false;
    }

    /**
     * Moves the enemy towards the next tile of the path, as far as the distance left allows.
     *
     * @return the distance still left to walk after this step
     */
    private double advanceTowards(Enemy enemy, PathNode next, double remaining) {
        final double dx = next.x - enemy.getPosX();
        final double dy = next.y - enemy.getPosY();
        final double distance = Math.hypot(dx, dy);

        if (distance <= remaining) {
            enemy.setPosX(next.x);
            enemy.setPosY(next.y);
            enemy.setCurrentTileX(next.x);
            enemy.setCurrentTileY(next.y);
            enemy.setTargetTileX(next.x);
            enemy.setTargetTileY(next.y);
            path.poll();
            return remaining - distance;
        }
        enemy.setPosX(enemy.getPosX() + dx / distance * remaining);
        enemy.setPosY(enemy.getPosY() + dy / distance * remaining);
        enemy.setTargetTileX(next.x);
        enemy.setTargetTileY(next.y);
        return 0;
    }

    /** Drops the rest of the path but keeps the step in progress, so the enemy never stops between two tiles. */
    private void finishCurrentStepOnly(Enemy enemy) {
        clearBlockedStep();
        if (path.isEmpty()) {
            return;
        }
        if (isMidStep(enemy)) {
            final PathNode inProgress = path.peek();
            path.clear();
            path.add(inProgress);
        } else {
            path.clear();
        }
    }

    private void turnBackToTileCentre(Enemy enemy) {
        path.clear();
        clearBlockedStep();
        enemy.setTargetTileX(enemy.getCurrentTileX());
        enemy.setTargetTileY(enemy.getCurrentTileY());
        if (isMidStep(enemy)) {
            path.add(new PathNode(enemy.getCurrentTileX(), enemy.getCurrentTileY()));
        }
    }

    /**
     * True for the tile the enemy stands on or is already stepping to, when the occupancy really has it as the holder:
     * such a tile is walked without asking again. A tile the enemy only believes to hold is asked for like any other.
     */
    private boolean isStepHeldBy(Enemy enemy, PathNode node) {
        final boolean ownTile = (node.x == enemy.getTargetTileX() && node.y == enemy.getTargetTileY())
                || (node.x == enemy.getCurrentTileX() && node.y == enemy.getCurrentTileY());
        return ownTile && occupancy.ownerOf(node.x, node.y) == enemy;
    }

    private static boolean isWalkable(TileMap map, PathNode node) {
        if (node.x < 0 || node.y < 0 || node.x >= map.getWidth() || node.y >= map.getHeight()) {
            return false;
        }
        final var tile = map.getTile(node.x, node.y);
        return tile != null && tile.isWalkable();
    }

    /** True for the tile the player stands on and the tile the player is stepping to. */
    private static boolean isPlayerTile(PathNode node, Player player) {
        return (node.x == player.getCurrentTileX() && node.y == player.getCurrentTileY())
                || (node.x == player.getTargetTileX() && node.y == player.getTargetTileY());
    }

    private static boolean isMidStep(Enemy enemy) {
        return Math.abs(enemy.getPosX() - enemy.getCurrentTileX()) > AT_TILE_CENTRE_EPSILON
                || Math.abs(enemy.getPosY() - enemy.getCurrentTileY()) > AT_TILE_CENTRE_EPSILON;
    }

    /** Chebyshev distance of one tile or less: the eight tiles around the player, and the player's own tile. */
    private static boolean isNextToPlayer(Enemy enemy, Player player) {
        return Math.max(Math.abs(player.getCurrentTileX() - enemy.getCurrentTileX()),
                Math.abs(player.getCurrentTileY() - enemy.getCurrentTileY())) <= 1;
    }

    private static double distanceToPlayer(Enemy enemy, Player player) {
        return Math.hypot((double) player.getCurrentTileX() - enemy.getCurrentTileX(),
                (double) player.getCurrentTileY() - enemy.getCurrentTileY());
    }
}
