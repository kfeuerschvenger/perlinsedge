package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Panel for selecting a map type before starting a new game.
 * Displays a grid of map options with representative images.
 */
public class MapSelectionPanel extends VBox {

    private Consumer<MapType> onMapSelected;
    private Runnable onBackAction;

    // Mapping of MapType to its background image path
    private final Map<MapType, String> mapThumbnails = new HashMap<>();

    public MapSelectionPanel() {
        setSpacing(20); // Spacing between elements in the VBox
        setAlignment(Pos.CENTER); // Center elements horizontally and vertically

        minWidthProperty().bind(prefWidthProperty());
        minHeightProperty().bind(prefHeightProperty());

        // General styles for the panel
        setStyle("-fx-background-color: rgba(0, 0, 0, 0.8);" +
                "-fx-border-color: #333333;" +
                "-fx-border-width: 2;" +
                "-fx-padding: 20;");

        // Title text
        Text title = new Text("Select a Map");
        // Bind font size to a percentage of panel height for responsiveness
        title.fontProperty().bind(Bindings.createObjectBinding(() ->
                        Font.font("Arial", Math.max(20, getHeight() * 0.06)), // Min 20, max 6% of height
                heightProperty()));
        title.setFill(Color.WHITE);

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
                MapType.DARK_FOREST,
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
        // Bind font size for back button
        backButton.fontProperty().bind(Bindings.createObjectBinding(() ->
                        Font.font("Arial", Math.max(12, getHeight() * 0.03)), // Min 12, max 3% of height
                heightProperty()));


        getChildren().addAll(title, mapGrid, backButton);

        // Crucial: Allow the GridPane to grow and take all available vertical space in the VBox
        VBox.setVgrow(mapGrid, Priority.ALWAYS);
    }

    /**
     * Sets up the map thumbnail image paths.
     * Ensure these paths are correct and images exist in your project's resources directory.
     */
    private void setupMapThumbnails() {
        mapThumbnails.put(MapType.DARK_FOREST, "/assets/maps/dark_forest_thumbnail.png");
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
        Button button = new Button(mapType.name().replace("_", " ")); // Displays the pretty name
        // Set maximum width/height so button fills its grid cell
        button.setMaxWidth(Double.MAX_VALUE);
        button.setMaxHeight(Double.MAX_VALUE);
        // Bind font size to a percentage of button height for responsiveness
        button.fontProperty().bind(Bindings.createObjectBinding(() ->
                        Font.font("Arial", Math.max(14, button.getHeight() * 0.1)), // Min 14, max 10% of height
                button.heightProperty()));
        button.setTextFill(Color.WHITE);
        button.setContentDisplay(javafx.scene.control.ContentDisplay.BOTTOM); // Place text below the image

        // Load background image
        String imageUrl = mapThumbnails.get(mapType);
        if (imageUrl != null) {
            try {
                // Check if the resource can be found
                if (getClass().getResource(imageUrl) == null) {
                    System.err.println("RESOURCE NOT FOUND: " + imageUrl + " (check path in src/main/resources)");
                }
                Image backgroundImage = new Image(getClass().getResourceAsStream(imageUrl));
                BackgroundImage bi = new BackgroundImage(backgroundImage,
                        BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                        BackgroundPosition.CENTER,
                        // Scale image to cover the button while maintaining aspect ratio
                        new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, true));
                button.setBackground(new Background(bi));
            } catch (Exception e) {
                System.err.println("Error loading image for " + mapType.name() + ": " + imageUrl);
                e.printStackTrace(); // Print full stack trace for more details
                button.setStyle("-fx-background-color: #444444; -fx-background-radius: 5;");
            }
        } else {
            button.setStyle("-fx-background-color: #444444; -fx-background-radius: 5;");
        }

        // Styling and hover effects
        button.setStyle(button.getStyle() + "; -fx-border-color: #555555; -fx-border-width: 2;");
        button.setOnMouseEntered(e -> button.setStyle(button.getStyle().replace("#555555", "#FFFFFF"))); // Highlight border on hover
        button.setOnMouseExited(e -> button.setStyle(button.getStyle().replace("#FFFFFF", "#555555"))); // Restore border on exit

        return button;
    }

    /**
     * Creates a standard menu button with common styling.
     * @param text The text for the button.
     * @return The configured Button.
     */
    private Button createMenuButton(String text) {
        Button button = new Button(text);
        button.setPrefWidth(150); // This will be fixed, but the overall panel scales
        button.setPrefHeight(35);
        button.setFont(Font.font("Arial", 16)); // Will be overridden by binding if added later
        button.setTextFill(Color.WHITE);
        button.setStyle("-fx-background-color: #555555; -fx-background-radius: 5;");
        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: #777777; -fx-background-radius: 5;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: #555555; -fx-background-radius: 5;"));
        return button;
    }

    public void setOnMapSelected(Consumer<MapType> onMapSelected) {
        this.onMapSelected = onMapSelected;
    }

    public void setOnBackAction(Runnable onBackAction) {
        this.onBackAction = onBackAction;
    }
}