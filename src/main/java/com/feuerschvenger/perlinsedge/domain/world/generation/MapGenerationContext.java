package com.feuerschvenger.perlinsedge.domain.world.generation;

import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Immutable holder of every parameter needed to generate a map.
 * It avoids generator methods with a long argument list.
 */
@Getter
@AllArgsConstructor
public class MapGenerationContext {
    private final MapType mapType;
    private final long seed;
    private final float heightNoiseFrequency;
    private final int heightNoiseOctaves;
    private final float temperatureNoiseFrequency;
    private final int temperatureNoiseOctaves;
    private final float moistureNoiseFrequency;
    private final int moistureNoiseOctaves;
    private final float stonePatchFrequency;
    private final float stoneThreshold;
    private final float crystalsPatchFrequency;
    private final float crystalsThreshold;
    private final float treeDensity;
    private final float riverDensity;
}
