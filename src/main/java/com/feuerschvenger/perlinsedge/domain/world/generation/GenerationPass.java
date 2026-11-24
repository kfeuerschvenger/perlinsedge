package com.feuerschvenger.perlinsedge.domain.world.generation;

/**
 * One step of the generation of a map. A pass reads and writes the arrays of the context and nothing else, so each one
 * can be tested on a small context without the others.
 */
public interface GenerationPass {

    /**
     * Returns the name the timings of this pass are reported under.
     *
     * @return a short stable name
     */
    String name();

    /**
     * Runs the pass.
     *
     * @param context the state of the generation in progress
     * @throws IllegalStateException when a field the pass needs has not been computed by an earlier pass
     */
    void apply(GenerationContext context);
}
