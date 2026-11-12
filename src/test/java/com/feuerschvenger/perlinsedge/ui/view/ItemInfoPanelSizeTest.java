package com.feuerschvenger.perlinsedge.ui.view;

import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.Test;

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
            panel.show("Long item", LONG_TEXT);
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
            panel.show("Ax", "short");
            return null;
        });
        Measure shortTitle = measure(panel);

        FxToolkitSupport.onFx(() -> {
            panel.show("A rather long title for an item (12)", "short");
            return null;
        });
        Measure longTitle = measure(panel);

        assertEquals(shortTitle.height(), longTitle.height(), 1e-9);
    }

    @Test
    void aLongDescriptionScrollsInsideTheAreaAndWrapsInsteadOfGoingSideways() {
        ItemInfoPanel panel = panelInScene();

        FxToolkitSupport.onFx(() -> {
            panel.show("Long item", LONG_TEXT);
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
            panel.show("Long item", LONG_TEXT);
            panel.detailsArea().setVvalue(1.0);
            panel.show("Other", "short");
            return null;
        });

        assertEquals(0.0, FxToolkitSupport.onFx(() -> panel.detailsArea().getVvalue()), 1e-9);
    }
}
