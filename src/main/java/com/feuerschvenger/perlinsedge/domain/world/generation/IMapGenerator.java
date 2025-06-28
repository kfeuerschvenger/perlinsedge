package com.feuerschvenger.perlinsedge.domain.world.generation;

import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

/**
 * Interface for map generation strategies.
 * Implementations should provide a way to generate a TileMap based on the provided context.
 */
public interface IMapGenerator {

    /**
     * Generates a TileMap based on the specified width, height, and context.
     *
     * @param width   The width of the map.
     * @param height  The height of the map.
     * @param context The context containing parameters for map generation.
     * @return A TileMap generated according to the specified parameters.
     */
    TileMap generateMap(int width, int height, MapGenerationContext context);

    /**
     * Returns the type of map this generator produces.
     * @return The MapType associated with this generator.
     */
    MapType getMapType();

    /**
     * Returns the seed used for random number generation in this map generator.
     * @return The seed value.
     */
    long getSeed();

    /**
     * Returns the frequency of height noise used in the map generation.
     * @return The height noise frequency.
     */
    float getHeightNoiseFrequency();

    /**
     * Returns the number of octaves used for height noise generation.
     * @return The number of octaves.
     */
    int getHeightNoiseOctaves();

    /**
     * Returns the frequency of temperature noise used in the map generation.
     * @return The temperature noise frequency.
     */
    float getTemperatureNoiseFrequency();

    /**
     * Returns the number of octaves used for temperature noise generation.
     * @return The number of octaves.
     */
    int getTemperatureNoiseOctaves();

    /**
     * Returns the frequency of moisture noise used in the map generation.
     * @return The moisture noise frequency.
     */
    float getMoistureNoiseFrequency();

    /**
     * Returns the number of octaves used for moisture noise generation.
     * @return The number of octaves.
     */
    int getMoistureNoiseOctaves();

    /**
     * Returns the frequency of stone patches in the map.
     * @return The stone patch frequency.
     */
    float getStonePatchFrequency();

    /**
     * Returns the threshold for stone generation in the map.
     * @return The stone threshold.
     */
    float getStoneThreshold();

    /**
     * Returns the frequency of crystal patches in the map.
     * @return The crystal patch frequency.
     */
    float getCrystalsPatchFrequency();

    /**
     * Returns the threshold for crystal generation in the map.
     * @return The crystal threshold.
     */
    float getCrystalsThreshold();

    /**
     * Returns the frequency of tree patches in the map.
     * @return The tree patch frequency.
     */
    float getTreeDensity();

    /**
     * Returns the frequency of river patches in the map.
     * @return The river density.
     */
    float getRiverDensity();

}