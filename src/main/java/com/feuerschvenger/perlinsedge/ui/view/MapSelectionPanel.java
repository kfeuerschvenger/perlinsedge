package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.util.function.Consumer;

/**
 * Panel for selecting a map type before starting a new game.
 * Displays a grid of map options as styled buttons.
 */
public class MapSelectionPanel extends VBox {
    private Consumer<MapType> onMapSelected;
    private Runnable onBackAction;

    public MapSelectionPanel() {
        setSpacing(20); // Spacing between elements in the VBox
        setAlignment(Pos.CENTER); // Center elements horizontally and vertically

        minWidthProperty().bind(prefWidthProperty());
        minHeightProperty().bind(prefHeightProperty());

        getStyleClass().add(UiTheme.PANEL);

        // Title text
        Label title = new Label(UiTheme.caps("Select a Map"));
        title.getStyleClass().add(UiTheme.TITLE);

        // Map grid container
        GridPane mapGrid = new GridPane();
        mapGrid.setHgap(20); // Horizontal gap between cells
        mapGrid.setVgap(20); // Vertical gap between cells
        mapGrid.setPadding(new Insets(20)); // Padding around the grid
        mapGrid.setAlignment(Pos.CENTER); // Center the grid within its allocated space

        // Define responsive column constraints (3 columns, each taking 1/3 of available width)
        for (int i = 0; i < 3; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS); // Allow columns to grow horizontally
            cc.setPercentWidth(100.0 / 3);
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
        int numRows = (int) Math.ceil(selectableMaps.length / 3.0); // Calculate rows needed (e.g., 6 maps / 3 cols = 2 rows)
        for (int i = 0; i < numRows; i++) {
            RowConstraints rc = new RowConstraints();
            rc.setVgrow(Priority.ALWAYS); // Allow rows to grow vertically
            rc.setPercentHeight(100.0 / numRows);
            mapGrid.getRowConstraints().add(rc);
        }


        int col = 0;
        int row = 0;
        for (MapType mapType : selectableMaps) {
            Button mapButton = createMapButton(mapType);
            mapGrid.add(mapButton, col, row);

            mapButton.setOnAction(e -> {
                if (onMapSelected != null) {
                    onMapSelected.accept(mapType);
                }
            });

            col++;
            if (col >= 3) { // 3 columns per row
                col = 0;
                row++;
            }
        }

        // "Back" button
        Button backButton = createMenuButton("Back");
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
    private Button createMapButton(MapType mapType) {
        Button button = new Button(UiTheme.caps(mapType.getDisplayName()));
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
    private Button createMenuButton(String text) {
        Button button = new Button(UiTheme.caps(text));
        button.setPrefWidth(160);
        button.setPrefHeight(36);
        return button;
    }

    public void setOnMapSelected(Consumer<MapType> onMapSelected) {
        this.onMapSelected = onMapSelected;
    }

    public void setOnBackAction(Runnable onBackAction) {
        this.onBackAction = onBackAction;
    }
}