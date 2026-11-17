package com.feuerschvenger.perlinsedge.app;

import com.feuerschvenger.perlinsedge.app.managers.BuildingManager;
import com.feuerschvenger.perlinsedge.domain.utils.ClickFeedback;
import com.feuerschvenger.perlinsedge.app.managers.CombatManager;
import com.feuerschvenger.perlinsedge.app.managers.ContainerManager;
import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.app.managers.InteractionManager;
import com.feuerschvenger.perlinsedge.app.managers.PlayerManager;
import com.feuerschvenger.perlinsedge.app.managers.ResourceManager;
import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.domain.utils.GameMode;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Slime;
import com.feuerschvenger.perlinsedge.domain.world.model.Resource;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.FxCamera;
import com.feuerschvenger.perlinsedge.ui.view.FxMinimapDrawer;
import javafx.event.Event;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Clicks on the game canvas become orders, and every order leaves a green or red flash on the clicked tile.
 * The camera sits at the origin with zoom 1, so tile x,y has its centre at screen ((x - y) * 32, (x + y) * 16 + 16).
 */
class InputWiringClickTest {

    private static final int SIZE = 12;

    private GameStateManager state;
    private TileMap map;
    private Player player;
    private InteractionManager interactions;
    private Canvas canvas;
    private RecipeManager recipes;
    private BuildingManager buildings;

