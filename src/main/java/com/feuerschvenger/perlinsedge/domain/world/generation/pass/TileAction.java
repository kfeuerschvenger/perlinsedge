package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

/**
 * What a pass does to each tile a helper visits, such as setting its ground or reserving it. A pass implements it once
 * as a lambda over its context, which keeps the helper free of any knowledge of what is being carved.
 */
@FunctionalInterface
interface TileAction {

    /**
     * Acts on one tile. A helper may call it more than once for the same tile, so it must give the same result when
     * repeated.
     *
     * @param index an index into the arrays of the context
     */
    void apply(int index);
}
