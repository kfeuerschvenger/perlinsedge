package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.domain.entities.items.DroppedItem;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.ZoomSteps;
import com.feuerschvenger.perlinsedge.infra.fx.utils.SpriteManager;
import com.feuerschvenger.perlinsedge.infra.fx.utils.ThrowArc;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * Provides optimized rendering for items both as dropped entities and in UI components.
 * Features animation support, fallback rendering, and efficient resource handling.
 */
public final class FxItemDrawingHelper {
    // Rendering constants
    public static final double FLOATING_AMPLITUDE_PIXELS = 5.0;
    private static final double DROPPED_ITEM_TARGET_TILE_FRACTION = 0.6;
    private static final double DROPPED_ITEM_BASE_VERTICAL_OFFSET = 0.5;
    private static final double FLOATING_SPEED = 3.0;
    private static final double QUANTITY_TEXT_STROKE_WIDTH = 0.5;
    private static final double QUANTITY_TEXT_MARGIN = 5.0;

    // Fallback shape constants
    private static final double FALLBACK_MARGIN = 0.1;
    private static final double TOOL_HANDLE_WIDTH_FACTOR = 0.2;
    private static final double TOOL_HANDLE_HEIGHT_FACTOR = 0.8;
    private static final double OVAL_MARGIN = 0.15;
    /** Sides a margin or an inset is taken from, one on each side. */
    private static final int INSET_SIDES = 2;
    /** Fraction of a box that reaches its middle. */
    private static final double HALF = 0.5;
    /** Share of the box width and height a crystal cluster spans on each side of its centre. */
    private static final double CRYSTAL_EXTENT = 0.4;
    /** Share of the crystal width at which the two lower corners sit on each side of the centre. */
    private static final double CRYSTAL_BASE_HALF_WIDTH = 0.5;

    // Wood log and plank shapes, as fractions of the box
    private static final double LOG_TOP = 0.2;
    private static final double LOG_HEIGHT = 0.6;
    private static final double PLANK_LEFT = 0.05;
    private static final double PLANK_TOP = 0.25;
    private static final double PLANK_WIDTH = 0.9;
    private static final double PLANK_HEIGHT = 0.5;

    // Tool handle position, as fractions of the box
    private static final double TOOL_HANDLE_LEFT = 0.4;
    private static final double TOOL_HANDLE_TOP = 0.1;

    // Default shape, as fractions of the box
    private static final double DEFAULT_SHAPE_INSET = 0.2;
    private static final double DEFAULT_SHAPE_SIZE = 0.6;

    // Cached instances
    private static final Font QUANTITY_FONT = Font.font("Arial", FontWeight.BOLD, 10);
    private static final Color QUANTITY_TEXT_COLOR = Color.WHITE;
    private static final Color QUANTITY_TEXT_STROKE_COLOR = Color.BLACK;
    private static final AppConfig CONFIG = AppConfig.getInstance();
    private static final SpriteManager SPRITE_MANAGER = SpriteManager.getInstance();

    private FxItemDrawingHelper() {}

    /**
     * Renders a dropped item with floating animation and quantity display.
     *
     * @param gc GraphicsContext to render on
     * @param tileTopX Tile's top vertex X coordinate
     * @param tileTopY Tile's top vertex Y coordinate
     * @param zoom Current zoom factor
     * @param droppedItem Item to render
     * @param gameTime Current game time for animation
     */
    public static void drawDroppedItem(GraphicsContext gc, double tileTopX, double tileTopY,
                                       double zoom, DroppedItem droppedItem, double gameTime) {
        // Pre-calculate tile dimensions
        final double tileWidth = CONFIG.graphics().getTileWidth() * zoom;
        final double tileHeight = CONFIG.graphics().getTileHeight() * zoom;

        // Calculate floating animation offset; a thrown item is lifted along its arc on top of the floating
        final double flightLift = ThrowArc.liftPixels(droppedItem.flightProgress(gameTime), zoom);
        final double floatingOffset = Math.sin(gameTime * FLOATING_SPEED) * FLOATING_AMPLITUDE_PIXELS * zoom
                - flightLift;

        // Calcula el tamaño objetivo para el ítem basado en el tile y el zoom
        // Usamos el ancho del tile como referencia para el tamaño máximo
        double targetItemWidth = tileWidth * DROPPED_ITEM_TARGET_TILE_FRACTION;
        double targetItemHeight = tileHeight * DROPPED_ITEM_TARGET_TILE_FRACTION; // Puedes ajustar si quieres que el alto sea diferente

        // Calculate initial position (usando el tamaño del tile para el offset vertical)
        double baseDrawY = tileTopY + (tileHeight * DROPPED_ITEM_BASE_VERTICAL_OFFSET);

        // Get sprite and render
        final var itemType = droppedItem.getItemType();
        Image sprite = getItemSprite(itemType);

        double finalDrawX;
        double finalDrawY;
        double finalWidth;
        double finalHeight;

        if (sprite != null) {
            // The icon is drawn at the integer art scale on whole pixels, so its pixels stay crisp
            final double scale = ZoomSteps.integerArtScale(zoom);
            finalWidth = sprite.getWidth() * scale;
            finalHeight = sprite.getHeight() * scale;

            finalDrawX = PixelSnap.round(tileTopX - (finalWidth * HALF));
            // The icon floats over the tile, bobbing on whole pixels
            finalDrawY = PixelSnap.round(baseDrawY - finalHeight + floatingOffset
                    + (SPRITE_MANAGER.getItemSprite(itemType).getVerticalOffset() * zoom));

            gc.drawImage(sprite, finalDrawX, finalDrawY, finalWidth, finalHeight);
        } else {
            // Si no hay sprite, dibuja la forma de fallback con el tamaño objetivo
            finalWidth = targetItemWidth;
            finalHeight = targetItemHeight;
            finalDrawX = tileTopX - (finalWidth * HALF);
            finalDrawY = baseDrawY - finalHeight + floatingOffset; // Sin offset del sprite para fallback

            drawFallbackShape(gc, finalDrawX, finalDrawY, finalWidth, finalHeight, itemType, zoom);
        }

        // Render quantity if needed
        final int quantity = droppedItem.getQuantity();
        if (quantity > 1) {
            renderQuantityText(gc, tileTopX, finalDrawY + finalHeight, zoom, quantity);
        }
    }

