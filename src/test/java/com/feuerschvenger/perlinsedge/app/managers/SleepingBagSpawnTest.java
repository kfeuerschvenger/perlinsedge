package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingFactory;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.SleepingBag;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import com.feuerschvenger.perlinsedge.ui.view.FxToolkitSupport;
import com.feuerschvenger.perlinsedge.ui.view.HealthBar;
import com.feuerschvenger.perlinsedge.ui.view.InventoryPanel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A sleeping bag becomes the spawn point when clicked, and is used up by the respawn. */
class SleepingBagSpawnTest {

    private static final int SIZE = 21;
    private static final int FIRST_X = 5;
    private static final int FIRST_Y = 6;
    private static final int SECOND_X = 9;
    private static final int SECOND_Y = 3;
    private static final int PLAYER_START = 15;

    private GameStateManager state;
    private BuildingManager buildings;
    private PlayerManager players;
    private TileMap map;
    private Player player;
    private final List<String> hints = new ArrayList<>();

    @BeforeEach
    void setUp() {
        state = new GameStateManager();
        buildings = new BuildingManager(state, new RecipeManager(), new ContainerManager());
        buildings.setHintSink(hints::add);
        players = FxToolkitSupport.onFx(() -> new PlayerManager(state, new HealthBar(), new InventoryPanel(state)));
        map = new TileMap(SIZE, SIZE);
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.GRASS));
            }
        }
        player = new Player(PLAYER_START, PLAYER_START);
        state.setCurrentMap(map);
        state.setPlayer(player);
    }

    private SleepingBag bagAt(int x, int y) {
        final var bag = (SleepingBag) BuildingFactory.create(BuildingType.SLEEPING_BAG, x, y);
        map.getTile(x, y).setBuilding(bag);
        return bag;
    }

    private void respawn() {
        FxToolkitSupport.onFx(() -> {
            players.respawnPlayer();
            return null;
        });
    }

    @Test
    void clickingABagMakesItTheSpawnPointAndSaysSo() {
        final var bag = bagAt(FIRST_X, FIRST_Y);

        assertTrue(buildings.interactWithBuilding(FIRST_X, FIRST_Y));

        assertSame(bag, state.getSpawnBag());
        assertEquals(List.of("Spawn point set"), hints);
    }

    @Test
    void clickingAnotherBagMovesTheSpawnPoint() {
        bagAt(FIRST_X, FIRST_Y);
        final var second = bagAt(SECOND_X, SECOND_Y);
        buildings.interactWithBuilding(FIRST_X, FIRST_Y);

        buildings.interactWithBuilding(SECOND_X, SECOND_Y);

        assertSame(second, state.getSpawnBag());
    }

    @Test
    void theRespawnStandsOnTheActiveBagAndUsesItUpWithNoRefund() {
        bagAt(FIRST_X, FIRST_Y);
        buildings.interactWithBuilding(FIRST_X, FIRST_Y);

        respawn();

        assertEquals(FIRST_X, player.getCurrentTileX());
        assertEquals(FIRST_Y, player.getCurrentTileY());
        assertFalse(map.getTile(FIRST_X, FIRST_Y).hasBuilding(), "the bag is gone");
        assertNull(state.getSpawnBag());
        assertTrue(player.getInventory().stream().allMatch(item -> item == null || item.getQuantity() == 0));
    }

    @Test
    void onlyTheActiveBagBreaksAndTheOthersStayUnclaimed() {
        bagAt(FIRST_X, FIRST_Y);
        final var other = bagAt(SECOND_X, SECOND_Y);
        buildings.interactWithBuilding(FIRST_X, FIRST_Y);

        respawn();

        assertSame(other, map.getTile(SECOND_X, SECOND_Y).getBuilding());
        assertNull(state.getSpawnBag());
    }

    @Test
    void withoutABagTheRespawnFallsBackToASafeTile() {
        respawn();

        assertTrue(map.getTile(player.getCurrentTileX(), player.getCurrentTileY()).isWalkable());
    }

    @Test
    void demolishingTheActiveBagClearsTheSpawnPoint() {
        bagAt(FIRST_X, FIRST_Y);
        buildings.interactWithBuilding(FIRST_X, FIRST_Y);

        buildings.removeBuilding(FIRST_X, FIRST_Y);

        assertNull(state.getSpawnBag());
    }

    @Test
    void demolishingAnotherBagKeepsTheSpawnPoint() {
        final var active = bagAt(FIRST_X, FIRST_Y);
        bagAt(SECOND_X, SECOND_Y);
        buildings.interactWithBuilding(FIRST_X, FIRST_Y);

        buildings.removeBuilding(SECOND_X, SECOND_Y);

        assertSame(active, state.getSpawnBag());
    }

    @Test
    void aBagThatIsNoLongerOnItsTileIsIgnoredAndForgotten() {
        bagAt(FIRST_X, FIRST_Y);
        buildings.interactWithBuilding(FIRST_X, FIRST_Y);
        map.getTile(FIRST_X, FIRST_Y).setBuilding(null);

        respawn();

        assertNull(state.getSpawnBag());
        assertTrue(map.getTile(player.getCurrentTileX(), player.getCurrentTileY()).isWalkable());
    }

    @Test
    void aResetForgetsTheSpawnPoint() {
        bagAt(FIRST_X, FIRST_Y);
        buildings.interactWithBuilding(FIRST_X, FIRST_Y);

        state.resetGameState();

        assertNull(state.getSpawnBag());
    }
}
