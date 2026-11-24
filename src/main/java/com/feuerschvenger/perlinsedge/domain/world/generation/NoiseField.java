package com.feuerschvenger.perlinsedge.domain.world.generation;

/**
 * The noise fields a map can be built from. Each one is sampled once into an array of the context, and its seed is the
 * seed of the map plus an offset of its own, so the fields of one map are unrelated to each other.
 */
public enum NoiseField {
    /**
     * The height of the ground. It is sampled over the range of the noise, about -1 to 1, and later passes reshape it,
     * so after an island pass it can lie outside that range.
     */
    HEIGHT(0L, false),
    /** The temperature, from 0 to 1. */
    TEMPERATURE(1L, true),
    /** The moisture, from 0 to 1. */
    MOISTURE(2L, true),
    /** Where stone outcrops, from 0 to 1. */
    STONE(3L, true),
    /** Where crystals can grow, from 0 to 1. */
    CRYSTALS(4L, true),
    /** Where trees grow thickly, from 0 to 1. */
    TREE(5L, true);

    private final long seedOffset;
    private final boolean normalized;

    /** Creates a field with the offset it adds to the seed and whether its values are brought to 0..1. */
    NoiseField(long seedOffset, boolean normalized) {
        this.seedOffset = seedOffset;
        this.normalized = normalized;
    }

    /**
     * Returns the number added to the seed of the map to get the seed of this field.
     *
     * @return the offset
     */
    public long seedOffset() {
        return seedOffset;
    }

    /**
     * Tells whether the values of this field are brought from the -1..1 of the noise to 0..1 when it is sampled.
     *
     * @return true when the field is stored from 0 to 1, false when it keeps the range of the noise
     */
    public boolean isNormalized() {
        return normalized;
    }
}
