package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

import java.util.Objects;

/**
 * Which part of the land the highest ground turns into a different terrain.
 *
 * @param landShare the share of the land, above 0 and up to 1, that becomes the band, taken from the highest tiles
 * @param band      the terrain of the band, which must be land, such as {@link TileType#MOUNTAIN}
 */
public record HeightBandParams(float landShare, TileType band) {

    /** Checks the share is a share of the land and the band is a terrain of land. */
    public HeightBandParams {
        if (Float.isNaN(landShare) || landShare <= 0F || landShare > 1F) {
            throw new IllegalArgumentException("the band land share must be above 0 and at most 1: " + landShare);
        }
        Objects.requireNonNull(band, "band");
        if (!band.isLand()) {
            throw new IllegalArgumentException("the band must be land: " + band);
        }
    }
}
