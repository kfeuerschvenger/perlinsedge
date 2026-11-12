package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Building;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.pathfinding.InteractionPlanner;
import com.feuerschvenger.perlinsedge.domain.pathfinding.OccupiedTiles;
import com.feuerschvenger.perlinsedge.domain.pathfinding.PathNode;
import com.feuerschvenger.perlinsedge.domain.utils.GameMode;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.Consumer;

/**
 * Carries out the interaction the player asks for with a click on a resource, on a building such as a chest or a door,
 * or on an enemy, which is attacked with the item in hand. An enemy is followed while it moves: the walk is planned
 * again whenever it has changed tile, and the swing lands the first time it is within reach.
 * Within reach the interaction happens at once. Out of reach the player first walks toward the nearest walkable tile
 * next to the target and the interaction happens once, on arrival, which is the first moment the player is within
 * reach: the walk ends there instead of going on to the stand tile. A target hit too recently to be hit again keeps
 * the interaction pending until the hit lands, whether the player arrives during the cooldown or was already in reach
 * when it clicked. The walk is a pending interaction that a new click, a move order, a
 * pause, the death of the player, a change of game mode or the target disappearing cancels, and cancelling ends the
 * walk too, the player stopping on the tile it was stepping to.
 */
@Slf4j
public class InteractionManager {

    /** What came of a click. */
    public enum Outcome {
        /** The interaction was done or the walk to it started. */
        ACCEPTED,
        /** The click was valid but had no effect, because the target was hit too recently; it earns no feedback. */
        IGNORED,
        /** The click was refused. */
        REFUSED;

        /** @return true when the click earns the accepted flash */
        public boolean isAccepted() {
            return this == ACCEPTED;
        }
    }

    /** What a click on a tile does. */
    private enum Kind { NONE, BUILDING, RESOURCE, ENEMY }

    /**
     * The interaction the player is walking to. For an enemy the tile is the one the enemy stood on when the walk was
     * planned, and the enemy itself is the target; for anything else the enemy is null.
     */
    private record Pending(int tileX, int tileY, Kind kind, Enemy enemy) {
        Pending(int tileX, int tileY, Kind kind) {
            this(tileX, tileY, kind, null);
        }
    }

    private final GameStateManager gameState;
    private final BuildingManager buildingManager;
    private final ResourceManager resourceManager;
    private final CombatManager combatManager;
    private final ContainerManager containerManager;
    private final Runnable onContainerOpened;
    private final Consumer<String> hintSink;

    private Pending pending;

    /**
     * Creates the manager.
     *
     * @param gameState         shared game state
     * @param buildingManager   opens chests and toggles doors
     * @param resourceManager   hits resources
     * @param combatManager     swings at enemies
     * @param containerManager  tells whether an interaction opened a container
     * @param onContainerOpened called after an interaction opened a container, it shows the container
     * @param hintSink          receives the short message shown when the target cannot be reached
     */
    public InteractionManager(GameStateManager gameState, BuildingManager buildingManager,
                              ResourceManager resourceManager, CombatManager combatManager,
                              ContainerManager containerManager,
                              Runnable onContainerOpened, Consumer<String> hintSink) {
        this.gameState = gameState;
        this.buildingManager = buildingManager;
        this.resourceManager = resourceManager;
        this.combatManager = combatManager;
        this.containerManager = containerManager;
        this.onContainerOpened = onContainerOpened;
        this.hintSink = hintSink;
    }

