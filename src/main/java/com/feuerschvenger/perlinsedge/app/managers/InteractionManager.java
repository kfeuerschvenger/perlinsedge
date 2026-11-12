package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Building;
import com.feuerschvenger.perlinsedge.domain.pathfinding.InteractionPlanner;
import com.feuerschvenger.perlinsedge.domain.pathfinding.PathNode;
import com.feuerschvenger.perlinsedge.domain.utils.GameMode;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.Consumer;

/**
 * Carries out the interaction the player asks for with a click on a resource or on a building such as a chest or a door.
 * Within reach the interaction happens at once. Out of reach the player first walks toward the nearest walkable tile
 * next to the target and the interaction happens once, on arrival, which is the first moment the player is within
 * reach: the walk ends there instead of going on to the stand tile. A target hit too recently to be hit again keeps
 * the interaction pending until the hit lands. The walk is a pending interaction that a new click, a move order, a
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
    private enum Kind { NONE, BUILDING, RESOURCE }

    /** The interaction the player is walking to. */
    private record Pending(int tileX, int tileY, Kind kind) { }

    private final GameStateManager gameState;
    private final BuildingManager buildingManager;
    private final ResourceManager resourceManager;
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
     * @param containerManager  tells whether an interaction opened a container
     * @param onContainerOpened called after an interaction opened a container, it shows the container
     * @param hintSink          receives the short message shown when the target cannot be reached
     */
    public InteractionManager(GameStateManager gameState, BuildingManager buildingManager,
                              ResourceManager resourceManager, ContainerManager containerManager,
                              Runnable onContainerOpened, Consumer<String> hintSink) {
        this.gameState = gameState;
        this.buildingManager = buildingManager;
        this.resourceManager = resourceManager;
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
     *         the target was hit too recently for another hit to land, {@link Outcome#REFUSED} when there is nothing to
     *         interact with, the tool is missing or the target cannot be reached
     */
    public Outcome interact(int tileX, int tileY) {
        cancel();
        final Player player = gameState.getPlayer();
        final TileMap map = gameState.getCurrentMap();
        if (player == null || map == null || player.isDead()) {
            return Outcome.REFUSED;
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
        final List<PathNode> walk = InteractionPlanner.pathToReach(
                map, player.getWalkOriginX(), player.getWalkOriginY(), tileX, tileY);
        if (walk == null) {
            hintSink.accept("You can't reach that");
            return Outcome.REFUSED;
        }
        if (walk.isEmpty()) {
            // A walk left over from an earlier order would carry the player away from the target
            player.stop();
            if (InteractionPlanner.inReach(player.getCurrentTileX(), player.getCurrentTileY(), tileX, tileY)) {
                return perform(player, tile, kind);
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

    /** @return true while the player is walking to an interaction */
    public boolean hasPending() {
        return pending != null;
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
