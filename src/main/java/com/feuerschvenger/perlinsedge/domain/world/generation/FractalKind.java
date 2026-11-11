package com.feuerschvenger.perlinsedge.domain.world.generation;

/** How the octaves of a noise source are combined, independent of the noise library behind it. */
public enum FractalKind {
    /** A single layer of noise, octaves are ignored. */
    NONE,
    /** Fractional Brownian motion: smooth, rolling terrain. */
    FBM,
    /** Ridged multifractal: sharp ridges, used for mountains and dunes. */
    RIDGED
}
