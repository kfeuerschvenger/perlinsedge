package com.feuerschvenger.perlinsedge.app;

import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.ui.view.MainMenuPanel;
import com.feuerschvenger.perlinsedge.ui.view.MapSelectionPanel;
import javafx.scene.layout.StackPane;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

/**
 * Shows the main menu and the map selection on the root pane and moves between them. Each panel is built the first
 * time it is shown and reused afterwards.
 */
@Slf4j
final class MenuNavigator {
    /** Main menu width as a fraction of the root pane. */
    private static final double MAIN_MENU_WIDTH_RATIO = 0.35;
    /** Main menu height as a fraction of the root pane. */
    private static final double MAIN_MENU_HEIGHT_RATIO = 0.45;
    /** Map selection width as a fraction of the root pane. */
    private static final double MAP_SELECTION_WIDTH_RATIO = 0.6;
    /** Map selection height as a fraction of the root pane. */
    private static final double MAP_SELECTION_HEIGHT_RATIO = 0.7;

    private final StackPane rootPane;
    private final Consumer<MapType> onMapSelected;
    private final Runnable onExit;

    private MainMenuPanel mainMenuPanel;
    private MapSelectionPanel mapSelectionPanel;

    /**
     * Creates the navigator.
     *
     * @param rootPane      the pane the menus are shown on
     * @param onMapSelected called with the map the player picked
     * @param onExit        called when the player asks to leave the application
     */
    MenuNavigator(StackPane rootPane, Consumer<MapType> onMapSelected, Runnable onExit) {
        this.rootPane = rootPane;
        this.onMapSelected = onMapSelected;
        this.onExit = onExit;
    }

    /** Displays the main menu screen. */
    void showMainMenu() {
        rootPane.getChildren().clear();

        if (mainMenuPanel == null) {
            createMainMenuPanel();
        }

        rootPane.getChildren().add(mainMenuPanel);
        mainMenuPanel.toFront();
    }

    /** Displays the map selection menu. */
    void showMapSelectionMenu() {
        rootPane.getChildren().clear();

        if (mapSelectionPanel == null) {
            createMapSelectionPanel();
        }

        rootPane.getChildren().add(mapSelectionPanel);
        mapSelectionPanel.toFront();
    }

    private void createMainMenuPanel() {
        mainMenuPanel = new MainMenuPanel();

        mainMenuPanel.prefWidthProperty().bind(rootPane.widthProperty().multiply(MAIN_MENU_WIDTH_RATIO));
        mainMenuPanel.prefHeightProperty().bind(rootPane.heightProperty().multiply(MAIN_MENU_HEIGHT_RATIO));

        mainMenuPanel.setOnNewGameAction(this::showMapSelectionMenu);
        mainMenuPanel.setOnLoadGameAction(() -> log.warn("Load game not implemented"));
        mainMenuPanel.setOnExitAction(onExit);
    }

    private void createMapSelectionPanel() {
        mapSelectionPanel = new MapSelectionPanel();

        mapSelectionPanel.prefWidthProperty().bind(rootPane.widthProperty().multiply(MAP_SELECTION_WIDTH_RATIO));
        mapSelectionPanel.prefHeightProperty().bind(rootPane.heightProperty().multiply(MAP_SELECTION_HEIGHT_RATIO));
        mapSelectionPanel.minWidthProperty().bind(mapSelectionPanel.prefWidthProperty());
        mapSelectionPanel.minHeightProperty().bind(mapSelectionPanel.prefHeightProperty());

        mapSelectionPanel.setOnMapSelected(onMapSelected);
        mapSelectionPanel.setOnBackAction(this::showMainMenu);
    }
}