    /**
     * Handles a click on a tile: interacts at once when the player is in reach, otherwise starts the walk.
     * Any interaction that was pending is cancelled first.
     *
     * @param tileX tile column
     * @param tileY tile row
     * @return {@link Outcome#ACCEPTED} when the interaction was done or the walk started, {@link Outcome#IGNORED} when
     *         the target was hit too recently for another hit to land, which leaves the interaction pending until it
     *         can, {@link Outcome#REFUSED} when there is nothing to interact with, the tool is missing or the target
     *         cannot be reached
     */
    public Outcome interact(int tileX, int tileY) {
        cancel();
        final Player player = gameState.getPlayer();
        final TileMap map = gameState.getCurrentMap();
        if (player == null || map == null || player.isDead()) {
            return Outcome.REFUSED;
        }
        // An enemy on the tile is the target, ahead of whatever else the tile holds
        final Enemy enemy = enemyAt(tileX, tileY);
        if (enemy != null) {
            return attack(player, map, enemy);
        }
        final Tile tile = map.getTile(tileX, tileY);
        final Kind kind = kindOf(tile);
        if (kind == Kind.NONE) {
            return Outcome.REFUSED;
        }
        if (kind == Kind.RESOURCE && !resourceManager.hasToolFor(player, tile)) {
            resourceManager.hintMissingTool(tile);
            return Outcome.REFUSED;
        }

        // A player between two tiles finishes that step first, so the walk is planned from the tile it is approaching
        final PathNode origin = player.getWalkOrigin();
        final List<PathNode> walk = InteractionPlanner.pathToReach(map, origin.x, origin.y, tileX, tileY,
                OccupiedTiles.of(gameState.getEnemies()));
        if (walk == null) {
            hintSink.accept("You can't reach that");
            return Outcome.REFUSED;
        }
        if (walk.isEmpty()) {
            // A walk left over from an earlier order would carry the player away from the target
            player.stop();
            if (InteractionPlanner.inReach(player.getCurrentTileX(), player.getCurrentTileY(), tileX, tileY)) {
                final Outcome outcome = perform(player, tile, kind);
                if (outcome == Outcome.IGNORED) {
                    // The target was hit a moment ago: the interaction waits and lands when the cooldown ends, as it
                    // does for a player that arrives during the cooldown
                    pending = new Pending(tileX, tileY, kind);
                    log.debug("Waiting for {} at ({}, {}) to be hittable again", kind, tileX, tileY);
                }
                return outcome;
            }
            // The step in progress ends next to the target: the interaction happens on arrival
        } else if (!player.walkAlong(walk)) {
            return Outcome.REFUSED;
        }
        pending = new Pending(tileX, tileY, kind);
        log.debug("Walking to {} at ({}, {})", kind, tileX, tileY);
        return Outcome.ACCEPTED;
    }

    /**
     * Advances a pending interaction by one game tick: performs it and ends the walk the first time the player is in
     * reach, or cancels it when it can no longer happen.
     */
    public void update() {
        if (pending == null) {
            return;
        }
        final Player player = gameState.getPlayer();
        final TileMap map = gameState.getCurrentMap();
        if (player == null || map == null || player.isDead()
                || gameState.isGamePaused() || gameState.getCurrentGameMode() != GameMode.EXPLORATION) {
            cancel();
            return;
        }
        if (pending.kind() == Kind.ENEMY) {
            updateAttack(player, map);
            return;
        }
        final Tile tile = map.getTile(pending.tileX(), pending.tileY());
        final Kind kind = kindOf(tile);
        if (kind != pending.kind()) {
            cancel();
            return;
        }
        if (InteractionPlanner.inReach(player.getCurrentTileX(), player.getCurrentTileY(),
                pending.tileX(), pending.tileY())) {
            // The remaining steps only lead to the stand tile, which the interaction does not need
            player.stop();
            final Outcome outcome = perform(player, tile, kind);
            if (outcome == Outcome.IGNORED) {
                // The target was hit a moment ago: the interaction stays pending and lands when the cooldown ends
                return;
            }
            pending = null;
            if (outcome == Outcome.REFUSED) {
                gameState.getClickFeedback().flash(tile.getX(), tile.getY(), false, gameState.getGameTime());
            }
        } else if (!player.isMoving()) {
            // The walk ended short of the target, for example because something blocked the way
            cancel();
        }
    }

    /**
     * Forgets the pending interaction and ends the walk that was started for it, so a player never keeps walking to a
     * target it no longer has. Does nothing when no interaction is pending, which leaves a plain move order alone.
     */
    public void cancel() {
        if (pending == null) {
            return;
        }
        pending = null;
        final Player player = gameState.getPlayer();
        if (player != null) {
            player.stop();
        }
    }

    /**
     * Tells whether an interaction is waiting, either because the player is walking to it or because the target was
     * hit too recently. It exists so the tests, which drive the game ticks by hand, can tell when the wait is over; the
     * game itself never asks.
     *
     * @return true while an interaction is pending
     */
    public boolean hasPending() {
        return pending != null;
    }

    /**
     * Starts an attack on an enemy: the swing happens at once when the enemy is within reach, otherwise the player
     * walks toward it. A swing that must wait for the cooldown stays pending and lands when the cooldown ends.
     */
    private Outcome attack(Player player, TileMap map, Enemy enemy) {
        final int enemyX = enemy.getCurrentTileX();
        final int enemyY = enemy.getCurrentTileY();
        if (InteractionPlanner.inReach(player.getCurrentTileX(), player.getCurrentTileY(), enemyX, enemyY)) {
            // A walk left over from an earlier order would carry the player away from the enemy
            player.stop();
            return switch (combatManager.strikeEnemy(enemy)) {
                case HIT -> Outcome.ACCEPTED;
                case COOLDOWN -> {
                    pending = new Pending(enemyX, enemyY, Kind.ENEMY, enemy);
                    log.debug("Waiting to swing at the enemy on ({}, {})", enemyX, enemyY);
                    yield Outcome.IGNORED;
                }
                case REFUSED -> Outcome.REFUSED;
            };
        }
        if (!chase(player, map, enemy)) {
            return Outcome.REFUSED;
        }
        log.debug("Walking to the enemy on ({}, {})", enemyX, enemyY);
        return Outcome.ACCEPTED;
    }

