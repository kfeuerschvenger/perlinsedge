package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuPanelsTest {

    private static <T> T onFx(Supplier<T> supplier) {
        return FxToolkitSupport.onFx(supplier::get);
    }

    private static void run(Runnable action) {
        FxToolkitSupport.onFx(() -> {
            action.run();
            return null;
        });
    }

    private static <T extends Node> List<T> all(Node root, Class<T> type) {
        final List<T> found = new ArrayList<>();
        collect(root, type, found);
        return found;
    }

    private static <T extends Node> void collect(Node node, Class<T> type, List<T> found) {
        if (type.isInstance(node)) {
            found.add(type.cast(node));
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                collect(child, type, found);
            }
        }
    }

    private static Button button(Node root, String text) {
        return all(root, Button.class).stream().filter(b -> b.getText().equals(text)).findFirst()
                .orElseThrow(() -> new AssertionError("No button " + text));
    }

    private static void click(Button button) {
        run(button::fire);
    }

    // ---- main menu ----

    @Test
    void theMainMenuHasATitleAndThreeButtonsAndLoadingIsNotAvailableYet() {
        final MainMenuPanel menu = onFx(MainMenuPanel::new);

        assertEquals("PERLIN'S EDGE", all(menu, Label.class).getFirst().getText());
        assertTrue(menu.getStyleClass().contains(UiTheme.PANEL));
        assertFalse(button(menu, "NEW GAME").isDisabled());
        assertTrue(button(menu, "LOAD GAME").isDisabled());
        assertFalse(button(menu, "EXIT").isDisabled());
    }

    @Test
    void theMainMenuButtonsFireTheirCallbacks() {
        final MainMenuPanel menu = onFx(MainMenuPanel::new);
        final List<String> fired = new ArrayList<>();
        menu.setOnNewGameAction(() -> fired.add("new"));
        menu.setOnLoadGameAction(() -> fired.add("load"));
        menu.setOnExitAction(() -> fired.add("exit"));

        click(button(menu, "NEW GAME"));
        click(button(menu, "LOAD GAME"));
        click(button(menu, "EXIT"));

        assertEquals(List.of("new", "exit"), fired, "the disabled load button never fires");
    }

    @Test
    void theMainMenuButtonsDoNothingWhileNoCallbackIsSet() {
        final MainMenuPanel menu = onFx(MainMenuPanel::new);
        final List<String> fired = new ArrayList<>();

        click(button(menu, "NEW GAME"));
        click(button(menu, "LOAD GAME"));
        click(button(menu, "EXIT"));
        menu.setOnNewGameAction(() -> fired.add("new"));
        click(button(menu, "NEW GAME"));

        assertEquals(List.of("new"), fired, "only the button with a callback reports");
    }

    // ---- pause menu ----

    @Test
    void thePauseMenuStartsHiddenAndSaveAndLoadAreNotAvailableYet() {
        final PauseMenuPanel menu = onFx(PauseMenuPanel::new);

        assertFalse(menu.isVisible());
        assertFalse(menu.isManaged());
        assertEquals("GAME PAUSED", all(menu, Label.class).getFirst().getText());
        assertTrue(button(menu, "SAVE GAME").isDisabled());
        assertTrue(button(menu, "LOAD GAME").isDisabled());
        assertFalse(button(menu, "RESUME GAME").isDisabled());
        assertFalse(button(menu, "EXIT TO DESKTOP").isDisabled());
    }

    @Test
    void thePauseMenuButtonsFireTheirCallbacks() {
        final PauseMenuPanel menu = onFx(PauseMenuPanel::new);
        final List<String> fired = new ArrayList<>();
        menu.setOnResumeAction(() -> fired.add("resume"));
        menu.setOnSaveAction(() -> fired.add("save"));
        menu.setOnLoadAction(() -> fired.add("load"));
        menu.setOnBackToMainMenuAction(() -> fired.add("back"));
        menu.setOnExitAction(() -> fired.add("exit"));

        click(button(menu, "RESUME GAME"));
        click(button(menu, "SAVE GAME"));
        click(button(menu, "LOAD GAME"));
        click(button(menu, "BACK TO MAIN MENU"));
        click(button(menu, "EXIT TO DESKTOP"));

        assertEquals(List.of("resume", "back", "exit"), fired, "the disabled save and load buttons never fire");
    }

    @Test
    void backToTheMainMenuDoesNothingWhileNoCallbackIsSet() {
        final PauseMenuPanel menu = onFx(PauseMenuPanel::new);

        click(button(menu, "BACK TO MAIN MENU"));

        assertTrue(menu.getChildren().size() >= 6, "the panel is untouched");
    }

    // ---- map selection ----

    @Test
    void theMapSelectionOffersTheSixMapsAndABackButton() {
        final MapSelectionPanel panel = onFx(MapSelectionPanel::new);

        assertEquals("SELECT A MAP", all(panel, Label.class).getFirst().getText());
        for (MapType type : List.of(MapType.DENSE_FOREST, MapType.SWAMP, MapType.DESERT, MapType.TUNDRA,
                MapType.MEDITERRANEAN, MapType.ISLANDS)) {
            assertTrue(button(panel, UiTheme.caps(type.getDisplayName())).getStyleClass().contains(UiTheme.CARD));
        }
        assertNotNull(button(panel, "BACK"));
    }

    @Test
    void choosingAMapReportsItsType() {
        final MapSelectionPanel panel = onFx(MapSelectionPanel::new);
        final List<MapType> chosen = new ArrayList<>();
        panel.setOnMapSelected(chosen::add);

        click(button(panel, UiTheme.caps(MapType.DESERT.getDisplayName())));
        click(button(panel, UiTheme.caps(MapType.ISLANDS.getDisplayName())));

        assertEquals(List.of(MapType.DESERT, MapType.ISLANDS), chosen);
    }

    @Test
    void backReportsAndNeitherButtonFailsWithoutACallback() {
        final MapSelectionPanel panel = onFx(MapSelectionPanel::new);
        click(button(panel, "BACK"));
        click(button(panel, UiTheme.caps(MapType.SWAMP.getDisplayName())));
        final List<String> fired = new ArrayList<>();
        panel.setOnBackAction(() -> fired.add("back"));

        click(button(panel, "BACK"));

        assertEquals(List.of("back"), fired);
    }

    // ---- theme ----

    @Test
    void installingTheThemeAddsTheStylesheetOnceAndLoadsTheFontOnce() {
        final Scene scene = onFx(() -> new Scene(new Pane()));

        run(() -> UiTheme.install(scene));
        run(() -> UiTheme.install(scene));

        assertEquals(1, scene.getStylesheets().size());
        assertTrue(scene.getStylesheets().getFirst().endsWith("theme.css"));
    }

    @Test
    void aStyleClassCanBeSwitchedOnAndOffWithoutDuplicates() {
        final Label label = onFx(Label::new);

        UiTheme.setClass(label, UiTheme.TEXT_GOOD, true);
        UiTheme.setClass(label, UiTheme.TEXT_GOOD, true);
        assertEquals(1, label.getStyleClass().stream().filter(UiTheme.TEXT_GOOD::equals).count());

        UiTheme.setClass(label, UiTheme.TEXT_GOOD, false);
        assertFalse(label.getStyleClass().contains(UiTheme.TEXT_GOOD));
    }

    @Test
    void anUnavailableIconIsGreyedAndDimmedAndRestoredWhenAvailable() {
        final ImageView icon = onFx(ImageView::new);

        UiTheme.setIconUnavailable(icon, true);
        assertInstanceOf(ColorAdjust.class, icon.getEffect());
        assertEquals(-1.0, ((ColorAdjust) icon.getEffect()).getSaturation(), 1e-9);
        assertTrue(icon.getOpacity() < 1.0);

        UiTheme.setIconUnavailable(icon, false);
        assertNull(icon.getEffect());
        assertEquals(1.0, icon.getOpacity(), 1e-9);
    }

    @Test
    void capsUppercasesAndTreatsNullAsEmpty() {
        assertEquals("NEW GAME", UiTheme.caps("New Game"));
        assertEquals("", UiTheme.caps(null));
    }
}
