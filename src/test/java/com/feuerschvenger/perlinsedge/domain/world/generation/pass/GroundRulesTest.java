package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/** Pins the thresholds of each rule with one sample on each side of every boundary. */
class GroundRulesTest {

    private static final float DRY = 0.1F;
    private static final float WET = 0.9F;
    private static final float COLD = 0.1F;
    private static final float WARM = 0.9F;
    private static final float ANY = 0.5F;
    private static final int SWEEP_STEPS = 100;

    @ParameterizedTest
    @CsvSource({
            "-0.45, DEEP_WATER", "-0.35, SHALLOW_WATER", "-0.15, GRASS", "0.25, GRASS", "0.35, GRASS",
            "0.65, GRASS", "0.75, MOUNTAIN", "0.95, MOUNTAIN"})
    void denseForestIsGrassBelowTheMountainsAndNeverForestOrRockyGround(float height, TileType expected) {
        assertEquals(expected, GroundRules.DENSE_FOREST.classify(height, COLD, DRY));
        assertEquals(expected, GroundRules.DENSE_FOREST.classify(height, WARM, WET));
    }

    @ParameterizedTest
    @CsvSource({
            "-0.45, 0.5, DEEP_WATER", "-0.15, 0.5, SHALLOW_WATER", "-0.05, 0.9, SWAMP", "-0.05, 0.1, GRASS",
            "0.25, 0.9, GRASS", "0.35, 0.9, GRASS", "0.9, 0.1, GRASS"})
    void swampHasWideShallowsAndSwampOnlyInTheWetLowland(float height, float moisture, TileType expected) {
        assertEquals(expected, GroundRules.SWAMP.classify(height, ANY, moisture));
    }

    @ParameterizedTest
    @CsvSource({
            "-0.65, DEEP_WATER", "-0.45, SHALLOW_WATER", "-0.35, SAND", "-0.25, SAND", "-0.15, GRASS",
            "0.25, GRASS", "0.35, GRASS", "0.65, GRASS", "0.75, MOUNTAIN"})
    void islandsHaveASandShoreAndIgnoreTheClimate(float height, TileType expected) {
        assertEquals(expected, GroundRules.ISLANDS.classify(height, COLD, DRY));
        assertEquals(expected, GroundRules.ISLANDS.classify(height, WARM, WET));
    }

    @ParameterizedTest
    @CsvSource({"-0.5, DEEP_WATER", "-0.45, DEEP_WATER", "-0.35, SHALLOW_WATER", "-0.25, SHALLOW_WATER",
            "-0.15, SAND", "0.1, SAND", "0.14, SAND", "0.16, SAND", "0.19, SAND", "0.21, SAND", "0.25, SAND",
            "0.55, SAND", "0.65, SAND", "0.95, SAND"})
    void desertHasASeaAtTheBottomOfTheRangeAndNoInlandWater(float height, TileType expected) {
        assertEquals(expected, GroundRules.DESERT.classify(height, WARM, DRY));
    }

    @ParameterizedTest
    @CsvSource({
            "-0.5, 0.1, DEEP_WATER", "-0.5, 0.9, DEEP_WATER", "-0.3, 0.1, ICE", "-0.3, 0.25, ICE",
            "-0.3, 0.35, SHALLOW_WATER", "-0.3, 0.9, SHALLOW_WATER", "-0.1, 0.9, ICE", "0.1, 0.1, SNOW",
            "0.1, 0.4, SNOW", "0.1, 0.5, GRASS", "0.1, 0.9, GRASS", "0.3, 0.4, SNOW", "0.3, 0.45, SNOW",
            "0.3, 0.55, GRASS", "0.3, 0.8, GRASS", "0.45, 0.8, GRASS", "0.6, 0.8, SNOW"})
    void tundraFreezesTheShallowsAndTheCoastAndKeepsDeepWaterOpen(float height, float temperature,
                                                                  TileType expected) {
        assertEquals(expected, GroundRules.TUNDRA.classify(height, temperature, WET));
        assertEquals(expected, GroundRules.TUNDRA.classify(height, temperature, DRY));
    }

    @Test
    void noRuleEmitsForestOrRockyGroundWhateverTheHeightAndClimate() {
        final var rules = new GroundRule[] {GroundRules.DENSE_FOREST, GroundRules.SWAMP, GroundRules.ISLANDS,
            GroundRules.DESERT, GroundRules.TUNDRA, GroundRules.RIVERS, GroundRules.MEDITERRANEAN,
            GroundRules.MOUNTAINOUS};
        for (var rule : rules) {
            for (var step = -SWEEP_STEPS; step <= SWEEP_STEPS; step++) {
                final var height = (float) step / SWEEP_STEPS;
                for (var climate : new float[] {DRY, ANY, WET}) {
                    final var type = rule.classify(height, climate, climate);
                    assertNotEquals(TileType.FOREST, type, "forest at " + height);
                    assertNotEquals(TileType.ROCKY_GROUND, type, "rocky ground at " + height);
                }
            }
        }
    }

    @Test
    void everyRuleDeclaresTheClimateFieldsItsClassificationReads() {
        assertEquals(Set.of(NoiseField.TEMPERATURE), GroundRules.TUNDRA.climateFields());
        assertEquals(Set.of(), GroundRules.DENSE_FOREST.climateFields());
        assertEquals(Set.of(NoiseField.MOISTURE), GroundRules.SWAMP.climateFields());
        assertEquals(Set.of(NoiseField.MOISTURE), GroundRules.RIVERS.climateFields());
        assertEquals(Set.of(), GroundRules.ISLANDS.climateFields());
        assertEquals(Set.of(), GroundRules.DESERT.climateFields());
        assertEquals(Set.of(), GroundRules.MEDITERRANEAN.climateFields());
        assertEquals(Set.of(), GroundRules.MOUNTAINOUS.climateFields());
    }

    @ParameterizedTest
    @CsvSource({
            "-0.65, 0.5, DEEP_WATER", "-0.55, 0.5, SHALLOW_WATER", "-0.3, 0.5, GRASS", "-0.1, 0.5, GRASS",
            "-0.05, 0.5, GRASS", "-0.02, 0.5, SAND", "-0.01, 0.9, SAND", "0.1, 0.9, SWAMP",
            "0.1, 0.1, GRASS", "0.35, 0.9, GRASS", "0.25, 0.1, GRASS", "0.9, 0.1, GRASS"})
    void riversKeepDeepWaterForTheBottomAThinBeachAndSwampForTheWetLowland(float height, float moisture, TileType expected) {
        assertEquals(expected, GroundRules.RIVERS.classify(height, ANY, moisture));
    }

    @ParameterizedTest
    @CsvSource({"-0.45, DEEP_WATER", "-0.3, SHALLOW_WATER", "-0.1, SAND", "0.1, GRASS", "0.55, GRASS",
            "0.65, GRASS"})
    void mediterraneanHasASandShoreAndGrassAbove(float height, TileType expected) {
        assertEquals(expected, GroundRules.MEDITERRANEAN.classify(height, ANY, ANY));
    }

    @ParameterizedTest
    @CsvSource({"-0.45, DEEP_WATER", "-0.35, SHALLOW_WATER", "-0.25, SHALLOW_WATER", "-0.15, DEEP_WATER",
            "0.0, DEEP_WATER", "0.25, SHALLOW_WATER", "0.35, GRASS", "0.45, GRASS", "0.95, GRASS"})
    void mountainousHasASeaAtTheBottomAndItsWaterHighBetweenThePeaks(float height, TileType expected) {
        assertEquals(expected, GroundRules.MOUNTAINOUS.classify(height, ANY, ANY));
    }
}
