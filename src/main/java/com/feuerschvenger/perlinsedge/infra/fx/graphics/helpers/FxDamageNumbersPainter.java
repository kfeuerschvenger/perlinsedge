package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.domain.utils.DamageNumbers;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.ZoomSteps;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * Draws the floating damage numbers over the world: the pixel font in the colour of what the number reports, with a
 * dark outline so it reads on any ground, rising and fading out as the pool of {@link DamageNumbers} says.
 * It allocates nothing per number: the text is built when the number starts, the colours are constants and the font is
 * kept until the art scale changes.
 */
public final class FxDamageNumbersPainter {

    /** The family the GUI font registers under, which the world text shares. */
    static final String FONT_FAMILY = "Pixelify Sans";
    /** Font size in art pixels: the pixel font is designed on an 8 pixel grid. */
    static final double FONT_ART_PIXELS = 8.0;
    /** Colour of the outline. */
    static final Color OUTLINE = Color.web("#1d1830");

    private static final Color WHITE = Color.web("#fcf4e4");
    private static final Color YELLOW = Color.web("#ffd84a");
    private static final Color RED = Color.web("#e8574f");
    private static final Color GREEN = Color.web("#6ee07c");

    /** Offsets of the outline copies, in art pixels: the eight neighbours of the text. */
    private static final int[][] OUTLINE_OFFSETS = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-1, -1}, {1, -1}, {-1, 1}, {1, 1}
    };
    /** How high above the feet of an entity or the foot of a tile the number starts, in art pixels. */
    private static final double ENTITY_LIFT_ART_PIXELS = 22.0;
    private static final double RESOURCE_LIFT_ART_PIXELS = 26.0;
    /** Margin outside the canvas within which a number is still drawn, in screen pixels. */
    private static final double SCREEN_MARGIN = 64.0;

    private static Font cachedFont;
    private static double cachedFontSize = -1;

    private FxDamageNumbersPainter() {}

    /**
     * Gives the colour of a kind of number.
     *
     * @param kind what the number reports
     * @return white for damage to resources, yellow for damage to enemies, red for damage to the player, green for a heal
     */
    public static Color color(DamageNumbers.Kind kind) {
        return switch (kind) {
            case RESOURCE -> WHITE;
            case ENEMY -> YELLOW;
            case PLAYER -> RED;
            case HEAL -> GREEN;
        };
    }

    /**
     * Gives how high above the foot of the tile a number starts.
     *
     * @param kind what the number reports
     * @return art pixels: a resource such as a tree is taller than an entity, so its number starts higher
     */
    public static double lift(DamageNumbers.Kind kind) {
        return kind == DamageNumbers.Kind.RESOURCE ? RESOURCE_LIFT_ART_PIXELS : ENTITY_LIFT_ART_PIXELS;
    }

    /**
     * Draws every number that is on screen.
     *
     * @param gc           the graphics context of the world canvas, with no transform applied
     * @param numbers      the pool of numbers
     * @param gameTime     the game clock in seconds
     * @param halfTileW    half the tile width in screen pixels
     * @param halfTileH    half the tile height in screen pixels
     * @param offsetX      camera offset X
     * @param offsetY      camera offset Y
     * @param zoom         camera zoom
     * @param canvasWidth  canvas width in pixels
     * @param canvasHeight canvas height in pixels
     */
    public static void draw(GraphicsContext gc, DamageNumbers numbers, double gameTime, double halfTileW,
                            double halfTileH, double offsetX, double offsetY, double zoom, double canvasWidth,
                            double canvasHeight) {
        final double scale = ZoomSteps.integerArtScale(zoom);
        boolean prepared = false;
        for (int i = 0, count = numbers.size(); i < count; i++) {
            final DamageNumbers.Entry entry = numbers.get(i);
            if (!entry.isActive(gameTime)) {
                continue;
            }
            // The foot of the tile is one and a half half-tiles below its top vertex, where entities stand
            final double x = Math.rint((entry.getTileX() - entry.getTileY()) * halfTileW + offsetX
                    + entry.getOffsetX() * scale);
            final double y = Math.rint((entry.getTileX() + entry.getTileY()) * halfTileH + offsetY + halfTileH * 1.5
                    - (lift(entry.getKind()) + entry.rise(gameTime)) * scale);
            if (x < -SCREEN_MARGIN || x > canvasWidth + SCREEN_MARGIN
                    || y < -SCREEN_MARGIN || y > canvasHeight + SCREEN_MARGIN) {
                continue;
            }
            if (!prepared) {
                prepare(gc, scale);
                prepared = true;
            }
            gc.setGlobalAlpha(entry.opacity(gameTime));
            gc.setFill(OUTLINE);
            for (int[] offset : OUTLINE_OFFSETS) {
                gc.fillText(entry.getText(), x + offset[0] * scale, y + offset[1] * scale);
            }
            gc.setFill(color(entry.getKind()));
            gc.fillText(entry.getText(), x, y);
        }
        if (prepared) {
            gc.setGlobalAlpha(1.0);
        }
    }

    private static void prepare(GraphicsContext gc, double scale) {
        final double size = FONT_ART_PIXELS * scale;
        if (cachedFont == null || cachedFontSize != size) {
            cachedFont = Font.font(FONT_FAMILY, size);
            cachedFontSize = size;
        }
        gc.setFont(cachedFont);
        gc.setTextAlign(TextAlignment.CENTER);
    }
}
