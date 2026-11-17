package com.feuerschvenger.perlinsedge.app;

import com.feuerschvenger.perlinsedge.app.managers.BuildingManager;
import com.feuerschvenger.perlinsedge.app.managers.ContainerManager;
import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.domain.utils.GameMode;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.ui.view.BuildingPalettePanel;
import com.feuerschvenger.perlinsedge.ui.view.FxToolkitSupport;
import com.feuerschvenger.perlinsedge.ui.view.InventoryPanel;
import com.feuerschvenger.perlinsedge.ui.view.MapConfigPanel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameStateTransitionsTest {

    private GameStateManager state;
    private GameStateTransitions transitions;
    private BuildingManager buildings;
    private BuildingPalettePanel palette;

    @BeforeEach
    void setUp() {
        RecipeManager recipes = new RecipeManager();
        state = new GameStateManager();
        state.setPlayer(new Player(1, 1));
        ContainerManager containers = new ContainerManager();
        buildings = new BuildingManager(state, recipes, containers);
        transitions = FxToolkitSupport.onFx(() -> {
            palette = new BuildingPalettePanel(recipes);
            return new GameStateTransitions(state, buildings, containers, palette, new InventoryPanel(state),
                    new MapConfigPanel(MapType.DESERT, 1L), recipes, new GameLoop(dt -> { }, (alpha, dt) -> { }));
        });
    }

    private void holdHammerInSlot(int slot) {
        state.getPlayer().setItemAt(slot, new Item(ItemType.HAMMER, 1));
        state.getPlayer().selectHotbarSlot(slot);
    }

    private void onFx(Runnable action) {
        FxToolkitSupport.onFx(() -> {
            action.run();
            return null;
        });
    }

    @Test
    void theHookRunsAfterTheGameIsPausedSoTheFrameUnderTheMenuIsDrawnPaused() {
        List<Boolean> pausedWhenCalled = new ArrayList<>();
        transitions.setOnPaused(() -> pausedWhenCalled.add(state.isGamePaused()));

        onFx(transitions::togglePauseMenu);

        assertEquals(List.of(true), pausedWhenCalled);
        assertEquals(GameMode.PAUSED, state.getCurrentGameMode());
    }

    @Test
    void openingTheInventoryWithNoPlayerDoesNothing() {
        state.setPlayer(null);

        onFx(transitions::toggleInventory);
        onFx(() -> transitions.setGameMode(GameMode.INVENTORY_OPEN));

        assertEquals(GameMode.EXPLORATION, state.getCurrentGameMode());
        assertFalse(state.isInventoryOpen());
    }

    @Test
    void buildingModeWithNoPlayerIsIgnored() {
        state.setPlayer(null);

        onFx(transitions::syncBuildModeWithHand);

        assertEquals(GameMode.EXPLORATION, state.getCurrentGameMode());
    }

    @Test
    void holdingTheHammerEntersBuildModeAndShowsThePalette() {
        holdHammerInSlot(2);

        onFx(transitions::syncBuildModeWithHand);

        assertEquals(GameMode.BUILDING_MODE, state.getCurrentGameMode());
        assertTrue(buildings.isBuildingModeActive());
        assertTrue(FxToolkitSupport.onFx(palette::isVisible));
    }

    @Test
    void selectingAnotherSlotLeavesBuildModeAndHidesThePalette() {
        holdHammerInSlot(2);
        onFx(transitions::syncBuildModeWithHand);

        state.getPlayer().selectHotbarSlot(0);
        onFx(transitions::syncBuildModeWithHand);

        assertEquals(GameMode.EXPLORATION, state.getCurrentGameMode());
        assertFalse(buildings.isBuildingModeActive());
        assertFalse(FxToolkitSupport.onFx(palette::isVisible));
    }

    @Test
    void selectingTheHammerAgainEntersBuildModeAgain() {
        holdHammerInSlot(2);
        onFx(transitions::syncBuildModeWithHand);
        state.getPlayer().selectHotbarSlot(0);
        onFx(transitions::syncBuildModeWithHand);

        state.getPlayer().selectHotbarSlot(2);
        onFx(transitions::syncBuildModeWithHand);

        assertEquals(GameMode.BUILDING_MODE, state.getCurrentGameMode());
    }

    @Test
    void theHammerLeavingTheHandSlotLeavesBuildMode() {
        holdHammerInSlot(2);
        onFx(transitions::syncBuildModeWithHand);

        state.getPlayer().setItemAt(2, null);
        onFx(transitions::syncBuildModeWithHand);

        assertEquals(GameMode.EXPLORATION, state.getCurrentGameMode());
    }

    @Test
    void buildModeIsNotEnteredWhileTheGameIsPaused() {
        holdHammerInSlot(2);
        onFx(transitions::togglePauseMenu);

        onFx(transitions::syncBuildModeWithHand);

        assertEquals(GameMode.PAUSED, state.getCurrentGameMode());
        assertFalse(buildings.isBuildingModeActive());
    }

    @Test
    void buildModeIsNotEnteredWhileThePlayerIsDead() {
        holdHammerInSlot(2);
        state.getPlayer().takeDamage(10_000);
        assertTrue(state.isPlayerDead());

        onFx(transitions::syncBuildModeWithHand);

        assertEquals(GameMode.EXPLORATION, state.getCurrentGameMode());
    }

    @Test
    void openingTheInventoryClosesThePaletteAndClosingItBringsBuildModeBack() {
        holdHammerInSlot(2);
        onFx(transitions::syncBuildModeWithHand);

        onFx(transitions::toggleInventory);
        assertEquals(GameMode.INVENTORY_OPEN, state.getCurrentGameMode());
        assertFalse(FxToolkitSupport.onFx(palette::isVisible));
        onFx(transitions::syncBuildModeWithHand);
        assertEquals(GameMode.INVENTORY_OPEN, state.getCurrentGameMode());

        onFx(transitions::toggleInventory);
        onFx(transitions::syncBuildModeWithHand);
        assertEquals(GameMode.BUILDING_MODE, state.getCurrentGameMode());
    }

    @Test
    void thePieceChosenLastIsSelectedAgainWhenTheHammerComesBack() {
        holdHammerInSlot(2);
        onFx(transitions::syncBuildModeWithHand);
        buildings.setSelectedBuildingType(BuildingType.BED);

        state.getPlayer().selectHotbarSlot(0);
        onFx(transitions::syncBuildModeWithHand);
        assertEquals(null, buildings.getSelectedBuildingType());
        state.getPlayer().selectHotbarSlot(2);
        onFx(transitions::syncBuildModeWithHand);

        assertEquals(BuildingType.BED, buildings.getSelectedBuildingType());
    }

    @Test
    void goingBackToExplorationStillWorksWithNoPlayer() {
        onFx(transitions::toggleInventory);
        assertEquals(GameMode.INVENTORY_OPEN, state.getCurrentGameMode());
        state.setPlayer(null);

        onFx(() -> transitions.setGameMode(GameMode.EXPLORATION));

        assertEquals(GameMode.EXPLORATION, state.getCurrentGameMode());
    }

    @Test
    void resumingDoesNotRunThePauseHook() {
        List<String> calls = new ArrayList<>();
        transitions.setOnPaused(() -> calls.add("paused"));

        onFx(transitions::togglePauseMenu);
        onFx(transitions::togglePauseMenu);

        assertEquals(List.of("paused"), calls);
        assertTrue(!state.isGamePaused());
    }
}
