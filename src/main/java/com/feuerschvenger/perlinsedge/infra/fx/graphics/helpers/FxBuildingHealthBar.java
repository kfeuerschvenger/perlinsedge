package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.domain.entities.buildings.Building;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * The small health bar over a damaged building. It shows only while the hammer is in hand and hovers the building, and
 * only once the building lost some health, so an intact one stays clean. It is a dark pill with a coloured fill, the
 * shape of the other pills of the interface.
 */
public final class FxBuildingHealthBar {
    /** Width of the bar in art pixels. */
    public static final double WIDTH_ART = 20;
    /** Height of the bar in art pixels. */
    public static final double HEIGHT_ART = 4;
    /** Gap in art pixels between the bar and the top of the building. */
    public static final double GAP_ART = 4;

    private static final Color BACK = Color.rgb(29, 24, 48, 0.85);
    private static final Color GOOD = Color.rgb(118, 176, 76);
    private static final Color WARN = Color.rgb(255, 160, 42);
    private static final Color BAD = Color.rgb(212, 112, 92);
    private static final double WARN_BELOW = 0.6;
    private static final double BAD_BELOW = 0.3;
    /** Number of sides the inset is taken from, one on each side. */
    private static final int INSET_SIDES = 2;

    /** Not instantiable: the class only holds the rule and the drawing call. */
    private FxBuildingHealthBar() {
    }

    /**
     * Tells whether the health bar of a building is shown.
     *
     * @param buildMode whether the hammer is in hand
     * @param hovered   the building under the cursor, or null for none
     * @return true when the hammer hovers a building that lost some health
     */
    public static boolean isShown(boolean buildMode, Building hovered) {
        return buildMode && hovered != null && hovered.isDamaged();
    }

    /**
     * Gives the colour of the fill: green while the building is sound, amber when it is hurt and red when it is about
     * to fall.
     *
     * @param fraction the share of the health left
     * @return the fill colour
     */
    static Color fillFor(double fraction) {
        if (fraction < BAD_BELOW) {
            return BAD;
        }
        if (fraction < WARN_BELOW) {
            return WARN;
        }
        return GOOD;
    }

    /**
     * Draws the bar.
     *
     * @param gc       graphics context
     * @param mark     where the bar floats and how full it is
     * @param artScale screen pixels per art pixel
     */
    public static void draw(GraphicsContext gc, HealthBarMark mark, double artScale) {
        final double width = WIDTH_ART * artScale;
        final double height = HEIGHT_ART * artScale;
        final double x = PixelSnap.round(mark.centerX() - width / 2);
        final double y = PixelSnap.round(mark.topY() - (GAP_ART + HEIGHT_ART) * artScale);
        final double inset = artScale;
        final double fill = Math.clamp(mark.fraction(), 0.0, 1.0) * (width - INSET_SIDES * inset);

        gc.setFill(BACK);
        gc.fillRoundRect(x, y, width, height, height, height);
        if (fill > 0) {
            final double innerHeight = height - INSET_SIDES * inset;
            gc.setFill(fillFor(mark.fraction()));
            gc.fillRoundRect(x + inset, y + inset, fill, innerHeight, innerHeight, innerHeight);
        }
    }
}
