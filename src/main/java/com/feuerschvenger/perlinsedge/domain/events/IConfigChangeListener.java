package com.feuerschvenger.perlinsedge.domain.events;

import com.feuerschvenger.perlinsedge.domain.world.model.MapType;

/**
 * Interface for listening to configuration changes in the map generation settings.
 */
public interface IConfigChangeListener {

    void onMapConfigChanged(MapType mapType, long seed,
                            float heightFreq, int heightOctaves,
                            float tempFreq, int tempOctaves,
                            float moistureFreq, int moistureOctaves,
                            float stoneFreq, float stoneThreshold,
                            float crystalsFreq, float crystalsThreshold,
                            float treeDensity, float riverDensity);

}