    /**
     * Plans the walk toward an enemy from where the player is heading and makes it the pending interaction. The walk
     * goes around the enemies and ends on a free tile next to the target.
     *
     * @return false when there is no way to a tile next to the enemy, after hinting so
     */
    private boolean chase(Player player, TileMap map, Enemy enemy) {
        final int enemyX = enemy.getCurrentTileX();
        final int enemyY = enemy.getCurrentTileY();
        // A player between two tiles finishes that step first, so the walk is planned from the tile it is approaching
        final PathNode origin = player.getWalkOrigin();
        final List<PathNode> walk = InteractionPlanner.pathToReach(map, origin.x, origin.y, enemyX, enemyY,
                OccupiedTiles.of(gameState.getEnemies()));
        if (walk == null) {
            hintSink.accept("You can't reach that");
            return false;
        }
        if (!walk.isEmpty() && !player.walkAlong(walk)) {
            return false;
        }
        pending = new Pending(enemyX, enemyY, Kind.ENEMY, enemy);
        return true;
    }

    /**
     * Advances a pending attack: swings once the enemy is within reach, and while it is not, follows it, planning the
     * walk again whenever the enemy has moved to another tile or the walk has ended. The attack ends when the enemy
     * is gone or cannot be reached.
     */
    private void updateAttack(Player player, TileMap map) {
        final Enemy enemy = pending.enemy();
        if (enemy.isDead() || !gameState.getEnemies().contains(enemy)) {
            cancel();
            return;
        }
        final int enemyX = enemy.getCurrentTileX();
        final int enemyY = enemy.getCurrentTileY();
        if (InteractionPlanner.inReach(player.getCurrentTileX(), player.getCurrentTileY(), enemyX, enemyY)) {
            player.stop();
            switch (combatManager.strikeEnemy(enemy)) {
                case HIT -> pending = null;
                case COOLDOWN -> pending = new Pending(enemyX, enemyY, Kind.ENEMY, enemy);
                case REFUSED -> {
                    pending = null;
                    gameState.getClickFeedback().flash(enemyX, enemyY, false, gameState.getGameTime());
                }
            }
            return;
        }
        if (enemyX != pending.tileX() || enemyY != pending.tileY() || !player.isMoving()) {
            if (!chase(player, map, enemy)) {
                cancel();
                gameState.getClickFeedback().flash(enemyX, enemyY, false, gameState.getGameTime());
            }
        }
    }

    /** Finds the living enemy that a click on a tile points at: the one standing on it or drawn over it. */
    private Enemy enemyAt(int tileX, int tileY) {
        for (Enemy enemy : gameState.getEnemies()) {
            if (enemy.isDead()) {
                continue;
            }
            final boolean standsThere = enemy.getCurrentTileX() == tileX && enemy.getCurrentTileY() == tileY;
            final boolean drawnThere = Math.round(enemy.getPosX()) == tileX && Math.round(enemy.getPosY()) == tileY;
            if (standsThere || drawnThere) {
                return enemy;
            }
        }
        return null;
    }

    private Outcome perform(Player player, Tile tile, Kind kind) {
        if (kind == Kind.BUILDING) {
            final boolean interacted = buildingManager.interactWithBuilding(tile.getX(), tile.getY());
            if (interacted && containerManager.isContainerOpen()) {
                onContainerOpened.run();
            }
            return interacted ? Outcome.ACCEPTED : Outcome.REFUSED;
        }
        return switch (resourceManager.handleTileInteraction(player, tile)) {
            case HIT -> Outcome.ACCEPTED;
            case COOLDOWN -> Outcome.IGNORED;
            case REFUSED -> Outcome.REFUSED;
        };
    }

    /** What a click on a tile would interact with, a building taking precedence over a resource. */
    private static Kind kindOf(Tile tile) {
        if (tile == null) {
            return Kind.NONE;
        }
        final Building building = tile.getBuilding();
        if (building != null && building.isInteractable()) {
            return Kind.BUILDING;
        }
        return tile.hasResource() ? Kind.RESOURCE : Kind.NONE;
    }
}
