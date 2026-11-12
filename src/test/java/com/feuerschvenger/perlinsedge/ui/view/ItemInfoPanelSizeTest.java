package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.crafting.Recipe;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The info panel keeps one size whatever there is to read, and a long text scrolls inside it. */
class ItemInfoPanelSizeTest {

    private static final String LONG_TEXT = "A very long description that goes on and on. ".repeat(60);

    private record Measure(double width, double height, double areaHeight, double contentHeight) { }

    private static ItemInfoPanel panelInScene() {
        return FxToolkitSupport.onFx(() -> {
            ItemInfoPanel panel = new ItemInfoPanel();
            Pane root = new Pane(panel);
            Scene scene = new Scene(root, 800, 600);
            scene.getStylesheets().add(UiTheme.class.getResource(UiTheme.STYLESHEET_PATH).toExternalForm());
            root.applyCss();
            root.layout();
            return panel;
        });
    }

    private static Measure measure(ItemInfoPanel panel) {
        return FxToolkitSupport.onFx(() -> {
            panel.getParent().applyCss();
            ((Pane) panel.getParent()).layout();
            ScrollPane area = panel.detailsArea();
            return new Measure(panel.getWidth(), panel.getHeight(), area.getHeight(),
                    area.getContent().getLayoutBounds().getHeight());
        });
    }

    @Test
    void aLongDescriptionDoesNotChangeThePanelSize() {
        ItemInfoPanel panel = panelInScene();
        Measure empty = measure(panel);

        FxToolkitSupport.onFx(() -> {
            panel.show("long", "Long item", LONG_TEXT);
            return null;
        });
        Measure withLongText = measure(panel);

        assertEquals(empty.width(), withLongText.width(), 1e-9, "width");
        assertEquals(empty.height(), withLongText.height(), 1e-9, "height");
        assertEquals(empty.areaHeight(), withLongText.areaHeight(), 1e-9, "the reading area");
    }

    @Test
    void aLongTitleAndAShortOneGiveTheSamePanelSize() {
        ItemInfoPanel panel = panelInScene();
        FxToolkitSupport.onFx(() -> {
            panel.show("a", "Ax", "short");
            return null;
        });
        Measure shortTitle = measure(panel);

        FxToolkitSupport.onFx(() -> {
            panel.show("a", "A rather long title for an item (12)", "short");
            return null;
        });
        Measure longTitle = measure(panel);

        assertEquals(shortTitle.height(), longTitle.height(), 1e-9);
    }

    @Test
    void aLongDescriptionScrollsInsideTheAreaAndWrapsInsteadOfGoingSideways() {
        ItemInfoPanel panel = panelInScene();

        FxToolkitSupport.onFx(() -> {
            panel.show("long", "Long item", LONG_TEXT);
            return null;
        });
        Measure measured = measure(panel);

        assertTrue(measured.contentHeight() > measured.areaHeight() * 2, "the text is far taller than its area");
        FxToolkitSupport.onFx(() -> {
            assertEquals(ScrollPane.ScrollBarPolicy.NEVER, panel.detailsArea().getHbarPolicy());
            assertEquals(ScrollPane.ScrollBarPolicy.AS_NEEDED, panel.detailsArea().getVbarPolicy());
            assertTrue(panel.detailsArea().getContent().getLayoutBounds().getWidth()
                    <= panel.detailsArea().getWidth(), "wrapped text is no wider than the area");
            return null;
        });
    }

    @Test
    void selectingAnotherItemScrollsTheDescriptionBackToItsStart() {
        ItemInfoPanel panel = panelInScene();
        FxToolkitSupport.onFx(() -> {
            panel.show("long", "Long item", LONG_TEXT);
            panel.detailsArea().setVvalue(1.0);
            panel.show("other", "Other", "short");
            return null;
        });

        assertEquals(0.0, FxToolkitSupport.onFx(() -> panel.detailsArea().getVvalue()), 1e-9);
    }

