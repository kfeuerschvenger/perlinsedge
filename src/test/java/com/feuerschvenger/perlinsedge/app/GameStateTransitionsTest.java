package com.feuerschvenger.perlinsedge.app;

import com.feuerschvenger.perlinsedge.app.managers.BuildingManager;
import com.feuerschvenger.perlinsedge.app.managers.ContainerManager;
import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.app.managers.HotbarManager;
import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.utils.GameMode;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.ui.view.BuildingHotbarPanel;
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

    @BeforeEach
    void setUp() {
        RecipeManager recipes = new RecipeManager();
        state = new GameStateManager();
        state.setPlayer(new Player(1, 1));
        ContainerManager containers = new ContainerManager();
        BuildingManager buildings = new BuildingManager(state, recipes, containers);
        transitions = FxToolkitSupport.onFx(() -> {
            BuildingHotbarPanel hotbarPanel = new BuildingHotbarPanel(recipes);
            return new GameStateTransitions(state, buildings, containers,
                    new HotbarManager(buildings, state, hotbarPanel), new InventoryPanel(state),
                    new MapConfigPanel(MapType.DESERT, 1L), recipes, new GameLoop(dt -> { }, (alpha, dt) -> { }));
        });
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

        onFx(transitions::toggleBuildingMode);

        assertEquals(GameMode.EXPLORATION, state.getCurrentGameMode());
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
