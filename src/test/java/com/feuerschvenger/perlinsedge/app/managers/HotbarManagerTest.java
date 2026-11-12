package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import com.feuerschvenger.perlinsedge.domain.utils.GameMode;
import com.feuerschvenger.perlinsedge.ui.view.BuildingHotbarPanel;
import com.feuerschvenger.perlinsedge.ui.view.FxToolkitSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The page that is showing is read through the building behind slot 1, which differs from page to page.
 */
class HotbarManagerTest {

    private GameStateManager state;
    private BuildingManager buildings;
    private HotbarManager hotbar;
    private BuildingType firstOfPageOne;
    private BuildingType firstOfPageTwo;

    @BeforeEach
    void setUp() {
        RecipeManager recipes = new RecipeManager();
        state = new GameStateManager();
        buildings = new BuildingManager(state, recipes, new ContainerManager());
        BuildingHotbarPanel panel = FxToolkitSupport.onFx(() -> new BuildingHotbarPanel(recipes));
        hotbar = new HotbarManager(buildings, state, panel);
        firstOfPageOne = recipes.getAllBuildingRecipes().get(0).buildingType();
        firstOfPageTwo = recipes.getAllBuildingRecipes().get(9).buildingType();
        state.setCurrentGameMode(GameMode.BUILDING_MODE);
    }

    private BuildingType buildingInSlotOne() {
        FxToolkitSupport.onFx(() -> {
            hotbar.handleHotbarNumberSelection(1);
            return null;
        });
        return buildings.getSelectedBuildingType();
    }

    private boolean wheel(double deltaY, boolean ctrl) {
        return FxToolkitSupport.onFx(() -> hotbar.handleWheel(deltaY, ctrl));
    }

    @Test
    void aNotchOfWheelFlipsOnePageAndTheEventBelongsToTheHotbar() {
        assertEquals(firstOfPageOne, buildingInSlotOne());

        assertTrue(wheel(-40, false), "the wheel event must not zoom the camera");

        assertEquals(firstOfPageTwo, buildingInSlotOne());
    }

    @Test
    void aSmallDeltaIsConsumedButDoesNotFlipThePageYet() {
        assertTrue(wheel(-15, false));

        assertEquals(firstOfPageOne, buildingInSlotOne());
    }

    @Test
    void twoNotchesInOneEventFlipOnlyOnePageAndTheRestWaitsForTheNextEvent() {
        assertTrue(wheel(-80, false));

        assertEquals(firstOfPageTwo, buildingInSlotOne(), "an event flips at most one page");

        assertTrue(wheel(-1, false));

        assertEquals(firstOfPageOne, buildingInSlotOne(), "the second notch is paid out by the next event");
    }

    @Test
    void theWheelBelongsToTheCameraWhenCtrlIsHeld() {
        assertFalse(wheel(-40, true));

        assertEquals(firstOfPageOne, buildingInSlotOne());
    }

    @Test
    void theWheelBelongsToTheCameraOutsideBuildingMode() {
        state.setCurrentGameMode(GameMode.EXPLORATION);

        assertFalse(wheel(-40, false));
    }

    @Test
    void ctrlDropsTheHalfTurnedNotchSoItDoesNotCompleteLater() {
        wheel(-30, false);
        wheel(-30, true);
        wheel(-30, false);

        assertEquals(firstOfPageOne, buildingInSlotOne(), "the 30 units before the Ctrl event were dropped");
    }
}
