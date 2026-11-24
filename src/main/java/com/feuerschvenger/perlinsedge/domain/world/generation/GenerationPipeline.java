package com.feuerschvenger.perlinsedge.domain.world.generation;

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
     *
     * @param context  the state the passes work on
     * @param passes   the passes, in the order they run
     * @param observer told the time of each pass
     */
    public void run(GenerationContext context, List<GenerationPass> passes, PassObserver observer) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(observer, "observer");
        for (var pass : passes) {
            final var start = clock.getAsLong();
            pass.apply(context);
            observer.onPass(pass.name(), clock.getAsLong() - start);
        }
    }
}
