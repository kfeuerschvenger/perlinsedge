package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Panel for selecting a map type before starting a new game.
 * Displays a grid of map options with representative images.
 */
@Slf4j
public class MapSelectionPanel extends VBox {
    private static final double THUMBNAIL_HEIGHT = 96;

    private Consumer<MapType> onMapSelected;
    private Runnable onBackAction;

    // Mapping of MapType to its background image path
    private final Map<MapType, String> mapThumbnails = new HashMap<>();

    public MapSelectionPanel() {
        setSpacing(20); // Spacing between elements in the VBox
        setAlignment(Pos.CENTER); // Center elements horizontally and vertically

        minWidthProperty().bind(prefWidthProperty());
        minHeightProperty().bind(prefHeightProperty());

        getStyleClass().add(UiTheme.PANEL);

        // Title text
        Label title = new Label(UiTheme.caps("Select a Map"));
        title.getStyleClass().add(UiTheme.TITLE);

        // Configure map thumbnail paths
        setupMapThumbnails();

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
     * Sets up the map thumbnail image paths.
     * Ensure these paths are correct and images exist in your project's resources directory.
     */
    private void setupMapThumbnails() {
        mapThumbnails.put(MapType.DENSE_FOREST, "/assets/maps/dense_forest_thumbnail.png");
        mapThumbnails.put(MapType.SWAMP, "/assets/maps/swamp_thumbnail.png");
        mapThumbnails.put(MapType.DESERT, "/assets/maps/desert_thumbnail.png");
        mapThumbnails.put(MapType.TUNDRA, "/assets/maps/tundra_thumbnail.png");
        mapThumbnails.put(MapType.MEDITERRANEAN, "/assets/maps/mediterranean_thumbnail.png");
        mapThumbnails.put(MapType.ISLANDS, "/assets/maps/islands_thumbnail.png");
    }

    /**
     * Creates a styled button for map selection with a background image.
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
        button.setContentDisplay(ContentDisplay.TOP); // Place text below the image

        Image thumbnail = loadThumbnail(mapType);
        if (thumbnail != null) {
            ImageView view = new ImageView(thumbnail);
            view.setPreserveRatio(true);
            view.setSmooth(false);
            view.setFitHeight(THUMBNAIL_HEIGHT);
            button.setGraphic(view);
        }
        return button;
    }

    /** Loads the thumbnail of a map type; a missing or unreadable file leaves the plain button. */
    private Image loadThumbnail(MapType mapType) {
        String imageUrl = mapThumbnails.get(mapType);
        if (imageUrl == null) {
            return null;
        }
        try (InputStream in = getClass().getResourceAsStream(imageUrl)) {
            if (in == null) {
                log.warn("Map thumbnail not found: {} (check path in src/main/resources)", imageUrl);
                return null;
            }
            return new Image(in);
        } catch (Exception e) {
            log.warn("Error loading image for {}: {}", mapType.name(), imageUrl, e);
            return null;
        }
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