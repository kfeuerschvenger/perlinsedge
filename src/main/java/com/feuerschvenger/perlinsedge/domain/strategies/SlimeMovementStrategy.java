package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.pathfinding.EnemyOccupancy;
import com.feuerschvenger.perlinsedge.domain.pathfinding.PathNode;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Implements the movement behavior for Slime enemies.
 * Slimes jump towards the player when aggroed or jump randomly when idle.
 *
 * <p>A hop is a segment from the position the slime is at to a target tile. The position follows a smoothstep of the
 * time elapsed over the segment duration, so the speed is continuous and the slime never waits on the target before
 * landing. The duration of a hop is latched when it starts and nothing changes it afterwards. Aiming the hop at another
 * tile in the air starts a new segment from the current position, so the position is continuous at any moment of the
 * hop.</p>
 */
public class SlimeMovementStrategy implements EnemyMovementStrategy {
    // Configuration constants (specific to Slime movement)
    private static final double IDLE_JUMP_INTERVAL_MIN = 2.0;
    private static final double IDLE_JUMP_INTERVAL_MAX = 4.0;
    private static final double JUMP_SPEED_AGGRO = 0.5; // Duration of jump
    private static final double JUMP_SPEED_IDLE = 0.8;  // Duration of jump
    public static final double JUMP_HEIGHT_FACTOR = 50.0; // Public for rendering
    /** Wait before searching again after the next step of the path turned out to be unusable. */
    private static final double REPLAN_WAIT_SECONDS = 0.15;
    /** Longest random addition to the first wait when the step was blocked by another enemy, so a crowd does not act in step. */
    private static final double HELD_WAIT_SPREAD_SECONDS = 0.3;
    /** Longest wait for a step another enemy holds: the wait doubles while the same step stays blocked, up to this. */
    private static final double MAX_HELD_WAIT_SECONDS = 1.5;
    /** How much longer each repeated wait for the same held step is. */
    private static final double HELD_WAIT_GROWTH_FACTOR = 2.0;
    /** First wait before a hovering slime looks for a tile to land on again, doubled up to the longest after each miss. */
    private static final double HOVER_RETRY_SECONDS = 0.1;
    private static final double MAX_HOVER_RETRY_SECONDS = 1.0;
    /** How much longer each failed look for a landing tile makes the wait. */
    private static final double HOVER_RETRY_GROWTH_FACTOR = 2.0;
    /** Marks that no step of the path is blocked. */
    private static final int NO_STEP = Integer.MIN_VALUE;
    /** Extra cost of a tile another enemy holds in a search: a detour up to this long is taken instead. */
    private static final double HELD_TILE_PENALTY = 6.0;
    /** Rest on the ground between two hops of a chase, shortest and longest. Each slime has one fixed value. */
    private static final double MIN_REST_SECONDS = 0.02;
    private static final double MAX_REST_SECONDS = 0.2;
    /** Shortest segment of a bent hop, as a fraction of the hop duration, so a short bend is not abrupt. */
    private static final double MIN_BEND_FRACTION = 0.3;
    /** Longest segment of a bent hop, as a fraction of the hop duration. */
    private static final double MAX_BEND_FRACTION = 2.0;

    // Movement directions
    private static final int[] DX = {0, 1, 0, -1, 1, -1, 1, -1};
    private static final int[] DY = {1, 0, -1, 0, 1, 1, -1, -1};

