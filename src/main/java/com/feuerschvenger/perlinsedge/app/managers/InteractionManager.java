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
 * Within reach the interaction happens at once. Out of reach the player first walks to the nearest walkable tile next to
 * the target and the interaction happens once, on arrival. The walk is a pending interaction that a new click, a move
 * order, the death of the player, a change of game mode or the target disappearing cancels.
 */
@Slf4j
public class InteractionManager {

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
     * @param containerManager tells whether an interaction opened a container
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
     * @return true when the order was accepted, the interaction done or the walk started; false when there is nothing
     *         to interact with, the tool is missing or the target cannot be reached
     */
    public boolean interact(int tileX, int tileY) {
        cancel();
        final Player player = gameState.getPlayer();
        final TileMap map = gameState.getCurrentMap();
        if (player == null || map == null || player.isDead()) {
            return false;
        }
        final Tile tile = map.getTile(tileX, tileY);
        final Kind kind = kindOf(tile);
        if (kind == Kind.NONE) {
            return false;
        }
        if (kind == Kind.RESOURCE && !resourceManager.hasToolFor(player, tile)) {
            return false;
        }

        final List<PathNode> walk = InteractionPlanner.pathToReach(
                map, player.getCurrentTileX(), player.getCurrentTileY(), tileX, tileY);
        if (walk == null) {
            hintSink.accept("You can't reach that");
            return false;
        }
        if (walk.isEmpty()) {
            return perform(player, tile, kind);
        }
        if (!player.walkAlong(walk)) {
            return false;
        }
        pending = new Pending(tileX, tileY, kind);
        log.debug("Walking to {} at ({}, {})", kind, tileX, tileY);
        return true;
    }

    /** Advances a pending interaction by one game tick: performs it on arrival, or cancels it when it can no longer happen. */
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
            pending = null;
            perform(player, tile, kind);
        } else if (!player.isMoving()) {
            // The walk ended short of the target, for example because something blocked the way
            cancel();
        }
    }

    /** Forgets the pending interaction. The walk that was started is left to the player. */
    public void cancel() {
        pending = null;
    }

    /** @return true while the player is walking to an interaction */
    public boolean hasPending() {
        return pending != null;
    }

    private boolean perform(Player player, Tile tile, Kind kind) {
        if (kind == Kind.BUILDING) {
            final boolean interacted = buildingManager.interactWithBuilding(tile.getX(), tile.getY());
            if (interacted && containerManager.isContainerOpen()) {
                onContainerOpened.run();
            }
            return interacted;
        }
        return resourceManager.handleTileInteraction(player, tile);
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
