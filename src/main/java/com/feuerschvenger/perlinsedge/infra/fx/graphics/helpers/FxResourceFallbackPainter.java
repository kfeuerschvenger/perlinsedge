package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * Draws a pixel-art style stand-in for a harvestable resource when its sprite image is missing,
 * so every resource that blocks movement is also visible.
 * Each shape is a short list of coloured blocks on a small grid, anchored at the middle of the tile base.
 */
public final class FxResourceFallbackPainter {

    /** Size of one grid cell in screen pixels at zoom 1. */
    static final double CELL = 2.0;
    /** Distance from the top vertex of a tile to its base, matching where sprites are anchored. */
    private static final double BASE_NUDGE = 4.0;

    /**
     * One rectangle of a fallback shape, in grid cells.
     *
     * @param x     left edge relative to the tile centre, in cells
     * @param y     top edge relative to the base, in cells, negative values are above the base
     * @param width width in cells
     * @param height height in cells
     * @param color fill colour
     */
    record Block(int x, int y, int width, int height, Color color) {
    }

    private static final Color TRUNK = Color.web("#6b4423");
    private static final Color LEAVES_DARK = Color.web("#2e6b30");
    private static final Color LEAVES_MID = Color.web("#3a8a3c");
    private static final Color LEAVES_LIGHT = Color.web("#4fa84f");

    private static final Color ROCK_DARK = Color.web("#7d7d7d");
    private static final Color ROCK_MID = Color.web("#9a9a9a");
    private static final Color ROCK_LIGHT = Color.web("#b5b5b5");

    private static final Color CRYSTAL_DARK = Color.web("#6fd3e8");
    private static final Color CRYSTAL_LIGHT = Color.web("#9be9f7");
    private static final Color CRYSTAL_GLINT = Color.web("#e8fbff");

    private static final Color IRON_DARK = Color.web("#6b6b73");
    private static final Color IRON_MID = Color.web("#85858f");
    private static final Color IRON_FLECK = Color.web("#c9824a");
    private static final Color IRON_FLECK_LIGHT = Color.web("#e0a060");

    private static final List<Block> TREE = List.of(
            new Block(-1, -5, 2, 5, TRUNK),
            new Block(-5, -8, 10, 4, LEAVES_DARK),
            new Block(-4, -12, 8, 4, LEAVES_MID),
            new Block(-2, -15, 4, 3, LEAVES_LIGHT));

    private static final List<Block> STONE = List.of(
            new Block(-5, -4, 10, 4, ROCK_DARK),
            new Block(-4, -7, 8, 3, ROCK_MID),
            new Block(-2, -8, 3, 1, ROCK_LIGHT));

    private static final List<Block> CRYSTALS = List.of(
            new Block(-4, -6, 2, 6, CRYSTAL_DARK),
            new Block(-1, -10, 3, 10, CRYSTAL_LIGHT),
            new Block(2, -5, 2, 5, CRYSTAL_DARK),
            new Block(0, -9, 1, 4, CRYSTAL_GLINT));

    private static final List<Block> IRON_ORE = List.of(
            new Block(-5, -4, 10, 4, IRON_DARK),
            new Block(-4, -7, 8, 3, IRON_MID),
            new Block(-3, -5, 1, 1, IRON_FLECK),
            new Block(1, -6, 2, 1, IRON_FLECK),
            new Block(-1, -3, 1, 1, IRON_FLECK_LIGHT),
            new Block(3, -4, 1, 1, IRON_FLECK));

    private FxResourceFallbackPainter() {
    }

    /**
     * Returns the blocks that make up the fallback shape of a resource.
     *
     * @param type the resource type
     * @return the blocks, never empty
     */
    static List<Block> blocksFor(ResourceType type) {
        return switch (type) {
            case TREE -> TREE;
            case STONE -> STONE;
            case CRYSTALS -> CRYSTALS;
            case IRON_ORE -> IRON_ORE;
        };
    }

    /**
     * Draws the fallback shape of a resource on a tile.
     *
     * @param gc      graphics context
     * @param screenX screen X of the tile centre
     * @param screenY screen Y of the top vertex of the tile
     * @param zoom    current zoom level
     * @param type    the resource to draw
     * @param halfTileHeight half the zoomed tile height
     */
    public static void draw(GraphicsContext gc, double screenX, double screenY, double zoom,
                            ResourceType type, double halfTileHeight) {
        double baseY = screenY + halfTileHeight + BASE_NUDGE;
        double unit = CELL * zoom;

        gc.save();
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(Math.max(0.5, 0.5 * zoom));
        for (Block block : blocksFor(type)) {
            double x = screenX + block.x() * unit;
            double y = baseY + block.y() * unit;
            double width = block.width() * unit;
            double height = block.height() * unit;
            gc.setFill(block.color());
            gc.fillRect(x, y, width, height);
            gc.strokeRect(x, y, width, height);
        }
        gc.restore();
    }
}
