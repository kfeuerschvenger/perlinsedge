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

    private List<String> costTexts(BuildingType type) {
        return FxToolkitSupport.onFx(() -> panel.costLabels(type).stream().map(Label::getText).toList());
    }

    @Test
    void everyRecipeHasACardWithItsCost() {
        for (BuildingRecipe recipe : recipes.getAllBuildingRecipes()) {
            assertEquals(recipe.ingredients().size(), FxToolkitSupport.onFx(() -> panel.costLabels(recipe.buildingType()).size()));
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
        boolean reachedParent = press(BuildingType.DOOR);

        assertEquals(List.of(BuildingType.DOOR), chosen);
        assertFalse(reachedParent, "the press is consumed by the card");
        assertEquals(BuildingType.DOOR, FxToolkitSupport.onFx(panel::getShownSelected));
    }

    @Test
    void theSelectedCardKeepsItsBorderUntilAnotherPieceIsChosen() {
        update(BuildingType.WALL);
        assertTrue(FxToolkitSupport.onFx(() -> panel.cardNode(BuildingType.WALL).getStyleClass().contains(UiTheme.SLOT_SELECTED)));
        assertFalse(FxToolkitSupport.onFx(() -> panel.cardNode(BuildingType.DOOR).getStyleClass().contains(UiTheme.SLOT_SELECTED)));

        update(BuildingType.DOOR);

        assertFalse(FxToolkitSupport.onFx(() -> panel.cardNode(BuildingType.WALL).getStyleClass().contains(UiTheme.SLOT_SELECTED)));
        assertTrue(FxToolkitSupport.onFx(() -> panel.cardNode(BuildingType.DOOR).getStyleClass().contains(UiTheme.SLOT_SELECTED)));
    }

    @Test
    void droppingTheSelectionClearsEveryBorder() {
        update(BuildingType.WALL);

        update(null);

        assertNull(FxToolkitSupport.onFx(panel::getShownSelected));
        for (BuildingType type : BuildingType.values()) {
            assertFalse(FxToolkitSupport.onFx(() -> panel.cardNode(type).getStyleClass().contains(UiTheme.SLOT_SELECTED)), type.name());
        }
    }

    @Test
    void theCostShowsHeldOverNeededInGreenWhenEnoughAndRedWhenNot() {
        player.addItem(new Item(ItemType.STONE, 1));
        update(null);

        List<String> texts = costTexts(BuildingType.WALL);
        assertEquals(List.of("STONE 1/2"), texts);
        assertTrue(FxToolkitSupport.onFx(() -> panel.costLabels(BuildingType.WALL).get(0).getStyleClass().contains(UiTheme.TEXT_BAD)));

        player.addItem(new Item(ItemType.STONE, 1));
        update(null);

        assertEquals(List.of("STONE 2/2"), costTexts(BuildingType.WALL));
        assertTrue(FxToolkitSupport.onFx(() -> panel.costLabels(BuildingType.WALL).get(0).getStyleClass().contains(UiTheme.TEXT_GOOD)));
        assertFalse(FxToolkitSupport.onFx(() -> panel.costLabels(BuildingType.WALL).get(0).getStyleClass().contains(UiTheme.TEXT_BAD)));
    }

    @Test
    void eachIngredientOfAPieceIsColouredOnItsOwn() {
        player.addItem(new Item(ItemType.WOOD_PLANK, 5));
        update(null);

        List<Label> door = FxToolkitSupport.onFx(() -> panel.costLabels(BuildingType.DOOR));
        assertTrue(FxToolkitSupport.onFx(() -> door.get(0).getStyleClass().contains(UiTheme.TEXT_GOOD)), "planks 5/5");
        assertTrue(FxToolkitSupport.onFx(() -> door.get(1).getStyleClass().contains(UiTheme.TEXT_BAD)), "iron ingot 0/1");
    }
}
