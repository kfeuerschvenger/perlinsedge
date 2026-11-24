package com.feuerschvenger.perlinsedge.domain.world.generation;

import java.util.function.ObjLongConsumer;

/**
 * Receives the time each pass of a generation took, so timings can be logged without the passes knowing. It is called
 * after a pass finished, with the name of the pass and how long it ran in nanoseconds.
 */
@FunctionalInterface
public interface PassObserver extends ObjLongConsumer<String> {

    /** An observer that ignores everything. */
    PassObserver NONE = (String name, long nanos) -> { };
}
