package com.feuerschvenger.perlinsedge.domain.world.generation;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.LongSupplier;

/** Runs the passes of a recipe in order over one context and reports how long each one took. */
public final class GenerationPipeline {

    private final LongSupplier clock;

    /** Creates a pipeline that measures time with {@link System#nanoTime()}. */
    public GenerationPipeline() {
        this(System::nanoTime);
    }

    /**
     * Creates a pipeline with its own clock, so a test can make the timings exact.
     *
     * @param clock returns a time in nanoseconds, only the difference between two calls matters
     */
    GenerationPipeline(LongSupplier clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Applies every pass to the context, in list order. The observer hears of a pass after it finished, so a pass that
     * throws is not reported and stops the run.
     * <p>
     * A pass that appears more than once under the same name draws from a different random stream each time: the first
     * occurrence uses the plain stream of its salt, and the later ones mix in how many passes of that name ran before
     * them. Inserting a pass of another name never moves the stream of any pass.
     *
     * @param context  the state the passes work on
     * @param passes   the passes, in the order they run
     * @param observer told the time of each pass
     * @throws RuntimeException when a pass throws, the original exception itself, with an
     *                          {@link IllegalStateException} naming the pass attached as a suppressed exception
     */
    public void run(GenerationContext context, List<GenerationPass> passes, PassObserver observer) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(observer, "observer");
        Objects.requireNonNull(passes, "passes");
        final var occurrences = new HashMap<String, Integer>();
        try {
            for (var pass : passes) {
                Objects.requireNonNull(pass, "pass");
                final var name = pass.name();
                final var occurrence = occurrences.merge(name, 1, Integer::sum) - 1;
                context.beginPass(occurrence);
                final var start = clock.getAsLong();
                apply(pass, name, context);
                observer.accept(name, clock.getAsLong() - start);
            }
        } finally {
            context.beginPass(0);
        }
    }

    /** Applies one pass and, when it fails, rethrows the original exception with the pass named in a suppressed one. */
    private static void apply(GenerationPass pass, String name, GenerationContext context) {
        try {
            pass.apply(context);
        } catch (RuntimeException failure) {
            failure.addSuppressed(new IllegalStateException("the pass '" + name + "' failed"));
            throw failure;
        }
    }
}
