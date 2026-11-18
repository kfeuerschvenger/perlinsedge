package com.feuerschvenger.perlinsedge.app;

import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.ui.view.FxToolkitSupport;
import com.feuerschvenger.perlinsedge.ui.view.MainMenuPanel;
import com.feuerschvenger.perlinsedge.ui.view.MapSelectionPanel;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class MenuNavigatorTest {

    private final List<MapType> started = new ArrayList<>();
    private final List<String> exits = new ArrayList<>();
    private StackPane root;
    private MenuNavigator menus;

    private static <T> T onFx(Callable<T> task) {
        return FxToolkitSupport.onFx(task);
    }

    private static Button button(Node node, String text) {
        if (node instanceof Button b && b.getText().equals(text)) {
            return b;
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                final Button found = button(child, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private void fire(Node panel, String text) {
        final Button target = button(panel, text);
        onFx(() -> {
            target.fire();
            return null;
        });
    }

    @BeforeEach
    void setUp() {
        onFx(() -> {
            root = new StackPane();
            root.setPrefSize(1000, 600);
            new Scene(root);
            menus = new MenuNavigator(root, started::add, () -> exits.add("exit"));
            return null;
        });
    }

    @Test
    void theMainMenuIsTheOnlyChildWhileItIsShown() {
        onFx(() -> {
            menus.showMainMenu();
            return null;
        });

        assertEquals(1, root.getChildren().size());
        assertInstanceOf(MainMenuPanel.class, root.getChildren().getFirst());
    }

    @Test
    void theMainMenuIsSizedFromTheRootPane() {
        onFx(() -> {
            menus.showMainMenu();
            root.applyCss();
            root.layout();
            return null;
        });

        final MainMenuPanel menu = (MainMenuPanel) root.getChildren().getFirst();
        assertEquals(root.getWidth() * 0.35, menu.getPrefWidth(), 1e-6);
        assertEquals(root.getHeight() * 0.45, menu.getPrefHeight(), 1e-6);
    }

    @Test
    void newGameLeadsToTheMapSelectionAndBackLeadsToTheSameMainMenu() {
        onFx(() -> {
            menus.showMainMenu();
            return null;
        });
        final Node mainMenu = root.getChildren().getFirst();

        fire(mainMenu, "NEW GAME");
        assertEquals(1, root.getChildren().size());
        final Node selection = root.getChildren().getFirst();
        assertInstanceOf(MapSelectionPanel.class, selection);

        fire(selection, "BACK");
        assertSame(mainMenu, root.getChildren().getFirst(), "the panel is built once and reused");
    }

    @Test
    void choosingAMapReportsItToTheCallback() {
        onFx(() -> {
            menus.showMapSelectionMenu();
            return null;
        });

        fire(root.getChildren().getFirst(), "TUNDRA");

        assertEquals(List.of(MapType.TUNDRA), started);
    }

    @Test
    void exitReachesTheExitCallback() {
        onFx(() -> {
            menus.showMainMenu();
            return null;
        });

        fire(root.getChildren().getFirst(), "EXIT");

        assertEquals(List.of("exit"), exits);
    }

    @Test
    void theLoadButtonOfTheMainMenuIsInertForNow() {
        onFx(() -> {
            menus.showMainMenu();
            return null;
        });

        fire(root.getChildren().getFirst(), "LOAD GAME");

        assertEquals(1, root.getChildren().size());
        assertInstanceOf(MainMenuPanel.class, root.getChildren().getFirst());
    }
}
