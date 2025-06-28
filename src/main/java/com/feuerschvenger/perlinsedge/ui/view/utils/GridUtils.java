package com.feuerschvenger.perlinsedge.ui.view.utils;

import javafx.geometry.Insets;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class GridUtils {
    private static final int GRID_PADDING = 10;
    private static final int GRID_SPACING = 5;

    private static final Color GRID_BG_COLOR = Color.rgb(50, 50, 50, 0.5);

    private static final int PLAYER_COLS = 6; // Columnas fijas para jugador
    private static final int PLAYER_ROWS = 3; // Filas fijas para jugador

    public static GridPane createGrid(List<StackPane> slots) {
        return createFlexibleGrid(false, Objects.requireNonNullElseGet(slots, ArrayList::new));
    }

    public static GridPane createPlayerGrid(List<StackPane> slots) {
        return createFlexibleGrid(true, slots);
    }

    private static GridPane createFlexibleGrid(boolean isPlayerInventory, List<StackPane> slots) {
        GridPane grid = new GridPane();
        grid.setHgap(GRID_SPACING);
        grid.setVgap(GRID_SPACING);
        grid.setPadding(new Insets(GRID_PADDING));
        grid.setBackground(new Background(new BackgroundFill(GRID_BG_COLOR, new CornerRadii(5), Insets.EMPTY)));

        // Determinar dimensiones
        int cols, rows;
        if (isPlayerInventory) {
            cols = PLAYER_COLS;
            rows = PLAYER_ROWS;
        } else {
            // Para contenedores secundarios, calcular dimensiones óptimas
            int capacity = slots.size();
            cols = (int) Math.ceil(Math.sqrt(capacity));
            cols = Math.min(cols, 6); // Máximo 6 columnas
            rows = (int) Math.ceil((double) capacity / cols);
        }

        // Configurar columnas
        for (int i = 0; i < cols; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(col);
        }

        // Configurar filas
        for (int i = 0; i < rows; i++) {
            RowConstraints row = new RowConstraints();
            row.setVgrow(Priority.ALWAYS);
            grid.getRowConstraints().add(row);
        }

        // Añadir slots al grid
        for (int i = 0; i < slots.size(); i++) {
            int row = i / cols;
            int col = i % cols;
            grid.add(slots.get(i), col, row);
        }

        return grid;
    }

}
