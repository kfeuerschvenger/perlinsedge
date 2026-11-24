package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

/**
 * Selects tiles by index. The helpers of the generation passes take one instead of a boolean array, so a pass states
 * which tiles count with a lambda over the context and allocates nothing.
 */
@FunctionalInterface
interface Mask {

    /**
     * Tells whether a tile is selected.
     *
     * @param index an index into the arrays of the context
     * @return true when the tile counts
     */
    boolean test(int index);
}
