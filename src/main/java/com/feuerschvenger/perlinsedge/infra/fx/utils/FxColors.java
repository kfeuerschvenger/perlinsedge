package com.feuerschvenger.perlinsedge.infra.fx.utils;

import com.feuerschvenger.perlinsedge.domain.utils.Rgba;
import javafx.scene.paint.Color;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Single bridge between the framework-free domain colour ({@link Rgba}) and the JavaFX colour.
 * Conversions are cached so the render loop never allocates a colour per frame.
 */
public final class FxColors {

    private static final Map<Rgba, Color> CACHE = new ConcurrentHashMap<>();

    private FxColors() {
    }

    /**
     * Converts a domain colour to a JavaFX colour.
     *
     * @param rgba the domain colour
     * @return the cached JavaFX colour, or null when the input is null
     */
    public static Color toFx(Rgba rgba) {
        if (rgba == null) {
            return null;
        }
        return CACHE.computeIfAbsent(rgba, c -> new Color(c.r(), c.g(), c.b(), c.a()));
    }

    /**
     * Converts a JavaFX colour to a domain colour.
     *
     * @param color the JavaFX colour
     * @return the domain colour, or null when the input is null
     */
    public static Rgba fromFx(Color color) {
        if (color == null) {
            return null;
        }
        return new Rgba(color.getRed(), color.getGreen(), color.getBlue(), color.getOpacity());
    }
}
