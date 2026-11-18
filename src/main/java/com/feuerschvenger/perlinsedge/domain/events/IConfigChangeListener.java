package com.feuerschvenger.perlinsedge.domain.events;

import com.feuerschvenger.perlinsedge.domain.world.generation.MapGenerationContext;

/**
 * Interface for listening to configuration changes in the map generation settings.
 */
public interface IConfigChangeListener {

    /**
     * Called when the map generation settings change.
     *
     * @param context every parameter of the map to generate
     */
    void onMapConfigChanged(MapGenerationContext context);

}
