package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Panel for selecting a map type before starting a new game.
 * Displays a grid of map options as styled buttons.
 */
public final class MapSelectionPanel extends VBox {
    private static final double PANEL_SPACING = 20;
    private static final double GRID_GAP = 20;
    private static final double GRID_PADDING = 20;
    /** Columns of the map grid. */
    private static final int GRID_COLUMNS = 3;
    /** The whole of a grid, as the percentage its columns or rows share. */
    private static final double FULL_PERCENT = 100.0;
    private static final double BUTTON_WIDTH = 160;
    private static final double BUTTON_HEIGHT = 36;

    private Consumer<MapType> onMapSelected;
    private Runnable onBackAction;

    public MapSelectionPanel() {
        setSpacing(PANEL_SPACING); // Spacing between elements in the VBox
        setAlignment(Pos.CENTER); // Center elements horizontally and vertically

        minWidthProperty().bind(prefWidthProperty());
        minHeightProperty().bind(prefHeightProperty());

        getStyleClass().add(UiTheme.PANEL);

        // Title text
        var title = new Label(UiTheme.caps("Select a Map"));
        title.getStyleClass().add(UiTheme.TITLE);

        // Map grid container
        var mapGrid = new GridPane();
        mapGrid.setHgap(GRID_GAP); // Horizontal gap between cells
        mapGrid.setVgap(GRID_GAP); // Vertical gap between cells
        mapGrid.setPadding(new Insets(GRID_PADDING)); // Padding around the grid
        mapGrid.setAlignment(Pos.CENTER); // Center the grid within its allocated space

        // Define responsive column constraints (3 columns, each taking 1/3 of available width)
        for (var i = 0; i < GRID_COLUMNS; i++) {
            var cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS); // Allow columns to grow horizontally
            cc.setPercentWidth(FULL_PERCENT / GRID_COLUMNS);
            mapGrid.getColumnConstraints().add(cc);
        }

        // Only selected maps for the grid
        MapType[] selectableMaps = {
                MapType.DENSE_FOREST,
                MapType.SWAMP,
                MapType.DESERT,
                MapType.TUNDRA,
                MapType.MEDITERRANEAN,
                MapType.ISLANDS
        };

        // Define responsive row constraints (based on number of rows needed)
        var numRows = Math.ceilDiv(selectableMaps.length, GRID_COLUMNS); // Calculate rows needed (e.g., 6 maps / 3 cols = 2 rows)
        for (var i = 0; i < numRows; i++) {
            var rc = new RowConstraints();
            rc.setVgrow(Priority.ALWAYS); // Allow rows to grow vertically
            rc.setPercentHeight(FULL_PERCENT / numRows);
            mapGrid.getRowConstraints().add(rc);
        }


        var col = 0;
        var row = 0;
        for (MapType mapType : selectableMaps) {
            var mapButton = createMapButton(mapType);
            mapGrid.add(mapButton, col, row);

            mapButton.setOnAction(e -> {
                if (onMapSelected != null) {
                    onMapSelected.accept(mapType);
                }
            });

            col++;
            if (col >= GRID_COLUMNS) {
                col = 0;
                row++;
            }
        }

        // "Back" button
        var backButton = createMenuButton("Back");
        backButton.setOnAction(e -> {
            if (onBackAction != null) {
                onBackAction.run();
            }
        });

        getChildren().addAll(title, mapGrid, backButton);

        // Crucial: Allow the GridPane to grow and take all available vertical space in the VBox
        VBox.setVgrow(mapGrid, Priority.ALWAYS);
    }

    /**
     * Creates a styled button for map selection.
     *
     * @param mapType The MapType associated with this button.
     * @return The configured Button.
     */
    private static Button createMapButton(MapType mapType) {
        var button = new Button(UiTheme.caps(mapType.getDisplayName()));
        button.getStyleClass().add(UiTheme.CARD);
        // Set maximum width/height so button fills its grid cell
        button.setMaxWidth(Double.MAX_VALUE);
        button.setMaxHeight(Double.MAX_VALUE);
        return button;
    }

    /**
     * Creates a standard menu button.
     * @param text The text for the button.
     * @return The configured Button.
     */
    private static Button createMenuButton(String text) {
        var button = new Button(UiTheme.caps(text));
        button.setPrefWidth(BUTTON_WIDTH);
        button.setPrefHeight(BUTTON_HEIGHT);
        return button;
    }

    public void setOnMapSelected(Consumer<MapType> onMapSelected) {
        this.onMapSelected = onMapSelected;
    }

    public void setOnBackAction(Runnable onBackAction) {
        this.onBackAction = onBackAction;
    }
}
