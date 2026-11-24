package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

/**
 * Where stone outcrops.
 *
 * @param threshold the stone noise has to pass this value for a tile to hold stone, a number; a value of 1 or more
 *                  leaves the map without stone
 */
public record StoneParams(float threshold) {

    /** Checks the threshold is a number. */
    public StoneParams {
        if (Float.isNaN(threshold)) {
            throw new IllegalArgumentException("the stone threshold is not a number");
        }
    }
}
