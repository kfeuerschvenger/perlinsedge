package com.feuerschvenger.perlinsedge.domain.world.generation;

/**
 * One step of the generation of a map. A pass reads and writes the arrays of the context and nothing else, so each one
 * can be tested on a small context without the others.
 * <p>
 * Only {@code NoiseFieldsPass} may call {@link GenerationContext#noiseFactory()}. Every other pass reads the fields it
 * needs from the context, which keeps the noise library out of them and lets a test fill the fields by hand.
 * <p>
 * A pass that draws random numbers asks the context for a stream with a salt of its own. When the same pass appears
 * twice in one recipe, the pipeline gives each occurrence a different stream, see
 * {@link GenerationContext#random(long)}.
 */
public interface GenerationPass {

    /**
     * Returns the name the timings of this pass are reported under. Two passes of the same kind share a name, which is
     * how the pipeline tells that a pass appears more than once.
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
