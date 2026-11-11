package com.feuerschvenger.perlinsedge.infra.fx.utils;

import com.feuerschvenger.perlinsedge.domain.utils.Rgba;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FxColorsTest {

    private static final double DELTA = 1e-9;
    private static final double FLOAT_DELTA = 1e-6;

    @Test
    void toFxKeepsAllComponents() {
        Color fx = FxColors.toFx(new Rgba(0.1, 0.2, 0.3, 0.4));

        assertEquals(0.1, fx.getRed(), FLOAT_DELTA);
        assertEquals(0.2, fx.getGreen(), FLOAT_DELTA);
        assertEquals(0.3, fx.getBlue(), FLOAT_DELTA);
        assertEquals(0.4, fx.getOpacity(), FLOAT_DELTA);
    }

    @Test
    void toFxCachesEqualColors() {
        assertSame(FxColors.toFx(Rgba.rgb(20, 40, 80)), FxColors.toFx(Rgba.rgb(20, 40, 80)));
    }

    @Test
    void nullMapsToNullBothWays() {
        assertNull(FxColors.toFx(null));
        assertNull(FxColors.fromFx(null));
    }

    @Test
    void fromFxRoundTrips() {
        Rgba original = new Rgba(0.25, 0.5, 0.75, 1.0);
        assertEquals(original, FxColors.fromFx(FxColors.toFx(original)));
    }

    @Test
    void rgbMatchesFxRgb() {
        Color expected = Color.rgb(100, 150, 200);
        Color actual = FxColors.toFx(Rgba.rgb(100, 150, 200));

        assertEquals(expected.getRed(), actual.getRed(), DELTA);
        assertEquals(expected.getGreen(), actual.getGreen(), DELTA);
        assertEquals(expected.getBlue(), actual.getBlue(), DELTA);
    }

    @Test
    void darkerAndBrighterMatchJavaFxBehaviour() {
        assertSameColor(Color.BROWN.darker(), FxColors.toFx(Rgba.BROWN.darker()));
        assertSameColor(Color.BROWN.brighter(), FxColors.toFx(Rgba.BROWN.brighter()));
        assertSameColor(Color.WHITE.brighter(), FxColors.toFx(new Rgba(1, 1, 1, 1).brighter()));
        assertSameColor(Color.BLACK.brighter(), FxColors.toFx(new Rgba(0, 0, 0, 1).brighter()));
    }

    @Test
    void scaleBrightnessMatchesDeriveColor() {
        assertSameColor(Color.LIGHTGREEN.deriveColor(0, 1.0, 0.8, 1.0),
                FxColors.toFx(Rgba.LIGHT_GREEN.scaleBrightness(0.8)));
        assertSameColor(Color.LIGHTPINK.deriveColor(0, 1.0, 0.8, 1.0),
                FxColors.toFx(Rgba.LIGHT_PINK.scaleBrightness(0.8)));
    }

    @Test
    void opacityFactorMatchesDeriveColor() {
        assertSameColor(Color.RED.deriveColor(0, 1, 1, 0.7), FxColors.toFx(Rgba.RED.withOpacityFactor(0.7)));
    }

    /** Every public {@link Rgba} named constant, so a newly added constant is covered automatically. */
    static Stream<String> namedConstants() {
        return Arrays.stream(Rgba.class.getFields())
                .filter(f -> Modifier.isStatic(f.getModifiers()) && f.getType() == Rgba.class)
                .map(Field::getName);
    }

    @ParameterizedTest(name = "{0} matches the JavaFX palette")
    @MethodSource("namedConstants")
    void namedConstantMatchesJavaFxPalette(String name) throws ReflectiveOperationException {
        Rgba constant = (Rgba) Rgba.class.getField(name).get(null);
        Color expected = (Color) Color.class.getField(name.replace("_", "")).get(null);

        assertSameColor(expected, FxColors.toFx(constant));
    }

    @Test
    void namedConstantsAreDiscovered() {
        assertTrue(namedConstants().count() >= 15, "reflection found fewer constants than expected");
    }

    @Test
    void outOfRangeComponentsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Rgba(1.1, 0, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new Rgba(0, -0.1, 0, 1));
    }

    private static void assertSameColor(Color expected, Color actual) {
        assertEquals(expected.getRed(), actual.getRed(), 1e-6, "red");
        assertEquals(expected.getGreen(), actual.getGreen(), 1e-6, "green");
        assertEquals(expected.getBlue(), actual.getBlue(), 1e-6, "blue");
        assertEquals(expected.getOpacity(), actual.getOpacity(), 1e-6, "opacity");
    }
}