    // Movement state (now part of the strategy instance)
    private LinkedList<PathNode> currentPath;
    private double idleTimer;
    private boolean isJumping;
    private double jumpPhase; // 0.0 to 1.0 representing jump progress, drives the arc of the render
    /** Duration of the hop in the air, latched at take-off, in seconds. */
    private double hopDuration;
    /** Where the segment in progress started. */
    private double segmentStartX;
    private double segmentStartY;
    /** Time spent on the segment in progress and the time it lasts, in seconds. */
    private double segmentElapsed;
    private double segmentDuration;
    /** The jump phase the segment started at, so the arc of the render continues instead of restarting. */
    private double segmentStartPhase;
    /** True while the slime stays in the air because no tile around the take-off tile is free to land on. */
    private boolean hovering;
    /**
     * Time left before the path may be searched again or its next step tried again, after a step of it could not be
     * taken.
     */
    private double secondsOfReplanWait;
    /** The step of the path another enemy holds and the wait the slime last gave it, or {@link #NO_STEP}. */
    private int blockedStepX = NO_STEP;
    private int blockedStepY = NO_STEP;
    private double blockedWaitSeconds;
    /** Time left before a hovering slime looks for a tile to land on again, and the wait it gives the next miss. */
    private double secondsOfHoverWait;
    private double hoverRetrySeconds;
    /** How many times a hovering slime looked for a tile to land on, for the tests. */
    private int hoverChecks;
    /** The rest between hops of this slime, fixed at creation so neighbours hop out of step. */
    private final double restBetweenHops;
    /** Time left before the next hop of a chase may start. */
    private double secondsOfRest;
    /** Which enemy holds which tile, set on every update. */
    private EnemyOccupancy occupancy;

    private final Random random;
    private final PathPlanner pathPlanner = new PathPlanner();
    private final ChaseLeash leash = new ChaseLeash(AppConfig.getInstance().enemies().slime());

    /**
     * Creates a strategy with an empty path, on the ground, with its first idle hop scheduled. Its timing follows from
     * its arguments, so the same arguments always give the same rest and idle hops.
     *
     * @param seed         seed of the idle hops of the slime, such as a hash of the tile it spawned on
     * @param restFraction where the rest between hops falls between the shortest and the longest rest, from 0 up to
     *                     but not including 1
     */
    public SlimeMovementStrategy(long seed, double restFraction) {
        this.currentPath = new LinkedList<>();
        this.random = new Random(seed);
        this.restBetweenHops = MIN_REST_SECONDS + (MAX_REST_SECONDS - MIN_REST_SECONDS) * restFraction;
        resetIdleTimer();
        this.isJumping = false;
        this.jumpPhase = 0.0;
    }

