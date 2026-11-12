package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemHotbarPanelTest {

    private Player player;
    private ItemHotbarPanel panel;

    @BeforeEach
    void setUp() {
        player = new Player(0, 0);
        panel = FxToolkitSupport.onFx(ItemHotbarPanel::new);
    }

    private void update() {
        FxToolkitSupport.onFx(() -> {
            panel.update(player);
            return null;
        });
    }

    private boolean hasIcon(int slot) {
        return FxToolkitSupport.onFx(() -> panel.slotNode(slot).getChildren().stream().anyMatch(ImageView.class::isInstance));
    }

    private String countText(int slot) {
        return FxToolkitSupport.onFx(() -> panel.slotNode(slot).getChildren().stream()
                .filter(Label.class::isInstance)
                .map(Label.class::cast)
                .filter(label -> label.getStyleClass().contains(UiTheme.COUNT))
                .map(Label::getText)
                .findFirst()
                .orElse(""));
    }

    @Test
    void thePanelHasNineNumberedSlotsOfTheBuildingHotbarSize() {
        assertEquals(Player.HOTBAR_SIZE, panel.getChildren().size());
        assertEquals(BuildingHotbarPanel.SLOT_SIZE, FxToolkitSupport.onFx(() -> panel.slotNode(0).getPrefWidth()));
    }

    @Test
    void aSlotShowsTheIconAndTheCountOfItsStack() {
        player.setItemAt(2, new Item(ItemType.WOOD_LOG, 12));
        player.setItemAt(3, new Item(ItemType.AXE, 1));

        update();

        assertTrue(hasIcon(2));
        assertEquals("12", countText(2));
        assertTrue(hasIcon(3));
        assertEquals("", countText(3), "a single item shows no count");
        assertFalse(hasIcon(4));
        assertTrue(FxToolkitSupport.onFx(() -> panel.slotNode(4).getStyleClass().contains(UiTheme.SLOT_EMPTY)));
    }

    @Test
    void anIconIsDrawnWithoutSmoothingAtAWholeMultipleOfTheArt() {
        player.setItemAt(0, new Item(ItemType.STONE, 1));

        update();

        ImageView icon = FxToolkitSupport.onFx(() -> panel.slotNode(0).getChildren().stream()
                .filter(ImageView.class::isInstance).map(ImageView.class::cast).findFirst().orElseThrow());
        assertFalse(icon.isSmooth());
        assertEquals(0, icon.getFitWidth() % 16, "32 px is the 16 px art at 2x");
    }

    @Test
    void theSelectedSlotHasTheSelectionBorderAndNoOtherDoes() {
        player.selectHotbarSlot(4);

        update();

        for (int i = 0; i < Player.HOTBAR_SIZE; i++) {
            final int slot = i;
            assertEquals(i == 4, FxToolkitSupport.onFx(() -> panel.slotNode(slot).getStyleClass()
                    .contains(UiTheme.SLOT_SELECTED)), "slot " + i);
        }
        assertEquals(4, panel.getShownSelectedIndex());
    }

    @Test
    void theSelectionFollowsTheHandAndTheItemsFollowThePlayer() {
        player.setItemAt(1, new Item(ItemType.PICKAXE, 1));
        player.selectHotbarSlot(1);
        update();
        assertTrue(hasIcon(1));

        player.removeItemFromSlot(1);
        player.selectHotbarSlot(6);
        update();

        assertFalse(hasIcon(1));
        assertEquals(6, panel.getShownSelectedIndex());
        assertFalse(FxToolkitSupport.onFx(() -> panel.slotNode(1).getStyleClass().contains(UiTheme.SLOT_SELECTED)));
    }

    @Test
    void aPlayerWithNoItemsShowsEmptySlotsAndWithoutAPlayerNothingIsSelected() {
        player.setItemAt(0, new Item(ItemType.STONE, 3));
        update();

        FxToolkitSupport.onFx(() -> {
            panel.update(null);
            return null;
        });

        assertFalse(hasIcon(0));
        assertEquals(-1, panel.getShownSelectedIndex());
    }

    @Test
    void thePanelReadsTheFirstNineSlotsOfAGameStatePlayer() {
        GameStateManager state = new GameStateManager();
        state.setPlayer(player);
        player.setItemAt(8, new Item(ItemType.COAL, 5));
        player.setItemAt(9, new Item(ItemType.COAL, 7));

        update();

        assertEquals("5", countText(8));
        StackPane last = FxToolkitSupport.onFx(() -> panel.slotNode(Player.HOTBAR_SIZE - 1));
        assertTrue(last.getChildren().stream().anyMatch(ImageView.class::isInstance));
    }
}
