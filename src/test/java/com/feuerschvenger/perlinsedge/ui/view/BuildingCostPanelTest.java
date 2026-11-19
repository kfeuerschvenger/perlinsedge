package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildingCostPanelTest {

    private final RecipeManager recipes = new RecipeManager();
    private Player player;
    private BuildingCostPanel panel;

    @BeforeEach
    void setUp() {
        player = new Player(0, 0);
        panel = FxToolkitSupport.onFx(() -> new BuildingCostPanel(recipes));
    }

    private void update(BuildingType selected) {
        FxToolkitSupport.onFx(() -> {
            panel.update(player, selected);
            return null;
        });
    }

    private void enable(boolean enabled) {
        FxToolkitSupport.onFx(() -> {
            panel.setEnabled(enabled);
            return null;
        });
    }

    private List<String> lines() {
        return FxToolkitSupport.onFx(() -> panel.lineLabels().stream().map(Label::getText).toList());
    }

    private boolean has(int line, String style) {
        return FxToolkitSupport.onFx(() -> panel.lineLabels().get(line).getStyleClass().contains(style));
    }

    @Test
    void itIsHiddenUntilBuildModeIsOnAndAPieceIsSelected() {
        assertFalse(FxToolkitSupport.onFx(panel::isVisible));

        enable(true);
        update(null);
        assertFalse(FxToolkitSupport.onFx(panel::isVisible), "no piece selected");

        update(BuildingType.FENCE);
        assertTrue(FxToolkitSupport.onFx(panel::isVisible));
        assertTrue(FxToolkitSupport.onFx(panel::isManaged));

        enable(false);
        assertFalse(FxToolkitSupport.onFx(panel::isVisible), "build mode is off");
        assertFalse(FxToolkitSupport.onFx(panel::isManaged));
    }

    @Test
    void itIsTitledWillConsume() {
        assertEquals("WILL CONSUME:", FxToolkitSupport.onFx(() -> panel.titleLabel().getText()));
    }

    @Test
    void theTitleCarriesThePanelTitleStyleThatTheThemeDefines() throws java.io.IOException {
        assertTrue(FxToolkitSupport.onFx(() -> panel.titleLabel().getStyleClass().contains(UiTheme.PANEL_TITLE)));
        try (java.io.InputStream css = getClass().getResourceAsStream(UiTheme.STYLESHEET_PATH)) {
            final String theme = new String(css.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            final String rule = theme.substring(theme.indexOf("." + UiTheme.PANEL_TITLE));
            final String body = rule.substring(0, rule.indexOf('}'));
            assertTrue(body.contains("-fx-font-weight: bold"), "bold");
            assertTrue(body.contains("-fx-padding: 0 0 4 0"), "room below the title");
        }
    }

    @Test
    void itListsEveryIngredientOfTheSelectedPieceWithItsQuantity() {
        enable(true);
        update(BuildingType.FURNACE);

        assertEquals(List.of("STONE x8 (HAVE 0)", "COAL x2 (HAVE 0)"), lines());
    }

    @Test
    void aLineThePlayerLacksIsMarkedRedAndOneHeldIsGreen() {
        player.addItem(new Item(ItemType.STONE, 9));
        enable(true);
        update(BuildingType.FURNACE);

        assertEquals("STONE x8", lines().get(0));
        assertTrue(has(0, UiTheme.TEXT_GOOD));
        assertFalse(has(0, UiTheme.TEXT_BAD));
        assertEquals("COAL x2 (HAVE 0)", lines().get(1));
        assertTrue(has(1, UiTheme.TEXT_BAD));
        assertFalse(has(1, UiTheme.TEXT_GOOD));
    }

    @Test
    void itFollowsTheSelectionAndTheInventory() {
        enable(true);
        update(BuildingType.FENCE);
        assertEquals(List.of("WOOD LOG x2 (HAVE 0)"), lines());

        player.addItem(new Item(ItemType.WOOD_LOG, 2));
        update(BuildingType.FENCE);
        assertEquals(List.of("WOOD LOG x2"), lines());
        assertTrue(has(0, UiTheme.TEXT_GOOD));

        update(BuildingType.FURNACE);
        assertEquals(2, lines().size());
    }

    @Test
    void itDoesNotTakeClicksMeantForTheWorld() {
        assertTrue(FxToolkitSupport.onFx(panel::isMouseTransparent));
    }
}