    private static void scrollWheel(ItemInfoPanel panel, double deltaY) {
        FxToolkitSupport.onFx(() -> {
            ScrollPane area = panel.detailsArea();
            // Fired at the text, as the platform does for a mouse over it, so it bubbles up through the viewport
            Event.fireEvent(area.getContent(), new ScrollEvent(ScrollEvent.SCROLL, 20, 20, 20, 20, false, false, false, false, false,
                    false, 0, deltaY, 0, deltaY, ScrollEvent.HorizontalTextScrollUnits.NONE, 0,
                    ScrollEvent.VerticalTextScrollUnits.NONE, 0, 0, null));
            return null;
        });
    }

    @Test
    void theMouseWheelScrollsTheDescription() {
        ItemInfoPanel panel = panelInScene();
        FxToolkitSupport.onFx(() -> {
            panel.show("long", "Long item", LONG_TEXT);
            return null;
        });
        measure(panel);

        scrollWheel(panel, -80);

        assertTrue(FxToolkitSupport.onFx(() -> panel.detailsArea().getVvalue()) > 0.0, "the wheel moved the text");
    }

    @Test
    void showingTheSameSubjectAgainKeepsTheScrollPosition() {
        ItemInfoPanel panel = panelInScene();
        FxToolkitSupport.onFx(() -> {
            panel.show("long", "Long item", LONG_TEXT);
            return null;
        });
        measure(panel);
        scrollWheel(panel, -80);
        double scrolled = FxToolkitSupport.onFx(() -> panel.detailsArea().getVvalue());

        // The inventory shows its selection again on every tick of the game loop
        for (int tick = 0; tick < 10; tick++) {
            FxToolkitSupport.onFx(() -> {
                panel.show("long", "Long item", LONG_TEXT);
                return null;
            });
        }

        assertEquals(scrolled, FxToolkitSupport.onFx(() -> panel.detailsArea().getVvalue()), 1e-9);
    }

    @Test
    void theScrollBarThumbCanBeDraggedWhileTheSelectionIsShownAgain() {
        ItemInfoPanel panel = panelInScene();
        FxToolkitSupport.onFx(() -> {
            panel.show("long", "Long item", LONG_TEXT);
            return null;
        });
        measure(panel);

        FxToolkitSupport.onFx(() -> {
            ScrollBar bar = (ScrollBar) panel.detailsArea().lookup(".scroll-bar:vertical");
            bar.setValue(bar.getMax() / 2);
            panel.show("long", "Long item", LONG_TEXT);
            return null;
        });

        assertTrue(FxToolkitSupport.onFx(() -> panel.detailsArea().getVvalue()) > 0.0);
    }

    @Test
    void showingARecipeAgainKeepsItsIngredientLinesInPlace() {
        ItemInfoPanel panel = panelInScene();
        Recipe recipe = new RecipeManager().getAllItemRecipes().get(0);
        FxToolkitSupport.onFx(() -> {
            panel.displayRecipe(recipe, type -> 0);
            return null;
        });
        List<Text> first = FxToolkitSupport.onFx(panel::shownIngredientLines);

        FxToolkitSupport.onFx(() -> {
            panel.displayRecipe(recipe, type -> 0);
            return null;
        });

        assertEquals(first, FxToolkitSupport.onFx(panel::shownIngredientLines), "the same nodes, none rebuilt");
    }

    @Test
    void theIngredientsAreAboveTheDescription() {
        ItemInfoPanel panel = panelInScene();
        Recipe recipe = new RecipeManager().getAllItemRecipes().get(0);
        FxToolkitSupport.onFx(() -> {
            panel.displayRecipe(recipe, type -> 0);
            return null;
        });

        double ingredientsTop = FxToolkitSupport.onFx(() ->
                panel.shownIngredientLines().get(0).getParent().getBoundsInParent().getMinY());
        double descriptionTop = FxToolkitSupport.onFx(() -> {
            VBox details = (VBox) panel.detailsArea().getContent();
            return details.getChildren().get(2).getBoundsInParent().getMinY();
        });

        assertTrue(ingredientsTop < descriptionTop);
    }
}
