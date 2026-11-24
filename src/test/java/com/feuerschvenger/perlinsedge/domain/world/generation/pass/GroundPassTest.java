package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.TestContexts;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroundPassTest {

    private static final int WIDTH = 16;
    private static final int HEIGHT = 16;
    private static final float NO_NUDGE = 0F;
    private static final float AMPLITUDE = 0.05F;
    private static final float TOLERANCE = 1e-6F;
    private static final float BOUNDARY = 0.2F;
    private static final float ABOVE_BOUNDARY = 0.1F;
    private static final float CLIMATE = 0.5F;
    private static final float LOW_CLIMATE = 0.1F;
    private static final float HIGH_CLIMATE = 0.9F;
    private static final float NEGATIVE_AMPLITUDE = -0.1F;
    private static final int LIFTED_X = 3;
    private static final int LIFTED_Y = 4;
    private static final int CLIMATE_X = 2;
    private static final int CLIMATE_Y = 2;
    private static final int NEXT_ROW = CLIMATE_Y + 1;
    private static final long NEXT_SEED = 1L;

    /** A rule that returns hills above the boundary and sand below, and reads no climate. */
    private static final GroundRule SPLIT = GroundRule.ofHeight(
            (float height, float temperature, float moisture) -> height >= BOUNDARY ? TileType.HILL : TileType.SAND);

    private static final Set<NoiseField> BOTH_CLIMATES = Set.of(NoiseField.TEMPERATURE, NoiseField.MOISTURE);

    private final GenerationContext context = TestContexts.of(WIDTH, HEIGHT);

    private static void fillHeight(GenerationContext target, float height) {
        Arrays.fill(target.defineField(NoiseField.HEIGHT), height);
    }

    private static void fillClimate(GenerationContext target) {
        Arrays.fill(target.defineField(NoiseField.TEMPERATURE), CLIMATE);
        Arrays.fill(target.defineField(NoiseField.MOISTURE), CLIMATE);
    }

    private void fillFields(float height) {
        fillHeight(context, height);
        fillClimate(context);
    }

    @Test
    void withoutDitherEveryTileIsClassifiedByItsOwnValues() {
        fillFields(0F);
        context.field(NoiseField.HEIGHT)[context.index(LIFTED_X, LIFTED_Y)] = BOUNDARY + ABOVE_BOUNDARY;

        new GroundPass(new GroundParams(SPLIT, NO_NUDGE)).apply(context);

        assertEquals(TileType.HILL, context.groundAt(context.index(LIFTED_X, LIFTED_Y)));
        assertEquals(TileType.SAND, context.groundAt(context.index(0, 0)));
        assertTrue(Arrays.stream(context.ground()).noneMatch(Objects::isNull));
    }

    @Test
    void theRuleSeesTheClimateOfTheSameTile() {
        fillFields(0F);
        context.field(NoiseField.TEMPERATURE)[context.index(CLIMATE_X, CLIMATE_Y)] = LOW_CLIMATE;
        context.field(NoiseField.MOISTURE)[context.index(CLIMATE_X, CLIMATE_Y)] = HIGH_CLIMATE;
        final var byClimate = new GroundRule(BOTH_CLIMATES, (float height, float temperature, float moisture) ->
                temperature < moisture ? TileType.SWAMP : TileType.GRASS);

        new GroundPass(new GroundParams(byClimate, NO_NUDGE)).apply(context);

        assertEquals(TileType.SWAMP, context.groundAt(context.index(CLIMATE_X, CLIMATE_Y)));
        assertEquals(TileType.GRASS, context.groundAt(context.index(CLIMATE_X, NEXT_ROW)));
    }

    @Test
    void aRuleThatReadsNoClimateRunsOnAContextWithoutClimateFieldsAndGetsTheNeutralValue() {
        fillHeight(context, 0F);
        final var seen = new float[2];
        final var recorder = GroundRule.ofHeight((float height, float temperature, float moisture) -> {
            seen[0] = temperature;
            seen[1] = moisture;
            return TileType.SAND;
        });

        new GroundPass(new GroundParams(recorder, NO_NUDGE)).apply(context);

        assertFalse(context.hasField(NoiseField.TEMPERATURE));
        assertFalse(context.hasField(NoiseField.MOISTURE));
        assertEquals(GroundPass.NEUTRAL_CLIMATE, seen[0]);
        assertEquals(GroundPass.NEUTRAL_CLIMATE, seen[1]);
        assertEquals(TileType.SAND, context.groundAt(0));
    }

    @Test
    void aRuleThatReadsOneClimateFieldNeedsOnlyThatField() {
        fillHeight(context, 0F);
        Arrays.fill(context.defineField(NoiseField.MOISTURE), HIGH_CLIMATE);
        final var wet = new GroundRule(Set.of(NoiseField.MOISTURE), (float height, float temperature, float moisture) ->
                moisture > CLIMATE ? TileType.SWAMP : TileType.GRASS);

        new GroundPass(new GroundParams(wet, NO_NUDGE)).apply(context);

        assertEquals(TileType.SWAMP, context.groundAt(0));
    }

    @Test
    void ditherNudgesTheHeightByAtMostTheAmplitudeAndNeverWritesItBack() {
        fillFields(BOUNDARY);
        final var largestNudge = new float[1];
        final var recorder = GroundRule.ofHeight((float height, float temperature, float moisture) -> {
            largestNudge[0] = Math.max(largestNudge[0], Math.abs(height - BOUNDARY));
            return TileType.SAND;
        });

        new GroundPass(new GroundParams(recorder, AMPLITUDE)).apply(context);

        assertTrue(largestNudge[0] <= AMPLITUDE + TOLERANCE, "nudge " + largestNudge[0]);
        assertTrue(largestNudge[0] > 0F);
        for (var value : context.field(NoiseField.HEIGHT)) {
            assertEquals(BOUNDARY, value);
        }
    }

    @Test
    void ditherSplitsTilesThatSitOnTheBoundaryAndRepeatsForTheSameSeed() {
        fillFields(BOUNDARY);
        new GroundPass(new GroundParams(SPLIT, AMPLITUDE)).apply(context);
        final var first = context.ground();
        final var hills = Arrays.stream(first).filter((TileType type) -> type == TileType.HILL).count();

        final var again = TestContexts.of(WIDTH, HEIGHT);
        fillHeight(again, BOUNDARY);
        new GroundPass(new GroundParams(SPLIT, AMPLITUDE)).apply(again);

        assertTrue(hills > 0 && hills < first.length, "hills " + hills + " of " + first.length);
        assertArrayEquals(first, again.ground());
    }

    @Test
    void theDitherDependsOnTheSeed() {
        fillFields(BOUNDARY);
        new GroundPass(new GroundParams(SPLIT, AMPLITUDE)).apply(context);
        final var other = TestContexts.of(context.type(), context.seed() + NEXT_SEED, WIDTH, HEIGHT,
                TestContexts.FLAT);
        fillHeight(other, BOUNDARY);

        new GroundPass(new GroundParams(SPLIT, AMPLITUDE)).apply(other);

        assertFalse(Arrays.equals(context.ground(), other.ground()));
    }

    @Test
    void aMissingFieldTheRuleReadsIsReportedByName() {
        fillHeight(context, 0F);
        final var byClimate = new GroundRule(BOTH_CLIMATES, (float height, float temperature, float moisture) ->
                TileType.SAND);
        final var pass = new GroundPass(new GroundParams(byClimate, NO_NUDGE));

        final var error = assertThrows(IllegalStateException.class, () -> pass.apply(context));

        assertTrue(error.getMessage().contains("TEMPERATURE"));
    }

    @Test
    void aMissingHeightFieldIsReportedByName() {
        final var pass = new GroundPass(new GroundParams(SPLIT, NO_NUDGE));

        final var error = assertThrows(IllegalStateException.class, () -> pass.apply(context));

        assertTrue(error.getMessage().contains("HEIGHT"));
    }

    @Test
    void ruleRefusesAFieldThatIsNotClimateAndAMissingClassifier() {
        final var heightField = Set.of(NoiseField.HEIGHT);

        assertThrows(IllegalArgumentException.class, () -> new GroundRule(heightField,
                (float height, float temperature, float moisture) -> TileType.SAND));
        assertThrows(NullPointerException.class, () -> GroundRule.ofHeight(null));
    }

    @Test
    void paramsRefuseAMissingRuleAndABadAmplitude() {
        assertThrows(NullPointerException.class, () -> new GroundParams(null, NO_NUDGE));
        assertThrows(IllegalArgumentException.class, () -> new GroundParams(SPLIT, NEGATIVE_AMPLITUDE));
        assertThrows(IllegalArgumentException.class, () -> new GroundParams(SPLIT, Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> new GroundParams(SPLIT, Float.POSITIVE_INFINITY));
    }

    @Test
    void namesItselfForTheTimings() {
        assertEquals("ground", new GroundPass(new GroundParams(SPLIT, NO_NUDGE)).name());
    }
}
