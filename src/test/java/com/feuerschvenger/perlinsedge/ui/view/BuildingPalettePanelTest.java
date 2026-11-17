package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.crafting.BuildingRecipe;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import javafx.event.Event;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildingPalettePanelTest {

    private final RecipeManager recipes = new RecipeManager();
    private final List<BuildingType> chosen = new ArrayList<>();
    private Player player;
    private BuildingPalettePanel panel;

    @BeforeEach
    void setUp() {
        player = new Player(0, 0);
        panel = FxToolkitSupport.onFx(() -> {
            BuildingPalettePanel created = new BuildingPalettePanel(recipes);
            created.setOnBuildingSelected(chosen::add);
            return created;
        });
    }

    private void update(BuildingType selected) {
        FxToolkitSupport.onFx(() -> {
            panel.update(player, selected);
            return null;
        });
    }

    /** Fires a press on a card, and tells whether the panel above it still saw the press bubble up. */
    private boolean press(BuildingType type) {
        return FxToolkitSupport.onFx(() -> {
            boolean[] bubbled = {false};
            panel.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> bubbled[0] = true);
            Event.fireEvent(panel.cardNode(type), new MouseEvent(MouseEvent.MOUSE_PRESSED, 5, 5, 5, 5,
                    MouseButton.PRIMARY, 1, false, false, false, false, true, false, false, true, false, false, null));
            return bubbled[0];
        });
    }

    @Test
    void everyRecipeHasACard() {
        for (BuildingRecipe recipe : recipes.getAllBuildingRecipes()) {
            assertNotNull(FxToolkitSupport.onFx(() -> panel.cardNode(recipe.buildingType())));
        }
    }

    @Test
    void thePaletteIsHiddenUntilItIsShown() {
        assertFalse(FxToolkitSupport.onFx(panel::isVisible));

        FxToolkitSupport.onFx(() -> {
            panel.showPalette();
            return null;
        });
        assertTrue(FxToolkitSupport.onFx(panel::isVisible));

        FxToolkitSupport.onFx(() -> {
            panel.hidePalette();
            return null;
        });
        assertFalse(FxToolkitSupport.onFx(panel::isVisible));
    }

    @Test
    void clickingACardChoosesThePieceAndTheClickNeverReachesTheWorld() {
        boolean reachedParent = press(BuildingType.CHEST);

        assertEquals(List.of(BuildingType.CHEST), chosen);
        assertFalse(reachedParent, "the press is consumed by the card");
        assertEquals(BuildingType.CHEST, FxToolkitSupport.onFx(panel::getShownSelected));
    }

    @Test
    void theSelectedCardKeepsItsBorderUntilAnotherPieceIsChosen() {
        update(BuildingType.FENCE);
        assertTrue(FxToolkitSupport.onFx(() -> panel.cardNode(BuildingType.FENCE).getStyleClass().contains(UiTheme.SLOT_SELECTED)));
        assertFalse(FxToolkitSupport.onFx(() -> panel.cardNode(BuildingType.CHEST).getStyleClass().contains(UiTheme.SLOT_SELECTED)));

        update(BuildingType.CHEST);

        assertFalse(FxToolkitSupport.onFx(() -> panel.cardNode(BuildingType.FENCE).getStyleClass().contains(UiTheme.SLOT_SELECTED)));
        assertTrue(FxToolkitSupport.onFx(() -> panel.cardNode(BuildingType.CHEST).getStyleClass().contains(UiTheme.SLOT_SELECTED)));
    }

    @Test
    void droppingTheSelectionClearsEveryBorder() {
        update(BuildingType.FENCE);

        update(null);

        assertNull(FxToolkitSupport.onFx(panel::getShownSelected));
        for (BuildingType type : BuildingType.values()) {
            assertFalse(FxToolkitSupport.onFx(() -> panel.cardNode(type).getStyleClass().contains(UiTheme.SLOT_SELECTED)), type.name());
        }
    }

    @Test
    void aCardHoldsOnlyTheIconAndTheNameAndTheRotateHint() {
        for (BuildingRecipe recipe : recipes.getAllBuildingRecipes()) {
            final BuildingType type = recipe.buildingType();
            final int expected = type.isRotatable() ? 3 : 2;
            assertEquals(expected, FxToolkitSupport.onFx(() -> panel.cardNode(type).getChildren().size()), type.name());
        }
    }

    @Test
    void aCardThePlayerCannotAffordIsGreyedAndGoesBackToNormalWhenItCan() {
        update(null);

        assertFalse(FxToolkitSupport.onFx(() -> panel.isAffordable(BuildingType.FENCE)));
        assertTrue(FxToolkitSupport.onFx(() -> panel.cardNode(BuildingType.FENCE).getStyleClass().contains(UiTheme.CELL_UNAVAILABLE)));
        assertNotNull(FxToolkitSupport.onFx(() -> panel.iconOf(BuildingType.FENCE).getEffect()), "the icon is greyscale");

        player.addItem(new Item(ItemType.WOOD_LOG, 2));
        update(null);

        assertTrue(FxToolkitSupport.onFx(() -> panel.isAffordable(BuildingType.FENCE)));
        assertFalse(FxToolkitSupport.onFx(() -> panel.cardNode(BuildingType.FENCE).getStyleClass().contains(UiTheme.CELL_UNAVAILABLE)));
        assertNull(FxToolkitSupport.onFx(() -> panel.iconOf(BuildingType.FENCE).getEffect()));
        assertEquals(1.0, FxToolkitSupport.onFx(() -> panel.iconOf(BuildingType.FENCE).getOpacity()));
    }

    @Test
    void aPieceIsAffordableOnlyWhenEveryIngredientIsHeld() {
        player.addItem(new Item(ItemType.WOOD_PLANK, 5));
        update(null);

        assertFalse(FxToolkitSupport.onFx(() -> panel.isAffordable(BuildingType.BED)), "fiber is missing");

        player.addItem(new Item(ItemType.FIBER, 5));
        update(null);

        assertTrue(FxToolkitSupport.onFx(() -> panel.isAffordable(BuildingType.BED)));
    }

    @Test
    void theNameWrapsToATwoLineCellInsteadOfCuttingOff() {
        for (BuildingRecipe recipe : recipes.getAllBuildingRecipes()) {
            final BuildingType type = recipe.buildingType();
            final Label name = FxToolkitSupport.onFx(() -> panel.nameOf(type));
            assertTrue(FxToolkitSupport.onFx(name::isWrapText), type.name());
            assertEquals(javafx.scene.text.TextAlignment.CENTER, FxToolkitSupport.onFx(name::getTextAlignment), type.name());
            assertEquals(BuildingPalettePanel.NAME_HEIGHT, FxToolkitSupport.onFx(name::getMinHeight), type.name());
            assertEquals(BuildingPalettePanel.NAME_HEIGHT, FxToolkitSupport.onFx(name::getMaxHeight), type.name());
        }
    }

    @Test
    void thePaletteDrivesTheCostPanelItWasGiven() {
        BuildingCostPanel cost = FxToolkitSupport.onFx(() -> {
            BuildingCostPanel created = new BuildingCostPanel(recipes);
            panel.setCostPanel(created);
            return created;
        });
        assertFalse(FxToolkitSupport.onFx(cost::isVisible));

        FxToolkitSupport.onFx(() -> {
            panel.showPalette();
            return null;
        });
        update(BuildingType.FENCE);
        assertTrue(FxToolkitSupport.onFx(cost::isVisible));

        FxToolkitSupport.onFx(() -> {
            panel.hidePalette();
            return null;
        });
        assertFalse(FxToolkitSupport.onFx(cost::isVisible));
    }

    @Test
    void theRotateHintShowsOnlyOnTheSelectedCardOfAPieceThatCanTurn() {
        update(BuildingType.CHEST);
        assertTrue(FxToolkitSupport.onFx(() -> panel.isRotateHintShown(BuildingType.CHEST)));
        assertFalse(FxToolkitSupport.onFx(() -> panel.isRotateHintShown(BuildingType.BED)), "not selected");

        update(BuildingType.CAMPFIRE);
        assertFalse(FxToolkitSupport.onFx(() -> panel.isRotateHintShown(BuildingType.CHEST)));
        assertFalse(FxToolkitSupport.onFx(() -> panel.isRotateHintShown(BuildingType.CAMPFIRE)), "it cannot turn");

        update(null);
        assertFalse(FxToolkitSupport.onFx(() -> panel.isRotateHintShown(BuildingType.CHEST)));
    }

    @Test
    void theCardsShowTheRotationTheyAreGiven() {
        final java.util.List<BuildingType> asked = new ArrayList<>();
        FxToolkitSupport.onFx(() -> {
            panel.update(player, BuildingType.BED, type -> {
                asked.add(type);
                return type == BuildingType.BED ? 2 : 0;
            });
            return null;
        });

        assertTrue(asked.containsAll(List.of(BuildingType.values())), "every card is asked for its rotation");
    }
}