    /**
     * Renders fallback shapes for missing item sprites.
     *
     * @param gc GraphicsContext to render on
     * @param x Top-left X coordinate
     * @param y Top-left Y coordinate
     * @param width Rendering width
     * @param height Rendering height
     * @param itemType Type of item to render
     * @param zoom Current zoom factor
     */
    public static void drawFallbackShape(GraphicsContext gc, double x, double y,
                                         double width, double height, ItemType itemType, double zoom) {
        gc.save();

        // Pre-calculate common dimensions
        final double margin = width * FALLBACK_MARGIN;
        final double centerX = x + (width / 2);
        final double centerY = y + (height / 2);
        final double lineWidth = 1 * zoom;

        // Set common stroke properties
        gc.setLineWidth(lineWidth);

        // Render based on item type
        switch (itemType) {
            case WOOD_LOG -> drawWoodLog(gc, x, y, width, height, margin);
            case WOOD_PLANK -> drawWoodPlank(gc, x, y, width, height);
            case STONE -> drawStone(gc, x, y, width, height, margin);
            case CRYSTALS -> drawCrystals(gc, centerX, centerY, width * CRYSTAL_EXTENT, height * CRYSTAL_EXTENT);
            case SLIME_BALL -> drawSlimeBall(gc, x, y, width, height);
            case PICKAXE -> drawTool(gc, x, y, width, height, Tool.PICKAXE);
            case AXE -> drawTool(gc, x, y, width, height, Tool.AXE);
            case SWORD -> drawTool(gc, x, y, width, height, Tool.SWORD);
            case HAMMER -> drawTool(gc, x, y, width, height, Tool.HAMMER);
            default -> drawDefaultShape(gc, x, y, width, height);
        }

        gc.restore();
    }

    // ==================================================================
    //  PRIVATE RENDERING HELPERS
    // ==================================================================

    private static Image getItemSprite(ItemType itemType) {
        SpriteManager.SpriteMetadata meta = SPRITE_MANAGER.getItemSprite(itemType);
        return (meta != null) ? meta.getImage() : null;
    }

    private static void renderQuantityText(GraphicsContext gc, double centerX, double baseY, double zoom, int quantity) {
        final var text = String.valueOf(quantity);
        double textY = baseY + (QUANTITY_TEXT_MARGIN * zoom);

        gc.setFont(QUANTITY_FONT);
        gc.setTextAlign(TextAlignment.CENTER);

        // Draw text outline
        gc.setStroke(QUANTITY_TEXT_STROKE_COLOR);
        gc.setLineWidth(QUANTITY_TEXT_STROKE_WIDTH);
        gc.strokeText(text, centerX, textY);

        // Draw main text
        gc.setFill(QUANTITY_TEXT_COLOR);
        gc.fillText(text, centerX, textY);
    }

    private static void drawWoodLog(GraphicsContext gc, double x, double y, double width, double height, double margin) {
        double rectX = x + margin;
        double rectY = y + height * LOG_TOP;
        double rectWidth = width - (INSET_SIDES * margin);
        double rectHeight = height * LOG_HEIGHT;

        gc.setFill(Color.BROWN.darker());
        gc.fillRect(rectX, rectY, rectWidth, rectHeight);

        gc.setStroke(Color.DARKGOLDENROD);
        gc.strokeRect(rectX, rectY, rectWidth, rectHeight);
    }

    private static void drawWoodPlank(GraphicsContext gc, double x, double y, double width, double height) {
        double rectX = x + width * PLANK_LEFT;
        double rectY = y + height * PLANK_TOP;
        double rectWidth = width * PLANK_WIDTH;
        double rectHeight = height * PLANK_HEIGHT;

        gc.setFill(Color.BROWN.brighter());
        gc.fillRect(rectX, rectY, rectWidth, rectHeight);

        gc.setStroke(Color.DARKGOLDENROD.darker());
        gc.strokeRect(rectX, rectY, rectWidth, rectHeight);
    }

