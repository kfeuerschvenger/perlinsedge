package com.feuerschvenger.perlinsedge.app;

import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.ui.view.FxToolkitSupport;
import com.feuerschvenger.perlinsedge.ui.view.HealthBar;
import com.feuerschvenger.perlinsedge.ui.view.InventoryPanel;
import com.feuerschvenger.perlinsedge.ui.view.ItemHotbarPanel;
import com.feuerschvenger.perlinsedge.ui.view.MapConfigPanel;
import com.feuerschvenger.perlinsedge.ui.view.PauseMenuPanel;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Builds the game screen on a root pane the way the application does and checks what it puts on it. */
class GameScreenTest {

    private static final double WIDTH = 1000;
    private static final double HEIGHT = 700;

    private final GameStateManager state = new GameStateManager();
    private final List<String> events = new ArrayList<>();
    private StackPane root;
    private Scene scene;
    private GameScreen screen;

    private static <T> T onFx(Callable<T> task) {
        return FxToolkitSupport.onFx(task);
    }

    private static <T extends Node> List<T> all(Node node, Class<T> type) {
        final List<T> found = new ArrayList<>();
        if (type.isInstance(node)) {
            found.add(type.cast(node));
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                found.addAll(all(child, type));
            }
        }
        return found;
    }

    @BeforeEach
    void setUp() {
        onFx(() -> {
            root = new StackPane();
            root.setPrefSize(WIDTH, HEIGHT);
            scene = new Scene(root);
            screen = new GameScreen(root, state, new RecipeManager());
            return null;
        });
    }

    private void layout() {
        onFx(() -> {
            root.applyCss();
            root.layout();
            return null;
        });
    }

    @Test
    void showingTheScreenPutsTheWorldLayerBelowThePanelLayer() {
        onFx(() -> {
            screen.show();
            return null;
        });

        assertEquals(2, root.getChildren().size());
        final Pane world = (Pane) root.getChildren().get(0);
        final Pane panels = (Pane) root.getChildren().get(1);
        assertEquals(WIDTH, world.getPrefWidth(), 1e-9);
        assertEquals(HEIGHT, world.getPrefHeight(), 1e-9);
        assertFalse(panels.isPickOnBounds(), "clicks fall through the empty parts of the panel layer");
        final Canvas canvas = all(world, Canvas.class).getFirst();
        assertEquals(WIDTH, canvas.getWidth(), 1e-9);
        assertEquals(HEIGHT, canvas.getHeight(), 1e-9);
    }

    @Test
    void theMapConfigAndInventoryPanelsStartHiddenAndOutOfTheLayout() {
        onFx(() -> {
            screen.show();
            return null;
        });

        final Node panels = root.getChildren().get(1);
        final MapConfigPanel config = all(panels, MapConfigPanel.class).getFirst();
        final InventoryPanel inventory = all(panels, InventoryPanel.class).getFirst();
        assertFalse(config.isVisible());
        assertFalse(config.isManaged());
        assertFalse(inventory.isVisible());
        assertFalse(inventory.isManaged());
    }

    @Test
    void theHotbarIsCentredAtTheBottomAndTheHealthBarSitsAboveIt() {
        onFx(() -> {
            screen.show();
            return null;
        });
        layout();

        final Node panels = root.getChildren().get(1);
        final ItemHotbarPanel hotbar = all(panels, ItemHotbarPanel.class).getFirst();
        final HealthBar health = all(panels, HealthBar.class).getFirst();

        assertEquals((root.getWidth() - hotbar.getWidth()) / 2, hotbar.getLayoutX(), 1e-6);
        assertTrue(hotbar.getLayoutY() + hotbar.getHeight() <= root.getHeight());
        assertEquals(hotbar.getLayoutX(), health.getLayoutX(), 1e-6);
        assertTrue(health.getLayoutY() + health.getHeight() <= hotbar.getLayoutY());
    }

    @Test
    void theOrchestratorGetsAHiddenPauseMenuWhoseButtonsReachTheCallbacks() {
        onFx(() -> {
            screen.show();
            return null;
        });
        final GameOrchestrator orchestrator = onFx(() -> screen.createOrchestrator(scene,
                () -> events.add("exit"), () -> events.add("menu")));
        assertNotNull(orchestrator);

        final PauseMenuPanel pause = all(root.getChildren().get(1), PauseMenuPanel.class).getFirst();
        assertFalse(pause.isVisible());
        assertFalse(pause.isManaged());
        final List<Button> buttons = all(pause, Button.class);
        final Button exit = buttons.stream().filter(b -> b.getText().equals("EXIT TO DESKTOP")).findFirst().orElseThrow();
        final Button back = buttons.stream().filter(b -> b.getText().equals("BACK TO MAIN MENU")).findFirst().orElseThrow();

        onFx(() -> {
            exit.fire();
            back.fire();
            return null;
        });

        assertEquals(List.of("exit", "menu"), events);
    }

    @Test
    void aGameStartedOnTheScreenBuildsAWorldAndAPlayerAndLeavingStopsIt() {
        onFx(() -> {
            screen.show();
            return null;
        });
        final GameOrchestrator orchestrator = onFx(() -> screen.createOrchestrator(scene, () -> { }, () -> { }));

        onFx(() -> {
            orchestrator.startNewGame(MapType.DESERT);
            return null;
        });
        try {
            assertNotNull(state.getCurrentMap());
            assertNotNull(state.getPlayer());
        } finally {
            onFx(() -> {
                orchestrator.leaveGame();
                return null;
            });
        }
    }
}
