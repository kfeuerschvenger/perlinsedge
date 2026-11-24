package com.feuerschvenger.perlinsedge.domain.world.generation;

/** Receives the time each pass of a generation took, so timings can be logged without the passes knowing. */
@FunctionalInterface
public interface PassObserver {

    /** An observer that ignores everything. */
    PassObserver NONE = (String name, long nanos) -> { };

    /**
     * Called after a pass finished.
     *
     * @param name  the name of the pass
     * @param nanos how long the pass ran, in nanoseconds
     */
    void onPass(String name, long nanos);
}
