package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.domain.entities.buildings.ProcessingStation;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

/**
 * Lets time pass for the processing stations on the map and puts what they make on the ground. A product drops on the
 * tile in front of the station. When that tile cannot hold an item, such as a solid piece or a resource, the pile
 * goes to the next free edge neighbour, turning clockwise from the front, and to the tile of the station when the
 * four are blocked.
 * The stations are the ones in the {@link GameStateManager}, which clears them with the world.
 */
public class StationManager {
    /** The edges a tile shares with its neighbours. */
    private static final int EDGES = 4;

    private final GameStateManager gameState;
    private final ItemDropManager itemDropManager;

    /**
     * Creates the manager.
     *
     * @param gameState       shared game state, which holds the stations
     * @param itemDropManager drops the products
     */
    public StationManager(GameStateManager gameState, ItemDropManager itemDropManager) {
        this.gameState = gameState;
        this.itemDropManager = itemDropManager;
    }

    /**
     * Advances every station and drops what they finished.
     *
     * @param deltaTime seconds that passed on the game clock
     */
    public void update(double deltaTime) {
        final var map = gameState.getCurrentMap();
        if (map == null) {
            return;
        }
        for (ProcessingStation station : gameState.getStations()) {
            station.advance(deltaTime);
            for (Item product : station.drainProducts()) {
                dropInFront(map, station, product);
            }
        }
    }

    private void dropInFront(TileMap map, ProcessingStation station, Item product) {
        for (var turns = 0; turns < EDGES; turns++) {
            final var offset = station.frontOffset(turns);
            final var x = station.getTileX() + offset[0];
            final var y = station.getTileY() + offset[1];
            final var tile = map.getTile(x, y);
            if (tile != null && tile.isWalkable()) {
                itemDropManager.dropAt(product, x, y);
                return;
            }
        }
        itemDropManager.dropAt(product, station.getTileX(), station.getTileY());
    }
}
