package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Pins the thresholds of each rule with one sample on each side of every boundary. */
class GroundRulesTest {

    private static final float DRY = 0.1F;
    private static final float WET = 0.9F;
    private static final float COLD = 0.1F;
    private static final float WARM = 0.9F;
    private static final float ANY = 0.5F;

    @ParameterizedTest
    @CsvSource({
            "-0.45, 0.5, DEEP_WATER", "-0.35, 0.5, SHALLOW_WATER", "-0.15, 0.1, GRASS", "-0.15, 0.9, FOREST",
            "0.25, 0.1, GRASS", "0.35, 0.1, HILL", "0.55, 0.9, FOREST", "0.65, 0.9, HILL", "0.65, 0.1, HILL",
            "0.75, 0.1, MOUNTAIN", "0.75, 0.9, MOUNTAIN"})
    void denseForestBandsFollowTheHeightAndWetGroundBelowTheHillsIsForest(float height, float moisture,
                                                                         TileType expected) {
        assertEquals(expected, GroundRules.DENSE_FOREST.classify(height, ANY, moisture));
    }

    @ParameterizedTest
    @CsvSource({
            "-0.45, 0.5, DEEP_WATER", "-0.15, 0.5, SHALLOW_WATER", "-0.05, 0.9, SWAMP", "-0.05, 0.1, GRASS",
            "0.25, 0.9, GRASS", "0.35, 0.9, FOREST", "0.9, 0.1, FOREST"})
    void swampHasWideShallowsAndSwampOnlyInTheWetLowland(float height, float moisture, TileType expected) {
        assertEquals(expected, GroundRules.SWAMP.classify(height, ANY, moisture));
    }

    @ParameterizedTest
    @CsvSource({
            "-0.65, DEEP_WATER", "-0.45, SHALLOW_WATER", "-0.35, SAND", "-0.25, SAND", "-0.15, GRASS",
            "0.25, GRASS", "0.35, HILL", "0.65, HILL", "0.75, MOUNTAIN"})
    void islandsHaveASandShoreAndIgnoreTheClimate(float height, TileType expected) {
        assertEquals(expected, GroundRules.ISLANDS.classify(height, COLD, DRY));
        assertEquals(expected, GroundRules.ISLANDS.classify(height, WARM, WET));
    }

    @ParameterizedTest
    @CsvSource({"-0.5, DEEP_WATER", "0.1, DEEP_WATER", "0.17, SHALLOW_WATER", "0.25, SAND", "0.55, SAND",
            "0.65, HILL", "0.95, HILL"})
    void desertHasItsWaterAtTheTopOfTheRange(float height, TileType expected) {
        assertEquals(expected, GroundRules.DESERT.classify(height, WARM, DRY));
    }

    @ParameterizedTest
    @CsvSource({
            "-0.5, 0.1, DEEP_WATER", "-0.5, 0.9, DEEP_WATER", "-0.3, 0.1, ICE", "-0.3, 0.9, SHALLOW_WATER",
            "-0.1, 0.9, ICE", "0.1, 0.1, SNOW", "0.1, 0.9, GRASS", "0.3, 0.4, SNOW", "0.3, 0.8, FOREST",
            "0.45, 0.8, GRASS", "0.6, 0.8, SNOW"})
    void tundraFreezesTheShallowsAndTheCoastAndKeepsDeepWaterOpen(float height, float temperature,
                                                                  TileType expected) {
        assertEquals(expected, GroundRules.TUNDRA.classify(height, temperature, WET));
    }

    @Test
    void tundraInlandForestNeedsMoistureAndTheWarmGroundBelowTheSnowHeight() {
        assertEquals(TileType.FOREST, GroundRules.TUNDRA.classify(0.3F, WARM, WET));
        assertEquals(TileType.GRASS, GroundRules.TUNDRA.classify(0.3F, WARM, DRY));
        assertEquals(TileType.GRASS, GroundRules.TUNDRA.classify(0.45F, WARM, WET));
    }

    @ParameterizedTest
    @CsvSource({
            "-0.65, 0.5, DEEP_WATER", "-0.55, 0.5, SHALLOW_WATER", "-0.3, 0.5, SAND", "0.1, 0.9, SWAMP",
            "0.1, 0.1, GRASS", "0.35, 0.9, HILL", "0.25, 0.1, GRASS"})
    void riversKeepDeepWaterForTheBottomAndSwampForTheWetLowland(float height, float moisture, TileType expected) {
        assertEquals(expected, GroundRules.RIVERS.classify(height, ANY, moisture));
    }

    @ParameterizedTest
    @CsvSource({"-0.45, DEEP_WATER", "-0.3, SHALLOW_WATER", "-0.1, SAND", "0.1, GRASS", "0.55, GRASS", "0.65, HILL"})
    void mediterraneanHasASandShoreAndGrassUpToTheHills(float height, TileType expected) {
        assertEquals(expected, GroundRules.MEDITERRANEAN.classify(height, ANY, ANY));
    }

    @ParameterizedTest
    @CsvSource({"0.0, DEEP_WATER", "0.25, SHALLOW_WATER", "0.35, GRASS", "0.45, HILL", "0.95, HILL"})
    void mountainousHasItsWaterHighBetweenThePeaks(float height, TileType expected) {
        assertEquals(expected, GroundRules.MOUNTAINOUS.classify(height, ANY, ANY));
    }
}
