package com.feuerschvenger.perlinsedge.ui.view.utils;

import javafx.geometry.Insets;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Utility class for creating standardized grid layouts for inventory displays.
 * Provides methods to create both player inventory grids and flexible container grids.
 */
public final class GridUtils {
    // Layout configuration constants
    private static final int GRID_PADDING = 10;
    private static final int GRID_SPACING = 5;
    private static final Color GRID_BG_COLOR = Color.rgb(50, 50, 50, 0.5);
    private static final int PLAYER_COLS = 6; // Fixed column count for player inventory
    private static final int PLAYER_ROWS = 3; // Fixed row count for player inventory
    private static final int MAX_NON_PLAYER_COLS = 6; // Maximum columns for secondary containers

    // Private constructor to prevent instantiation (utility class)
    private GridUtils() {
        throw new AssertionError("GridUtils is a utility class and should not be instantiated");
    }

    /**
     * Creates a flexible grid for secondary containers (chests, etc.) with dynamic sizing.
     *
     * @param slots List of slot panes to arrange in the grid (can be null)
     * @return Configured GridPane instance
     */
    public static GridPane createGrid(List<StackPane> slots) {
        return createFlexibleGrid(false, Objects.requireNonNullElse(slots, Collections.emptyList()));
    }

    /**
     * Creates a fixed-size grid for player inventory with consistent dimensions.
     *
     * @param slots List of slot panes to arrange in the grid
     * @return Configured GridPane instance
     * @throws NullPointerException if slots is null
     */
    public static GridPane createPlayerGrid(List<StackPane> slots) {
        Objects.requireNonNull(slots, "Slots list cannot be null");
        return createFlexibleGrid(true, slots);
    }

    /**
     * Internal implementation for creating grid layouts with consistent styling.
     *
     * @param isPlayerInventory Whether to use player-specific dimensions
     * @param slots List of slot panes to arrange
     * @return Configured GridPane instance
     */
    private static GridPane createFlexibleGrid(boolean isPlayerInventory, List<StackPane> slots) {
        GridPane grid = initializeBaseGrid();

        // Determine grid dimensions based on inventory type
        int cols = isPlayerInventory ? PLAYER_COLS : calculateOptimalColumns(slots.size());
        int rows = isPlayerInventory ? PLAYER_ROWS : calculateRequiredRows(slots.size(), cols);

        configureGridConstraints(grid, cols, rows);
        populateGridSlots(grid, slots, cols);

        return grid;
    }

    /**
     * Initializes a base grid with consistent styling and spacing.
     */
    private static GridPane initializeBaseGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(GRID_SPACING);
        grid.setVgap(GRID_SPACING);
        grid.setPadding(new Insets(GRID_PADDING));
        grid.setBackground(new Background(new BackgroundFill(GRID_BG_COLOR, new CornerRadii(5), Insets.EMPTY)));
        return grid;
    }

    /**
     * Calculates optimal column count for non-player containers.
     *
     * @param slotCount Number of slots to accommodate
     * @return Optimal column count (capped at MAX_NON_PLAYER_COLS)
     */
    private static int calculateOptimalColumns(int slotCount) {
        int calculatedCols = (int) Math.ceil(Math.sqrt(slotCount));
        return Math.min(calculatedCols, MAX_NON_PLAYER_COLS);
    }

    /**
     * Calculates required rows based on slot count and columns.
     *
     * @param slotCount Number of slots to accommodate
     * @param columns Number of columns in the grid
     * @return Required row count
     */
    private static int calculateRequiredRows(int slotCount, int columns) {
        return (int) Math.ceil((double) slotCount / columns);
    }

    /**
     * Configures row and column constraints for the grid.
     *
     * @param grid GridPane to configure
     * @param cols Number of columns
     * @param rows Number of rows
     */
    private static void configureGridConstraints(GridPane grid, int cols, int rows) {
        // Configure columns with equal growth priority
        for (int i = 0; i < cols; i++) {
            ColumnConstraints colConstraints = new ColumnConstraints();
            colConstraints.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(colConstraints);
        }

        // Configure rows with equal growth priority
        for (int i = 0; i < rows; i++) {
            RowConstraints rowConstraints = new RowConstraints();
            rowConstraints.setVgrow(Priority.ALWAYS);
            grid.getRowConstraints().add(rowConstraints);
        }
    }

    /**
     * Populates the grid with inventory slots.
     *
     * @param grid GridPane to populate
     * @param slots List of slot panes
     * @param cols Number of columns in the grid
     */
    private static void populateGridSlots(GridPane grid, List<StackPane> slots, int cols) {
        for (int i = 0; i < slots.size(); i++) {
            int row = i / cols;
            int col = i % cols;
            grid.add(slots.get(i), col, row);
        }
    }

}