    @BeforeEach
    void setUp() {
        state = new GameStateManager();
        map = new TileMap(SIZE, SIZE);
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                map.setTile(x, y, new Tile(x, y, x == 9 ? TileType.DEEP_WATER : TileType.GRASS));
            }
        }
        player = new Player(2, 2);
        state.setCurrentMap(map);
        state.setPlayer(player);

        canvas = new Canvas(800, 600);
        Canvas minimapCanvas = new Canvas(100, 60);
        FxCamera camera = new FxCamera(0, 0, 1.0);
        ContainerManager containers = new ContainerManager();
        recipes = new RecipeManager();
        buildings = new BuildingManager(state, recipes, containers);
        interactions = new InteractionManager(state, buildings, new ResourceManager(state), new CombatManager(state),
                containers, () -> { }, message -> { });
        InputWiring wiring = new InputWiring(state, buildings, new PlayerManager(state, null, null), interactions,
                camera, null, new FxMinimapDrawer(minimapCanvas, camera, canvas), canvas);
        wiring.registerMouseHandlers();
    }

    private void fire(javafx.event.EventType<MouseEvent> type, double x, double y, MouseButton button) {
        Event.fireEvent(canvas, new MouseEvent(type, x, y, x, y, button, 1,
                false, false, false, false, button == MouseButton.PRIMARY, false, button == MouseButton.SECONDARY,
                true, false, false, null));
    }

    /** Presses a button over the centre of a tile, which is all a click needs. */
    private void click(int tileX, int tileY, MouseButton button) {
        fire(MouseEvent.MOUSE_PRESSED, (tileX - tileY) * 32.0, (tileX + tileY) * 16.0 + 16.0, button);
    }

    private ClickFeedback.Kind flash() {
        return state.getClickFeedback().kind(state.getGameTime());
    }

    @Test
    void aMoveOrderToAWalkableTileFlashesGreenOnThatTile() {
        click(5, 2, MouseButton.SECONDARY);

        assertEquals(ClickFeedback.Kind.ACCEPTED, flash());
        assertEquals(5, state.getClickFeedback().getTileX());
        assertEquals(2, state.getClickFeedback().getTileY());
        assertTrue(player.isMoving());
    }

    @Test
    void aMoveOrderToWaterFlashesRed() {
        click(9, 4, MouseButton.SECONDARY);

        assertEquals(ClickFeedback.Kind.REJECTED, flash());
        assertFalse(player.isMoving());
    }

    @Test
    void aMoveOrderToTheTileUnderThePlayerFlashesRed() {
        click(2, 2, MouseButton.SECONDARY);

        assertEquals(ClickFeedback.Kind.REJECTED, flash());
    }

    @Test
    void anInteractionWithNothingFlashesRed() {
        click(4, 4, MouseButton.PRIMARY);

        assertEquals(ClickFeedback.Kind.REJECTED, flash());
    }

    @Test
    void aLeftClickOnASlimeInReachAttacksItAndFlashesGreen() {
        final Slime slime = new Slime(3, 2);
        state.getEnemies().add(slime);

        click(3, 2, MouseButton.PRIMARY);

        assertEquals(ClickFeedback.Kind.ACCEPTED, flash());
        assertTrue(slime.getHealth() < slime.getMaxHealth());
    }

    @Test
    void aLeftClickOnAFarSlimeFlashesGreenAndWalksToIt() {
        state.getEnemies().add(new Slime(7, 2));

        click(7, 2, MouseButton.PRIMARY);

        assertEquals(ClickFeedback.Kind.ACCEPTED, flash());
        assertTrue(interactions.hasPending());
        assertTrue(player.isMoving());
    }

    @Test
    void aRightClickOnASlimeFlashesRedAndDoesNotWalk() {
        state.getEnemies().add(new Slime(7, 2));

        click(7, 2, MouseButton.SECONDARY);

        assertEquals(ClickFeedback.Kind.REJECTED, flash());
        assertFalse(player.isMoving());
    }

    @Test
    void aRightClickCancelsThePendingAttack() {
        state.getEnemies().add(new Slime(7, 2));
        click(7, 2, MouseButton.PRIMARY);

        click(2, 6, MouseButton.SECONDARY);

        assertFalse(interactions.hasPending());
    }

    @Test
    void aFarResourceFlashesGreenAndStartsTheWalk() {
        map.getTile(7, 2).setWorldResource(new Resource(ResourceType.STONE, 1));

        click(7, 2, MouseButton.PRIMARY);

        assertEquals(ClickFeedback.Kind.ACCEPTED, flash());
        assertTrue(interactions.hasPending());
        assertTrue(player.isMoving());
    }

    @Test
    void aResourceInReachFlashesGreenWithoutWalking() {
        map.getTile(3, 2).setWorldResource(new Resource(ResourceType.STONE, 1));

        click(3, 2, MouseButton.PRIMARY);

        assertEquals(ClickFeedback.Kind.ACCEPTED, flash());
        assertFalse(interactions.hasPending());
        assertFalse(player.isMoving());
    }

    @Test
    void aClickOnAResourceDuringTheHitCooldownDoesNotFlash() {
        map.getTile(3, 2).setWorldResource(new Resource(ResourceType.STONE, 3));
        click(3, 2, MouseButton.PRIMARY);
        state.getClickFeedback().clear();

        click(3, 2, MouseButton.PRIMARY);

        assertEquals(ClickFeedback.Kind.NONE, flash(), "no green for a click that had no effect, no red for a valid one");
    }

    @Test
    void aMoveOrderCancelsThePendingInteraction() {
        map.getTile(7, 2).setWorldResource(new Resource(ResourceType.STONE, 1));
        click(7, 2, MouseButton.PRIMARY);

        click(2, 6, MouseButton.SECONDARY);

        assertFalse(interactions.hasPending());
    }

    @Test
    void anOrderDoesNotWaitForTheReleaseOrTheClickEvent() {
        // JavaFX never delivers the click to the canvas when the pointer is over another node at the release, so the
        // order must already have happened at the press
        click(5, 2, MouseButton.SECONDARY);

        assertTrue(player.isMoving(), "the press alone started the walk");
    }

    @Test
    void aClickEventOnItsOwnDoesNothing() {
        fire(MouseEvent.MOUSE_CLICKED, (5 - 2) * 32.0, (5 + 2) * 16.0 + 16.0, MouseButton.SECONDARY);

        assertFalse(player.isMoving());
        assertEquals(ClickFeedback.Kind.NONE, flash(), "the press was the order, the click after it is not a second one");
    }

    @Test
    void aMouseMovedBetweenPressAndReleaseStillActsOnThePressedTile() {
        fire(MouseEvent.MOUSE_PRESSED, (5 - 2) * 32.0, (5 + 2) * 16.0 + 16.0, MouseButton.SECONDARY);
        fire(MouseEvent.MOUSE_RELEASED, 300, 300, MouseButton.SECONDARY);

        assertEquals(5, state.getClickFeedback().getTileX());
        assertEquals(2, state.getClickFeedback().getTileY());
        assertTrue(player.isMoving());
    }

    @Test
    void theFlashEndsOnItsOwn() {
        click(5, 2, MouseButton.SECONDARY);

        assertEquals(ClickFeedback.Kind.NONE,
                state.getClickFeedback().kind(state.getGameTime() + ClickFeedback.DURATION_SECONDS + 0.01));
    }

    private void buildMode(BuildingType selected) {
        state.setCurrentGameMode(GameMode.BUILDING_MODE);
        buildings.activateBuildingMode();
        buildings.setSelectedBuildingType(selected);
    }

    @Test
    void aLeftClickInBuildModePlacesTheSelectedPieceAndFlashesGreen() {
        player.addItem(new Item(ItemType.WOOD_LOG, 2));
        buildMode(BuildingType.FENCE);

        click(5, 2, MouseButton.PRIMARY);

        assertTrue(map.getTile(5, 2).hasBuilding());
        assertEquals(ClickFeedback.Kind.ACCEPTED, flash());
    }

    @Test
    void aLeftClickThatCannotPlaceFlashesRedAndPlacesNothing() {
        buildMode(BuildingType.FENCE);

        click(5, 2, MouseButton.PRIMARY);

        assertFalse(map.getTile(5, 2).hasBuilding());
        assertEquals(ClickFeedback.Kind.REJECTED, flash());
    }

    @Test
    void aLeftClickWithNoPieceSelectedDoesNothingAtAll() {
        player.addItem(new Item(ItemType.WOOD_LOG, 2));
        buildMode(null);

        click(5, 2, MouseButton.PRIMARY);

        assertFalse(map.getTile(5, 2).hasBuilding());
        assertEquals(ClickFeedback.Kind.NONE, flash());
        assertEquals(2, player.getItemCount(new Item(ItemType.WOOD_LOG, 1)));
    }

    @Test
    void aRightClickInBuildModeDemolishesAndRefunds() {
        player.addItem(new Item(ItemType.WOOD_LOG, 2));
        buildMode(BuildingType.FENCE);
        click(5, 2, MouseButton.PRIMARY);
        assertEquals(0, player.getItemCount(new Item(ItemType.WOOD_LOG, 1)));

        click(5, 2, MouseButton.SECONDARY);

        assertFalse(map.getTile(5, 2).hasBuilding());
        assertEquals(2, player.getItemCount(new Item(ItemType.WOOD_LOG, 1)));
        assertEquals(ClickFeedback.Kind.ACCEPTED, flash());
    }

    @Test
    void aRightClickOnAnEmptyTileInBuildModeFlashesRed() {
        buildMode(BuildingType.FENCE);

        click(5, 2, MouseButton.SECONDARY);

        assertEquals(ClickFeedback.Kind.REJECTED, flash());
    }
}