    @Override
    public void updateMovement(Enemy enemy, double deltaTime, TileMap map, Player player, EnemyOccupancy occupancy) {
        this.occupancy = occupancy;
        // Slime-specific movement logic
        enemy.snapshot(); // Snapshot entity's position for interpolation
        pathPlanner.tick(deltaTime);
        secondsOfReplanWait = Math.max(0, secondsOfReplanWait - deltaTime);
        secondsOfHoverWait = Math.max(0, secondsOfHoverWait - deltaTime);
        secondsOfRest = Math.max(0, secondsOfRest - deltaTime);

        // Next to the player the slime holds its ground and strikes instead of hopping: a hop in the air finishes
        // first, then no new one starts until the player is out of reach again
        final boolean inReach = SlimeAttackStrategy.isInReach(enemy, player);
        // The leash advances every update, in reach or not, so the hold of an alert runs out while the slime strikes
        final boolean chasing = leash.update(distanceToPlayer(enemy, player), player.isDead(), deltaTime);
        if (inReach) {
            currentPath.clear();
            clearBlockedStep();
        } else if (chasing) {
            // The path is searched from the tile the slime is on, which is the take-off tile while it is in the air:
            // the search waits for the landing
            if (!isJumping) {
                handleAggroBehavior(enemy, map, player);
            }
        } else {
            // Without a chase the hops along a path to the player stop once the one in the air has landed
            currentPath.clear();
            clearBlockedStep();
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

    /** @return the duration latched for the hop in the air, in seconds, for the tests */
    double hopDuration() { return hopDuration; }

    /** @return true while the slime hangs in the air with no free tile to land on, for the tests */
    boolean isHovering() { return hovering; }

    /** @return the rest on the ground between two hops of a chase, in seconds, for the tests */
    double restBetweenHops() { return restBetweenHops; }

    /** @return the time left before the next idle hop is due, in seconds, for the tests */
    double secondsUntilIdleHop() { return idleTimer; }

    /** @return how many times a hovering slime looked for a tile to land on, for the tests */
    int hoverChecks() { return hoverChecks; }

    /** @return how many steps the path still has, for the tests */
    int pathLength() { return currentPath.size(); }

    /** Schedules the next idle hop a random time ahead, between the minimum and the maximum idle interval. */
    private void resetIdleTimer() {
        idleTimer = IDLE_JUMP_INTERVAL_MIN +
                (IDLE_JUMP_INTERVAL_MAX - IDLE_JUMP_INTERVAL_MIN) * random.nextDouble();
    }

    /** @return the distance in tiles between the tile of the enemy and the tile of the player */
    private static double distanceToPlayer(Enemy enemy, Player player) {
        return Math.hypot((double) player.getCurrentTileX() - enemy.getCurrentTileX(),
                (double) player.getCurrentTileY() - enemy.getCurrentTileY());
    }

    /**
     * Searches a path to the player when the one the slime has no longer leads there, and hops at random when none
     * is found. Called only on the ground: the search starts from the tile the slime is on.
     */
    private void handleAggroBehavior(Enemy enemy, TileMap map, Player player) {
        if (secondsOfReplanWait <= 0 && needsNewPath(player)) {
            // Capped, and skipped for a while after a failed search (the player may be unreachable)
            var pathToPlayer = pathPlanner.findPath(
                    map,
                    enemy.getCurrentTileX(), enemy.getCurrentTileY(),
                    player.getCurrentTileX(), player.getCurrentTileY(),
                    occupancy.penaltyFor(enemy, HELD_TILE_PENALTY)
            ).nodes();

            if (!pathToPlayer.isEmpty()) {
                currentPath = new LinkedList<>(pathToPlayer);
                removeCurrentPositionFromPath(enemy);
                forgetBlockedStepUnlessAhead();
            } else {
                moveRandomly(enemy, map, player, JUMP_SPEED_AGGRO); // Fallback to random move if no path
            }
        }
    }

    /**
     * The path is kept between hops while it still ends on the tile the player is on or stepping to, because each
     * step is checked again when it is taken. It is searched again when it is empty or the player has moved away from
     * its end.
     *
     * @return true when a new path has to be searched
     */
    private boolean needsNewPath(Player player) {
        return currentPath.isEmpty() || !isPlayerTile(currentPath.getLast().x, currentPath.getLast().y, player);
    }

    /** Drops the first node of the path when it is the tile the slime is already on. */
    private void removeCurrentPositionFromPath(Enemy enemy) {
        if (!currentPath.isEmpty() &&
                currentPath.getFirst().x == enemy.getCurrentTileX() &&
                currentPath.getFirst().y == enemy.getCurrentTileY()) {
            currentPath.removeFirst();
        }
    }

    /**
     * Counts the idle timer down and hops at random when it runs out. A hop that falls due while the slime is in the
     * air or resting after a landing is kept pending and taken once it can start, instead of being dropped.
     */
    private void handleIdleBehavior(Enemy enemy, TileMap map, Player player, double deltaTime) {
        idleTimer -= deltaTime;

        if (idleTimer <= 0 && !isJumping && secondsOfRest <= 0) {
            moveRandomly(enemy, map, player, JUMP_SPEED_IDLE);
            resetIdleTimer();
        }
    }

    /** Hops to a random free tile around the slime, with the given hop duration, unless it is already in the air. */
    private void moveRandomly(Enemy enemy, TileMap map, Player player, double duration) {
        if (isJumping || secondsOfRest > 0) {
            return;
        }

        List<int[]> possibleMoves = findPossibleMoves(enemy, map, player);
        if (!possibleMoves.isEmpty()) {
            int[] chosenMove = possibleMoves.get(random.nextInt(possibleMoves.size()));
            if (!occupancy.tryReserve(enemy, chosenMove[0], chosenMove[1])) {
                return;
            }
            enemy.setTargetTileX(chosenMove[0]); // Update enemy's target
            enemy.setTargetTileY(chosenMove[1]); // Update enemy's target
            startJump(enemy, duration);
        }
    }

    /**
     * @return the tiles around the slime it can hop to: free, not the player's, held by no other enemy, and with no
     *         corner cut
     */
    private List<int[]> findPossibleMoves(Enemy enemy, TileMap map, Player player) {
        List<int[]> moves = new ArrayList<>();

        for (var i = 0; i < DX.length; i++) {
            int nextX = enemy.getCurrentTileX() + DX[i];
            int nextY = enemy.getCurrentTileY() + DY[i];

            if (!isPlayerTile(nextX, nextY, player)
                    && occupancy.isFreeFor(enemy, nextX, nextY)
                    && canHop(map, enemy.getCurrentTileX(), enemy.getCurrentTileY(), nextX, nextY)) {
                moves.add(new int[]{nextX, nextY});
            }
        }
        return moves;
    }

    /** A hop lands on a free tile, and a diagonal hop may not cut the corner of a blocked tile. */
    private static boolean canHop(TileMap map, int fromX, int fromY, int toX, int toY) {
        if (!isValidMove(map, toX, toY)) {
            return false;
        }
        final boolean diagonal = toX != fromX && toY != fromY;
        return !diagonal || (isValidMove(map, toX, fromY) && isValidMove(map, fromX, toY));
    }

    /** @return true when the tile is inside the map and walkable by the rule the pathfinder uses */
    private static boolean isValidMove(TileMap map, int x, int y) {
        final boolean insideMap = x >= 0 && x < map.getWidth() && y >= 0 && y < map.getHeight();
        // The rule the pathfinder uses: water, resources and solid buildings block
        return insideMap && map.getTile(x, y).isWalkable();
    }

    /**
     * Takes off towards the target tile already set on the enemy. The duration is latched here and kept for the whole
     * hop, whatever the slime does meanwhile. The path is kept: a hop along a path to the player is one step of it.
     */
    private void startJump(Enemy enemy, double duration) {
        isJumping = true;
        hovering = false;
        hopDuration = duration;
        jumpPhase = 0;
        beginSegment(enemy, duration, 0);
    }

    /** Starts a segment from the position the enemy is at, lasting the given time, at the given jump phase. */
    private void beginSegment(Enemy enemy, double duration, double startPhase) {
        segmentStartX = enemy.getPosX();
        segmentStartY = enemy.getPosY();
        segmentElapsed = 0;
        segmentDuration = duration;
        segmentStartPhase = startPhase;
    }

    /**
     * Advances the hop in the air. A target that has become the tile of the player, or that cannot be landed on any
     * more, is replaced before the position moves, so the slime never lands on it. The position is a smoothstep from
     * the segment start to the target, and the hop ends when the segment does.
     */
    private void updateJump(Enemy enemy, double deltaTime, TileMap map, Player player) {
        if (hovering ? secondsOfHoverWait <= 0 : !isHopTargetValid(enemy, map, player)) {
            retargetHop(enemy, map, player);
        }
        if (hovering) {
            return;
        }

        segmentElapsed += deltaTime;
        final double t = Math.min(1.0, segmentElapsed / segmentDuration);
        final double eased = t * t * (3.0 - 2.0 * t);
        enemy.setPosX(segmentStartX + (enemy.getTargetTileX() - segmentStartX) * eased);
        enemy.setPosY(segmentStartY + (enemy.getTargetTileY() - segmentStartY) * eased);
        jumpPhase = segmentStartPhase + (1.0 - segmentStartPhase) * t;

        if (t >= 1.0) {
            completeJump(enemy);
            secondsOfRest = restBetweenHops;
        }
    }

    /**
     * @return true when the hop target is free, is not the player's, is held by no other enemy and does not cut a
     *         corner from the take-off tile
     */
    private boolean isHopTargetValid(Enemy enemy, TileMap map, Player player) {
        return !isPlayerTile(enemy.getTargetTileX(), enemy.getTargetTileY(), player)
                && occupancy.isFreeFor(enemy, enemy.getTargetTileX(), enemy.getTargetTileY())
                && canHop(map, enemy.getCurrentTileX(), enemy.getCurrentTileY(),
                enemy.getTargetTileX(), enemy.getTargetTileY());
    }

    /**
     * Aims the hop at the valid tile nearest to the slime among the take-off tile and its neighbours, as a new
     * segment from the current position. Candidates are the same tiles a hop from the take-off tile could have been
     * aimed at, so the landing is never more than a tile from the take-off tile, and each is checked as a first hop
     * would be: walkable, not the player's, and no blocked corner from the take-off tile. The segment lasts in
     * proportion to the distance it covers, within bounds, and the jump phase carries on from where it was.
     * When no candidate is valid, the slime hangs where it is, with the phase held, and looks again after a wait that
     * doubles from a tenth of a second up to a second while it stays stuck. The tile it held as its target is let go
     * at once, so only the take-off tile stays held and another enemy may use the rest; the target is the take-off
     * tile meanwhile. A slime whose take-off tile has become the player's and whose neighbours are all held therefore
     * hangs there, checking at most once a second, and lands back on the take-off tile as soon as the player leaves it.
     * The path to the player is dropped, because the hop no longer goes where it was searched for.
     */
    private void retargetHop(Enemy enemy, TileMap map, Player player) {
        if (hovering) {
            hoverChecks++;
        }
        final int fromX = enemy.getCurrentTileX();
        final int fromY = enemy.getCurrentTileY();
        var bestX = 0;
        var bestY = 0;
        double bestDistance = Double.MAX_VALUE;
        var found = false;
        // The take-off tile comes first as the zero offset, the neighbours follow
        for (int i = -1; i < DX.length; i++) {
            final int x = fromX + (i < 0 ? 0 : DX[i]);
            final int y = fromY + (i < 0 ? 0 : DY[i]);
            if (!canLandOn(enemy, map, player, x, y)) {
                continue;
            }
            final double distance = Math.hypot(x - enemy.getPosX(), y - enemy.getPosY());
            if (distance < bestDistance) {
                bestDistance = distance;
                bestX = x;
                bestY = y;
                found = true;
            }
        }
        currentPath.clear();
        // The new target is reserved and the old one let go: the take-off tile stays held either way
        if (!found || !occupancy.tryReserve(enemy, bestX, bestY)) {
            hover(enemy, fromX, fromY);
            return;
        }
        hovering = false;
        hoverRetrySeconds = 0;
        enemy.setTargetTileX(bestX);
        enemy.setTargetTileY(bestY);
        final double duration = Math.clamp(hopDuration * bestDistance,
                MIN_BEND_FRACTION * hopDuration, MAX_BEND_FRACTION * hopDuration);
        beginSegment(enemy, duration, jumpPhase);
    }

    /** Tells whether the enemy may hop from its take-off tile to a tile: not the player's, held by no other enemy, and walkable. */
    private boolean canLandOn(Enemy enemy, TileMap map, Player player, int x, int y) {
        return !isPlayerTile(x, y, player) && occupancy.isFreeFor(enemy, x, y)
                && canHop(map, enemy.getCurrentTileX(), enemy.getCurrentTileY(), x, y);
    }

    /**
     * Hangs in the air with the target moved back to the take-off tile, so the tile that was the target is no longer
     * held, and schedules the next look for a tile to land on.
     */
    private void hover(Enemy enemy, int fromX, int fromY) {
        hovering = true;
        occupancy.tryReserve(enemy, fromX, fromY);
        enemy.setTargetTileX(fromX);
        enemy.setTargetTileY(fromY);
        hoverRetrySeconds = hoverRetrySeconds > 0
                ? Math.min(MAX_HOVER_RETRY_SECONDS, hoverRetrySeconds * HOVER_RETRY_GROWTH_FACTOR)
                : HOVER_RETRY_SECONDS;
        secondsOfHoverWait = hoverRetrySeconds;
    }

    /**
     * Lands the hop on its target, which the update has just checked is free and not the player's, and puts the
     * position exactly on the tile.
     */
    private void completeJump(Enemy enemy) {
        isJumping = false;
        hovering = false;
        jumpPhase = 0;
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

    /** @return true when the slime is on the ground, has a step of the path to take and is not waiting on a held one */
    private boolean shouldStartJump() {
        return !currentPath.isEmpty() && !isJumping && secondsOfRest <= 0 && secondsOfReplanWait <= 0;
    }

    /** Forgets the blocked step and the wait it earned, once the slime moves on or the step is no longer ahead. */
    private void clearBlockedStep() {
        blockedStepX = NO_STEP;
        blockedStepY = NO_STEP;
        blockedWaitSeconds = 0;
    }

    /** Keeps the memory of a blocked step only while a new path still starts with it. */
    private void forgetBlockedStepUnlessAhead() {
        if (currentPath.isEmpty() || currentPath.getFirst().x != blockedStepX || currentPath.getFirst().y != blockedStepY) {
            clearBlockedStep();
        }
    }

    /**
     * Waits for a step that another enemy holds, with no search meanwhile. The first wait is random, between the replan
     * wait and the replan wait plus its spread, so a crowd does not act in step; the wait doubles, up to
     * {@link #MAX_HELD_WAIT_SECONDS}, each time the same step is still held when it is tried again.
     *
     * @return true when the step was already held after an earlier wait, which means its holder is not passing through
     */
    private boolean waitForHeldStep(PathNode step) {
        final boolean repeated = step.x == blockedStepX && step.y == blockedStepY;
        if (repeated) {
            blockedWaitSeconds = Math.min(MAX_HELD_WAIT_SECONDS, blockedWaitSeconds * HELD_WAIT_GROWTH_FACTOR);
        } else {
            blockedStepX = step.x;
            blockedStepY = step.y;
            blockedWaitSeconds = REPLAN_WAIT_SECONDS + HELD_WAIT_SPREAD_SECONDS * random.nextDouble();
        }
        secondsOfReplanWait = blockedWaitSeconds;
        return repeated;
    }

    /**
     * Takes the next step of the path as a hop. The step is checked first: something may have been built on the tile
     * since the path was searched, the player may have stepped on it, or the slime may be on a tile other than the one
     * the path was searched from, so the step is not a hop of one tile. A step that fails clears the path and makes the
     * slime wait before searching again. A tile another enemy holds does not fail the path at once: the slime keeps it
     * and waits for the tile to be let go without searching. When the same step is still held after the wait, the
     * path is dropped and searched again after a wait that doubles while the step stays held, see
     * {@link #waitForHeldStep(PathNode)}. The tile of a step that goes ahead is reserved before the slime takes off.
     */
    private void startNextJump(Enemy enemy, TileMap map, Player player) {
        final PathNode nextStep = currentPath.getFirst();
        final boolean adjacent = Math.abs(nextStep.x - enemy.getCurrentTileX()) <= 1
                && Math.abs(nextStep.y - enemy.getCurrentTileY()) <= 1;
        if (!adjacent
                || !canHop(map, enemy.getCurrentTileX(), enemy.getCurrentTileY(), nextStep.x, nextStep.y)
                || isPlayerTile(nextStep.x, nextStep.y, player)) {
            currentPath.clear();
            clearBlockedStep();
            // Wait before searching again instead of searching on every update meanwhile, which the player's step
            // would otherwise cause
            secondsOfReplanWait = REPLAN_WAIT_SECONDS;
            return;
        }
        if (!occupancy.tryReserve(enemy, nextStep.x, nextStep.y)) {
            if (waitForHeldStep(nextStep)) {
                // Still held after a wait: the holder stays, so the path is dropped and searched again once the
                // longer wait is over, priced to go around the held tiles when a detour is cheap
                currentPath.clear();
            }
            return;
        }
        currentPath.removeFirst();
        clearBlockedStep();
        enemy.setTargetTileX(nextStep.x);
        enemy.setTargetTileY(nextStep.y);

        if (!enemy.reachedPathTarget()) {
            startJump(enemy, JUMP_SPEED_AGGRO);
        } else {
            // If the path just has the current tile (e.g., path of 1), just complete it immediately
            completeJump(enemy);
        }
    }

}
