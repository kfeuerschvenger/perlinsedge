package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Building;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Door;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Draws every building type procedurally with isometric projection.
 * The domain model stays free of JavaFX, so the shapes live here and are selected by {@link Building#getType()}.
 */
public final class FxBuildingPainter {

    private static final double WALL_HEIGHT_FACTOR = 4.0;
    private static final double WALL_ELEVATION_OFFSET_FACTOR = 2.0;
    private static final double WALL_STROKE_WIDTH = 1.5;
    private static final Color BASE_WALL_COLOR = Color.rgb(100, 100, 100);
    private static final Color WALL_TOP_COLOR = BASE_WALL_COLOR.deriveColor(0, 1, 1.2, 1);
    private static final Color WALL_RIGHT_COLOR = BASE_WALL_COLOR.deriveColor(0, 1, 0.8, 1);
    private static final Color WALL_LEFT_COLOR = BASE_WALL_COLOR.deriveColor(0, 1, 0.9, 1);

    private static final double CHEST_SIZE_FACTOR = 0.6;
    private static final double CHEST_VISUAL_HEIGHT_FACTOR = 1.2;
    private static final double CHEST_ELEVATION_OFFSET_FACTOR = 0.3;
    private static final double CHEST_LOCK_SIZE_FACTOR = 0.4;
    private static final Color CHEST_BROWN = Color.rgb(139, 69, 19);
    private static final Color CHEST_LID_COLOR = CHEST_BROWN.deriveColor(0, 1, 1.2, 1);
    private static final Color CHEST_RIGHT_COLOR = CHEST_BROWN.deriveColor(0, 1, 0.8, 1);
    private static final Color CHEST_LEFT_COLOR = CHEST_BROWN.deriveColor(0, 1, 0.9, 1);

    private FxBuildingPainter() {
    }

    /**
     * Draws a building at the given screen position.
     *
     * @param gc      graphics context
     * @param building the building to draw
     * @param screenX  screen X of the tile origin
     * @param screenY  screen Y of the tile origin
     * @param zoom     current zoom level
     */
    public static void draw(GraphicsContext gc, Building building, double screenX, double screenY, double zoom) {
        switch (building.getType()) {
            case WALL -> drawWall(gc, screenX, screenY, zoom);
            case DOOR -> drawDoor(gc, building instanceof Door door && door.isOpen(), screenX, screenY, zoom);
            case BRIDGE -> drawBridge(gc, screenX, screenY, zoom);
            case DOCK -> drawDock(gc, screenX, screenY, zoom);
            case FENCE -> drawFence(gc, screenX, screenY, zoom);
            case CHEST -> drawChest(gc, screenX, screenY, zoom);
            case FURNACE -> drawFurnace(gc, screenX, screenY, zoom);
            case ANVIL -> drawAnvil(gc, screenX, screenY, zoom);
            case WORKBENCH -> drawWorkbench(gc, screenX, screenY, zoom);
            case CAMPFIRE -> drawCampfire(gc, screenX, screenY, zoom);
            case STANDING_TORCH -> drawStandingTorch(gc, screenX, screenY, zoom);
            case BED -> drawBed(gc, screenX, screenY, zoom);
        }
    }

    private static void drawStandingTorch(GraphicsContext gc, double screenX, double screenY, double zoom) {
        double postWidth = 6 * zoom;
        double postHeight = 34 * zoom;
        double flameHeight = 16 * zoom;
        double flameWidth = 12 * zoom;
        double postTopY = screenY - postHeight;

        gc.setFill(Color.SADDLEBROWN);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(0.5 * zoom);
        gc.fillRect(screenX - postWidth / 2, postTopY, postWidth, postHeight);
        gc.strokeRect(screenX - postWidth / 2, postTopY, postWidth, postHeight);

        gc.setFill(Color.ORANGE);
        gc.fillPolygon(new double[]{
                screenX, screenX + flameWidth / 2, screenX, screenX - flameWidth / 2
        }, new double[]{
                postTopY - flameHeight, postTopY - flameHeight * 0.3, postTopY + 2 * zoom, postTopY - flameHeight * 0.3
        }, 4);
        gc.setFill(Color.YELLOW);
        gc.fillPolygon(new double[]{
                screenX, screenX + flameWidth / 4, screenX, screenX - flameWidth / 4
        }, new double[]{
                postTopY - flameHeight * 0.65, postTopY - flameHeight * 0.2, postTopY, postTopY - flameHeight * 0.2
        }, 4);
    }

    private static void drawBridge(GraphicsContext gc, double screenX, double screenY, double zoom) {
        double length = 80 * zoom;
        double width = 30 * zoom;
        double thickness = 10 * zoom;

        double drawY = screenY - thickness / 2;

        gc.setFill(Color.SADDLEBROWN);
        gc.setStroke(Color.DARKGOLDENROD);
        gc.setLineWidth(1 * zoom);

        gc.fillRect(screenX - length / 2, drawY - width / 2, length, width);
        gc.strokeRect(screenX - length / 2, drawY - width / 2, length, width);

        double plankSpacing = 15 * zoom;
        for (double i = 0; i < length; i += plankSpacing) {
            gc.strokeLine(screenX - length / 2 + i, drawY - width / 2, screenX - length / 2 + i, drawY + width / 2);
        }
    }

    private static void drawDoor(GraphicsContext gc, boolean isOpen, double screenX, double screenY, double zoom) {
        double width = 40 * zoom;
        double height = 60 * zoom;

        double drawY = screenY - height / 2 - (10 * zoom);

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1 * zoom);

        if (isOpen) {
            gc.setFill(Color.web("#ADD8E6", 0.5));
            gc.fillRect(screenX - width / 2, drawY, width, height);
            gc.setStroke(Color.BROWN);
            gc.strokeRect(screenX - width / 2, drawY, width, height);
        } else {
            gc.setFill(Color.BROWN);
            gc.fillRect(screenX - width / 2, drawY, width, height);
            gc.strokeRect(screenX - width / 2, drawY, width, height);
            gc.setFill(Color.GOLD);
            gc.fillOval(screenX + width / 2 - (10 * zoom), drawY + height / 2 - (5 * zoom), 5 * zoom, 5 * zoom);
        }
    }

    private static void drawDock(GraphicsContext gc, double screenX, double screenY, double zoom) {
        double width = 60 * zoom;
        double height = 40 * zoom;
        double plankThickness = 8 * zoom;

        double drawY = screenY - height / 2;

        gc.setFill(Color.PERU);
        gc.setStroke(Color.BROWN);
        gc.setLineWidth(1 * zoom);

        gc.fillRect(screenX - width / 2, drawY, width, height);
        gc.strokeRect(screenX - width / 2, drawY, width, height);

        for (double i = 0; i < height; i += plankThickness + (2 * zoom)) {
            gc.strokeLine(screenX - width / 2, drawY + i, screenX + width / 2, drawY + i);
        }

        double postSize = 10 * zoom;
        double postHeight = 15 * zoom;
        gc.setFill(Color.DARKGOLDENROD);
        gc.fillRect(screenX - width / 2 + postSize / 2, drawY + height - postHeight, postSize, postHeight);
        gc.fillRect(screenX + width / 2 - postSize * 1.5, drawY + height - postHeight, postSize, postHeight);
    }

    private static void drawFence(GraphicsContext gc, double screenX, double screenY, double zoom) {
        double postWidth = 10 * zoom;
        double postHeight = 40 * zoom;
        double barHeight = 10 * zoom;
        double totalWidth = 50 * zoom;

        double drawY = screenY - postHeight;

        gc.setFill(Color.DARKGOLDENROD);
        gc.setStroke(Color.BROWN);
        gc.setLineWidth(1 * zoom);

        gc.fillRect(screenX - totalWidth / 2, drawY, postWidth, postHeight);
        gc.strokeRect(screenX - totalWidth / 2, drawY, postWidth, postHeight);

        gc.fillRect(screenX + totalWidth / 2 - postWidth, drawY, postWidth, postHeight);
        gc.strokeRect(screenX + totalWidth / 2 - postWidth, drawY, postWidth, postHeight);

        double barY = drawY + postHeight * 0.3;
        gc.fillRect(screenX - totalWidth / 2, barY, totalWidth, barHeight);
        gc.strokeRect(screenX - totalWidth / 2, barY, totalWidth, barHeight);

        double lowerBarY = drawY + postHeight * 0.7;
        gc.fillRect(screenX - totalWidth / 2, lowerBarY, totalWidth, barHeight);
        gc.strokeRect(screenX - totalWidth / 2, lowerBarY, totalWidth, barHeight);
    }

    private static void drawFurnace(GraphicsContext gc, double screenX, double screenY, double zoom) {
        double size = 50 * zoom;
        double openingWidth = 20 * zoom;
        double openingHeight = 15 * zoom;

        double drawY = screenY - size / 2;

        gc.setFill(Color.DARKGRAY);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1 * zoom);
        gc.fillRect(screenX - size / 2, drawY, size, size);
        gc.strokeRect(screenX - size / 2, drawY, size, size);

        gc.setFill(Color.ORANGERED);
        gc.fillRect(screenX - openingWidth / 2, drawY + (size * 0.6), openingWidth, openingHeight);

        gc.setFill(Color.YELLOW.deriveColor(0, 1, 1, 0.7));
        gc.fillOval(screenX - openingWidth / 4, drawY + (size * 0.6) + (openingHeight * 0.2), openingWidth / 2, openingHeight / 2);
    }

    private static void drawAnvil(GraphicsContext gc, double screenX, double screenY, double zoom) {
        double baseWidth = 40 * zoom;
        double baseHeight = 15 * zoom;
        double bodyWidth = 25 * zoom;
        double bodyHeight = 20 * zoom;
        double topWidth = 35 * zoom;
        double topHeight = 10 * zoom;

        gc.setFill(Color.DARKGRAY.darker());
        gc.fillRect(screenX - baseWidth / 2, screenY - baseHeight, baseWidth, baseHeight);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(0.5 * zoom);
        gc.strokeRect(screenX - baseWidth / 2, screenY - baseHeight, baseWidth, baseHeight);

        gc.setFill(Color.DARKGRAY);
        gc.fillRect(screenX - bodyWidth / 2, screenY - baseHeight - bodyHeight, bodyWidth, bodyHeight);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(0.5 * zoom);
        gc.strokeRect(screenX - bodyWidth / 2, screenY - baseHeight - bodyHeight, bodyWidth, bodyHeight);

        gc.setFill(Color.GRAY);
        gc.fillRoundRect(screenX - topWidth / 2, screenY - baseHeight - bodyHeight - topHeight, topWidth, topHeight, 5 * zoom, 5 * zoom);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(0.5 * zoom);
        gc.strokeRoundRect(screenX - topWidth / 2, screenY - baseHeight - bodyHeight - topHeight, topWidth, topHeight, 5 * zoom, 5 * zoom);
    }

    private static void drawWorkbench(GraphicsContext gc, double screenX, double screenY, double zoom) {
        double tableWidth = 60 * zoom;
        double tableHeight = 10 * zoom;
        double legWidth = 8 * zoom;
        double legHeight = 30 * zoom;

        gc.setFill(Color.SADDLEBROWN);
        gc.fillRect(screenX - tableWidth / 2, screenY - tableHeight - legHeight, tableWidth, tableHeight);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(0.5 * zoom);
        gc.strokeRect(screenX - tableWidth / 2, screenY - tableHeight - legHeight, tableWidth, tableHeight);

        gc.setFill(Color.BROWN);
        gc.fillRect(screenX - tableWidth / 2 + (legWidth / 2), screenY - legHeight, legWidth, legHeight);
        gc.fillRect(screenX + tableWidth / 2 - (legWidth * 1.5), screenY - legHeight, legWidth, legHeight);
        gc.fillRect(screenX - (legWidth / 2), screenY - legHeight + (legWidth * 0.7), legWidth, legHeight);
        gc.fillRect(screenX + tableWidth / 2 - (legWidth / 2) - (legWidth * 0.7), screenY - legHeight + (legWidth * 0.7), legWidth, legHeight);

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(0.5 * zoom);
        gc.strokeRect(screenX - tableWidth / 2 + (legWidth / 2), screenY - legHeight, legWidth, legHeight);
        gc.strokeRect(screenX + tableWidth / 2 - (legWidth * 1.5), screenY - legHeight, legWidth, legHeight);
    }

    private static void drawCampfire(GraphicsContext gc, double screenX, double screenY, double zoom) {
        double baseWidth = 30 * zoom;
        double baseHeight = 10 * zoom;
        double fireHeight = 25 * zoom;
        double woodHeight = 10 * zoom;

        gc.setFill(Color.BROWN.darker().darker());
        gc.fillOval(screenX - baseWidth / 2, screenY - baseHeight / 2 + woodHeight, baseWidth, baseHeight);

        gc.setFill(Color.ORANGE);
        gc.fillPolygon(new double[]{
                screenX, screenX + baseWidth / 4, screenX + baseWidth / 8, screenX - baseWidth / 8, screenX - baseWidth / 4
        }, new double[]{
                screenY - fireHeight, screenY - baseHeight / 2, screenY - baseHeight / 4, screenY - baseHeight / 4, screenY - baseHeight / 2
        }, 5);
        gc.setFill(Color.YELLOW);
        gc.fillPolygon(new double[]{
                screenX, screenX + baseWidth / 8, screenX - baseWidth / 8
        }, new double[]{
                screenY - fireHeight * 0.7, screenY - baseHeight / 2, screenY - baseHeight / 2
        }, 3);

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(0.5 * zoom);
        gc.strokeOval(screenX - baseWidth / 2, screenY - baseHeight / 2 + woodHeight, baseWidth, baseHeight);
    }

    private static void drawBed(GraphicsContext gc, double screenX, double screenY, double zoom) {
        double bedWidth = 50 * zoom;
        double bedLength = 70 * zoom;
        double headboardHeight = 25 * zoom;
        double headboardWidth = bedWidth * 0.8;

        double drawX = screenX;
        double drawY = screenY + (AppConfig.getInstance().graphics().getTileHalfHeight() * zoom);

        gc.setFill(Color.PERU);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1 * zoom);

        gc.fillRect(drawX - bedWidth / 2, drawY - bedLength, bedWidth, bedLength);
        gc.strokeRect(drawX - bedWidth / 2, drawY - bedLength, bedWidth, bedLength);

        gc.setFill(Color.LIGHTGRAY.deriveColor(0, 1, 1, 0.8));
        gc.fillRect(drawX - bedWidth / 2 + (2 * zoom), drawY - bedLength + (2 * zoom), bedWidth - (4 * zoom), bedLength - (4 * zoom));
        gc.strokeRect(drawX - bedWidth / 2 + (2 * zoom), drawY - bedLength + (2 * zoom), bedWidth - (4 * zoom), bedLength - (4 * zoom));

        gc.setFill(Color.SADDLEBROWN);
        gc.fillRect(drawX - headboardWidth / 2, drawY - bedLength - headboardHeight, headboardWidth, headboardHeight);
        gc.strokeRect(drawX - headboardWidth / 2, drawY - bedLength - headboardHeight, headboardWidth, headboardHeight);
    }

    // ------------------------------------------------------------------
    //  Wall
    // ------------------------------------------------------------------

    private static void drawWall(GraphicsContext gc, double screenX, double screenY, double zoom) {
        final AppConfig config = AppConfig.getInstance();

        final double halfWidth = config.graphics().getTileHalfWidth() * zoom;
        final double halfHeight = config.graphics().getTileHalfHeight() * zoom;
        final double tileBaseHeight = config.graphics().getTileVisualBaseHeight() * zoom;

        final double elevationOffset = halfHeight * WALL_ELEVATION_OFFSET_FACTOR;
        final double topY = screenY - elevationOffset;
        final double wallVisualHeight = tileBaseHeight * WALL_HEIGHT_FACTOR;

        final double rightX = screenX + halfWidth;
        final double rightY = topY + halfHeight;
        final double bottomY = topY + 2 * halfHeight;
        final double leftX = screenX - halfWidth;
        final double leftY = topY + halfHeight;
        final double baseRightY = rightY + wallVisualHeight;
        final double baseBottomY = bottomY + wallVisualHeight;
        final double baseLeftY = leftY + wallVisualHeight;

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(WALL_STROKE_WIDTH * zoom);

        gc.setFill(WALL_LEFT_COLOR);
        gc.beginPath();
        gc.moveTo(leftX, leftY);
        gc.lineTo(leftX, baseLeftY);
        gc.lineTo(screenX, baseBottomY);
        gc.lineTo(screenX, bottomY);
        gc.closePath();
        gc.fill();
        gc.stroke();

        gc.setFill(WALL_RIGHT_COLOR);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(WALL_STROKE_WIDTH * zoom);
        gc.beginPath();
        gc.moveTo(rightX, rightY);
        gc.lineTo(rightX, baseRightY);
        gc.lineTo(screenX, baseBottomY);
        gc.lineTo(screenX, bottomY);
        gc.closePath();
        gc.fill();
        gc.stroke();

        gc.setFill(WALL_TOP_COLOR);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(WALL_STROKE_WIDTH * zoom);
        gc.beginPath();
        gc.moveTo(screenX, topY);
        gc.lineTo(rightX, rightY);
        gc.lineTo(screenX, bottomY);
        gc.lineTo(leftX, leftY);
        gc.closePath();
        gc.fill();
        gc.stroke();
    }

    // ------------------------------------------------------------------
    //  Chest
    // ------------------------------------------------------------------

    /**
     * Vertical positions of the diamond that forms the lid of a chest.
     *
     * @param topY    Y of the back vertex
     * @param sideY   Y of the left and right vertices
     * @param bottomY Y of the front vertex
     */
    record ChestLid(double topY, double sideY, double bottomY) {
    }

    /**
     * Computes the lid diamond: the back vertex sits at the top of the chest, the front vertex one chest tile
     * height lower, and the side vertices halfway, so the lid has a real surface.
     *
     * @param chestTopY      Y of the top of the chest
     * @param chestHalfHeight half the height of the chest footprint
     * @return the lid vertices
     */
    static ChestLid chestLid(double chestTopY, double chestHalfHeight) {
        return new ChestLid(chestTopY, chestTopY + chestHalfHeight, chestTopY + 2 * chestHalfHeight);
    }

    private static void drawChest(GraphicsContext gc, double screenX, double screenY, double zoom) {
        final AppConfig config = AppConfig.getInstance();

        final double halfWidth = config.graphics().getTileHalfWidth() * zoom;
        final double halfHeight = config.graphics().getTileHalfHeight() * zoom;

        final double chestHalfWidth = halfWidth * CHEST_SIZE_FACTOR;
        final double chestHalfHeight = halfHeight * CHEST_SIZE_FACTOR;

        final double elevationOffset = halfHeight * CHEST_ELEVATION_OFFSET_FACTOR;
        final double chestTopY = screenY - elevationOffset;
        final double chestBodyHeight = chestHalfHeight * CHEST_VISUAL_HEIGHT_FACTOR;

        final ChestLid lid = chestLid(chestTopY, chestHalfHeight);
        final double lidRightX = screenX + chestHalfWidth;
        final double lidRightY = lid.sideY();
        final double lidBottomY = lid.bottomY();
        final double lidLeftX = screenX - chestHalfWidth;
        final double lidLeftY = lid.sideY();
        final double lidTopY = lid.topY();

        final double bodyBaseRightY = lidRightY + chestBodyHeight;
        final double bodyBaseBottomY = lidBottomY + chestBodyHeight;
        final double bodyBaseLeftY = lidLeftY + chestBodyHeight;

        final double lockWidth = chestHalfWidth * CHEST_LOCK_SIZE_FACTOR;
        final double lockHeight = chestHalfHeight * CHEST_LOCK_SIZE_FACTOR;
        final double lockX = screenX - lockWidth / 2;
        final double lockY = chestTopY + chestHalfHeight - lockHeight / 2;

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.0 * zoom);

        gc.setFill(CHEST_LEFT_COLOR);
        gc.beginPath();
        gc.moveTo(lidLeftX, lidLeftY);
        gc.lineTo(lidLeftX, bodyBaseLeftY);
        gc.lineTo(screenX, bodyBaseBottomY);
        gc.lineTo(screenX, lidBottomY);
        gc.closePath();
        gc.fill();
        gc.stroke();

        gc.setFill(CHEST_RIGHT_COLOR);
        gc.beginPath();
        gc.moveTo(lidRightX, lidRightY);
        gc.lineTo(lidRightX, bodyBaseRightY);
        gc.lineTo(screenX, bodyBaseBottomY);
        gc.lineTo(screenX, lidBottomY);
        gc.closePath();
        gc.fill();
        gc.stroke();

        gc.setFill(CHEST_LID_COLOR);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.0 * zoom);
        gc.beginPath();
        gc.moveTo(screenX, lidTopY);
        gc.lineTo(lidRightX, lidRightY);
        gc.lineTo(screenX, lidBottomY);
        gc.lineTo(lidLeftX, lidLeftY);
        gc.closePath();
        gc.fill();
        gc.stroke();

        gc.setFill(Color.DARKGRAY);
        gc.fillOval(lockX, lockY, lockWidth, lockHeight);
    }
}
