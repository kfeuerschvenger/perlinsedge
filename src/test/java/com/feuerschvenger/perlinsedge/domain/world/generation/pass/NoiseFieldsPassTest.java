package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.FractalKind;
import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseSettings;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseSourceFactory;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.TestContexts;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoiseFieldsPassTest {

    private static final int WIDTH = 5;
    private static final int HEIGHT = 3;
    private static final long SEED = 100L;
    private static final float TOLERANCE = 1e-6F;
    private static final float TENTH_DIVISOR = 10F;
    private static final float SECOND_COLUMN_VALUE = 0.1F;
    private static final float SECOND_ROW_VALUE = -0.1F;
    private static final float RANGE_SPAN = 2F;
    private static final float RANGE_OFFSET = 1F;
    private static final long MOISTURE_SEED_OFFSET = 2L;
    private static final int CHECKED_X = 4;
    private static final int CHECKED_Y = 2;
    private static final int TWO_FIELDS = 2;
    private static final NoiseSettings HEIGHT_SETTINGS = new NoiseSettings(0.02F, 4, FractalKind.FBM, 0.4F, 2F);
    private static final NoiseSettings PLAIN_SETTINGS = new NoiseSettings(0.05F, 0, FractalKind.NONE, 0F, 2F);

    /** Seeds and settings the factory was asked for, and how often each source was sampled. */
    private final Map<Long, NoiseSettings> created = new HashMap<>();
    private final Map<Long, Integer> samples = new HashMap<>();

    /** A factory whose source returns {@code x / 10 - y / 10}, so every tile has a distinct known value. */
    private final NoiseSourceFactory recording = (long seed, NoiseSettings settings) -> {
        created.put(seed, settings);
        return (float x, float y) -> {
            samples.merge(seed, 1, Integer::sum);
            return x / TENTH_DIVISOR - y / TENTH_DIVISOR;
        };
    };

    private GenerationContext context() {
        return TestContexts.of(MapType.DESERT, SEED, WIDTH, HEIGHT, recording);
    }

    @Test
    void computesOnlyTheFieldsOfTheParams() {
        final var context = context();
        final var fields = new EnumMap<NoiseField, NoiseSettings>(NoiseField.class);
        fields.put(NoiseField.HEIGHT, HEIGHT_SETTINGS);
        fields.put(NoiseField.STONE, PLAIN_SETTINGS);

        new NoiseFieldsPass(new NoiseFieldsParams(fields)).apply(context);

        assertTrue(context.hasField(NoiseField.HEIGHT));
        assertTrue(context.hasField(NoiseField.STONE));
        assertFalse(context.hasField(NoiseField.TEMPERATURE));
        assertFalse(context.hasField(NoiseField.TREE));
        assertEquals(TWO_FIELDS, created.size());
    }

    @Test
    void everyFieldGetsItsOwnSeedAndItsOwnSettings() {
        final var fields = new EnumMap<NoiseField, NoiseSettings>(NoiseField.class);
        fields.put(NoiseField.HEIGHT, HEIGHT_SETTINGS);
        fields.put(NoiseField.MOISTURE, PLAIN_SETTINGS);

        new NoiseFieldsPass(new NoiseFieldsParams(fields)).apply(context());

        assertSame(HEIGHT_SETTINGS, created.get(SEED));
        assertSame(PLAIN_SETTINGS, created.get(SEED + MOISTURE_SEED_OFFSET));
    }

    @Test
    void heightKeepsTheRangeOfTheNoiseAndTheOtherFieldsAreBroughtToZeroToOne() {
        final var context = context();
        final var fields = new EnumMap<NoiseField, NoiseSettings>(NoiseField.class);
        fields.put(NoiseField.HEIGHT, HEIGHT_SETTINGS);
        fields.put(NoiseField.TEMPERATURE, PLAIN_SETTINGS);

        new NoiseFieldsPass(new NoiseFieldsParams(fields)).apply(context);

        final var raw = CHECKED_X / TENTH_DIVISOR - CHECKED_Y / TENTH_DIVISOR;
        assertEquals(raw, context.field(NoiseField.HEIGHT)[context.index(CHECKED_X, CHECKED_Y)], TOLERANCE);
        assertEquals((raw + RANGE_OFFSET) / RANGE_SPAN,
                context.field(NoiseField.TEMPERATURE)[context.index(CHECKED_X, CHECKED_Y)], TOLERANCE);
    }

    @Test
    void sampleEveryTileOfEveryFieldExactlyOnceAtItsOwnCoordinates() {
        final var context = context();
        final var fields = new EnumMap<NoiseField, NoiseSettings>(NoiseField.class);
        fields.put(NoiseField.HEIGHT, HEIGHT_SETTINGS);

        new NoiseFieldsPass(new NoiseFieldsParams(fields)).apply(context);

        assertEquals(WIDTH * HEIGHT, samples.get(SEED));
        assertEquals(0F, context.field(NoiseField.HEIGHT)[context.index(0, 0)], TOLERANCE);
        assertEquals(SECOND_COLUMN_VALUE, context.field(NoiseField.HEIGHT)[context.index(1, 0)], TOLERANCE);
        assertEquals(SECOND_ROW_VALUE, context.field(NoiseField.HEIGHT)[context.index(0, 1)], TOLERANCE);
    }

    @Test
    void aFieldComputedTwiceIsRefused() {
        final var context = context();
        final var pass = new NoiseFieldsPass(new NoiseFieldsParams(Map.of(NoiseField.HEIGHT, HEIGHT_SETTINGS)));
        pass.apply(context);

        assertThrows(IllegalStateException.class, () -> pass.apply(context));
    }

    @Test
    void theParamsCopyTheMapAndRefuseMissingValues() {
        final var source = new EnumMap<NoiseField, NoiseSettings>(NoiseField.class);
        source.put(NoiseField.HEIGHT, HEIGHT_SETTINGS);
        final var params = new NoiseFieldsParams(source);
        source.put(NoiseField.TREE, PLAIN_SETTINGS);
        final var withNullValue = new EnumMap<NoiseField, NoiseSettings>(NoiseField.class);
        withNullValue.put(NoiseField.HEIGHT, null);
        final var withNullKey = Collections.singletonMap((NoiseField) null, HEIGHT_SETTINGS);
        final var copied = params.fields();

        assertEquals(List.of(NoiseField.HEIGHT), List.copyOf(copied.keySet()));
        assertThrows(UnsupportedOperationException.class, copied::clear);
        assertThrows(NullPointerException.class, () -> new NoiseFieldsParams(withNullValue));
        assertThrows(NullPointerException.class, () -> new NoiseFieldsParams(withNullKey));
        assertThrows(NullPointerException.class, () -> new NoiseFieldsPass(null));
    }

    @Test
    void namesItselfForTheTimings() {
        assertEquals("noise-fields", new NoiseFieldsPass(new NoiseFieldsParams(Map.of())).name());
    }
}
