package com.feuerschvenger.perlinsedge.domain.events;

import com.feuerschvenger.perlinsedge.domain.world.generation.IMapGenerator;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

/**
 * Interface to listen for map generation events.
 */
public interface IMapGenerationListener {

    void onMapGenerated(TileMap map, IMapGenerator generator);

}