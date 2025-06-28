package com.feuerschvenger.perlinsedge.domain.world.generation;

import com.feuerschvenger.perlinsedge.domain.world.model.MapType;

/**
 * Objeto que encapsula todos los parámetros necesarios para generar un mapa.
 * Esto evita métodos con una lista de argumentos demasiado larga.
 */
public class MapGenerationContext {
    private MapType mapType;
    private long seed;
    private float heightNoiseFrequency;
    private int heightNoiseOctaves;
    private float temperatureNoiseFrequency;
    private int temperatureNoiseOctaves;
    private float moistureNoiseFrequency;
    private int moistureNoiseOctaves;
    private float stonePatchFrequency;
    private float stoneThreshold;
    private float crystalsPatchFrequency;
    private float crystalsThreshold;
    private float treeDensity;
    private float riverDensity;

    public MapGenerationContext(MapType mapType, long seed,
                                float heightNoiseFrequency, int heightNoiseOctaves,
                                float temperatureNoiseFrequency, int temperatureNoiseOctaves,
                                float moistureNoiseFrequency, int moistureNoiseOctaves,
                                float stonePatchFrequency, float stoneThreshold,
                                float crystalsPatchFrequency, float crystalsThreshold,
                                float treeDensity, float riverDensity) {
        this.mapType = mapType;
        this.seed = seed;
        this.heightNoiseFrequency = heightNoiseFrequency;
        this.heightNoiseOctaves = heightNoiseOctaves;
        this.temperatureNoiseFrequency = temperatureNoiseFrequency;
        this.temperatureNoiseOctaves = temperatureNoiseOctaves;
        this.moistureNoiseFrequency = moistureNoiseFrequency;
        this.moistureNoiseOctaves = moistureNoiseOctaves;
        this.stonePatchFrequency = stonePatchFrequency;
        this.stoneThreshold = stoneThreshold;
        this.crystalsPatchFrequency = crystalsPatchFrequency;
        this.crystalsThreshold = crystalsThreshold;
        this.treeDensity = treeDensity;
        this.riverDensity = riverDensity;
    }

    public MapType getMapType() { return mapType; }
    public long getSeed() { return seed; }
    public float getHeightNoiseFrequency() { return heightNoiseFrequency; }
    public int getHeightNoiseOctaves() { return heightNoiseOctaves; }
    public float getTemperatureNoiseFrequency() { return temperatureNoiseFrequency; }
    public int getTemperatureNoiseOctaves() { return temperatureNoiseOctaves; }
    public float getMoistureNoiseFrequency() { return moistureNoiseFrequency; }
    public int getMoistureNoiseOctaves() { return moistureNoiseOctaves; }
    public float getStonePatchFrequency() { return stonePatchFrequency; }
    public float getStoneThreshold() { return stoneThreshold; }
    public float getCrystalsPatchFrequency() { return crystalsPatchFrequency; }
    public float getCrystalsThreshold() { return crystalsThreshold; }
    public float getTreeDensity() { return treeDensity; }
    public float getRiverDensity() { return riverDensity; }
}