    private static void drawStone(GraphicsContext gc, double x, double y, double width, double height, double margin) {
        double ovalX = x + margin;
        double ovalY = y + margin;
        double ovalWidth = width - (INSET_SIDES * margin);
        double ovalHeight = height - (INSET_SIDES * margin);

        gc.setFill(Color.GRAY.darker());
        gc.fillOval(ovalX, ovalY, ovalWidth, ovalHeight);

        gc.setStroke(Color.DARKGRAY);
        gc.strokeOval(ovalX, ovalY, ovalWidth, ovalHeight);
    }

    private static void drawCrystals(GraphicsContext gc, double centerX, double centerY, double width, double height) {
        double[] pointsX = {
                centerX,
                centerX + width,
                centerX + width * CRYSTAL_BASE_HALF_WIDTH,
                centerX - width * CRYSTAL_BASE_HALF_WIDTH,
                centerX - width
        };

        double[] pointsY = {
                centerY - height,
                centerY,
                centerY + height,
                centerY + height,
                centerY
        };

        gc.setFill(Color.CYAN.brighter());
        gc.fillPolygon(pointsX, pointsY, pointsX.length);

        gc.setStroke(Color.BLUEVIOLET);
        gc.strokePolygon(pointsX, pointsY, pointsX.length);
    }

    private static void drawSlimeBall(GraphicsContext gc, double x, double y, double width, double height) {
        double ovalX = x + (width * OVAL_MARGIN);
        double ovalY = y + (height * OVAL_MARGIN);
        double ovalWidth = width * (1 - (INSET_SIDES * OVAL_MARGIN));
        double ovalHeight = height * (1 - (INSET_SIDES * OVAL_MARGIN));

        gc.setFill(Color.LIMEGREEN.brighter());
        gc.fillOval(ovalX, ovalY, ovalWidth, ovalHeight);

        gc.setStroke(Color.DARKGREEN);
        gc.strokeOval(ovalX, ovalY, ovalWidth, ovalHeight);
    }

    /** The items drawn as a handle with a head. The head switch covers every constant, so a new tool cannot go undrawn. */
    private enum Tool { PICKAXE, AXE, SWORD, HAMMER }

    private static void drawTool(GraphicsContext gc, double x, double y, double width, double height, Tool tool) {
        // Draw handle
        double handleX = x + (width * TOOL_HANDLE_LEFT);
        double handleY = y + (height * TOOL_HANDLE_TOP);
        double handleWidth = width * TOOL_HANDLE_WIDTH_FACTOR;
        double handleHeight = height * TOOL_HANDLE_HEIGHT_FACTOR;

        gc.setFill(Color.SILVER);
        gc.fillRect(handleX, handleY, handleWidth, handleHeight);
        gc.setStroke(Color.DARKGRAY);
        gc.strokeRect(handleX, handleY, handleWidth, handleHeight);

        // Draw tool head
        gc.setFill(Color.GRAY.darker());
        double headCenterX = x + (width * HALF);

        // A switch expression has to cover every constant, which a switch statement does not
        final double[][] head = switch (tool) {
            case PICKAXE -> new double[][]{
                    {x + width * 0.1, x + width * 0.9, x + width * 0.9, x + width * 0.1},
                    {y + height * 0.2, y + height * 0.2, y + height * 0.4, y + height * 0.4}};
            case AXE -> new double[][]{
                    {x + width * 0.3, x + width * 0.7, x + width * 0.9, x + width * 0.1},
                    {y + height * 0.2, y + height * 0.2, y + height * 0.4, y + height * 0.4}};
            case HAMMER -> new double[][]{
                    {x + width * 0.2, x + width * 0.8, x + width * 0.8, x + width * 0.2},
                    {y + height * 0.15, y + height * 0.15, y + height * 0.4, y + height * 0.4}};
            case SWORD -> new double[][]{
                    {headCenterX - width * 0.05, headCenterX + width * 0.05, headCenterX},
                    {y + height * 0.05, y + height * 0.05, y + height * 0.4}};
        };
        gc.fillPolygon(head[0], head[1], head[0].length);
        gc.strokePolygon(head[0], head[1], head[0].length);
    }

    private static void drawDefaultShape(GraphicsContext gc, double x, double y, double width, double height) {
        double rectX = x + (width * DEFAULT_SHAPE_INSET);
        double rectY = y + (height * DEFAULT_SHAPE_INSET);
        double rectWidth = width * DEFAULT_SHAPE_SIZE;
        double rectHeight = height * DEFAULT_SHAPE_SIZE;

        gc.setFill(Color.DARKGRAY);
        gc.fillRect(rectX, rectY, rectWidth, rectHeight);

        gc.setStroke(Color.LIGHTGRAY);
        gc.strokeRect(rectX, rectY, rectWidth, rectHeight);
    }

}
