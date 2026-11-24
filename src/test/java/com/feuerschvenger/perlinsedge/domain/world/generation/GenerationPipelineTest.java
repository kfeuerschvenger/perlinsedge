package com.feuerschvenger.perlinsedge.domain.world.generation;

import com.feuerschvenger.perlinsedge.domain.world.generation.support.TestContexts;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GenerationPipelineTest {

    private static final int SIZE = 4;
    private static final long TICK = 25L;

    private final GenerationContext context = TestContexts.of(SIZE, SIZE);
    private final List<String> events = new ArrayList<>();

    /** A pass that records its name and the context it got. */
    private GenerationPass pass(String name) {
        return pass(name, (GenerationContext ctx) -> {
            assertSame(context, ctx);
            events.add(name);
        });
    }

    private static GenerationPass pass(String name, Consumer<GenerationContext> action) {
        return new GenerationPass() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public void apply(GenerationContext ctx) {
                action.accept(ctx);
            }
        };
    }

    @Test
    void runsThePassesInListOrderOverTheSameContext() {
        new GenerationPipeline().run(context, List.of(pass("a"), pass("b"), pass("c")), PassObserver.NONE);

        assertEquals(List.of("a", "b", "c"), events);
    }

    @Test
    void reportsTheNameAndTheDurationOfEveryPassToTheObserver() {
        final var clock = new long[] {0L};
        final var pipeline = new GenerationPipeline(() -> clock[0] += TICK);
        final var timings = new ArrayList<String>();

        final PassObserver observer = (String name, long nanos) -> timings.add(name + "=" + nanos);

        pipeline.run(context, List.of(pass("a"), pass("b")), observer);

        assertEquals(List.of("a=" + TICK, "b=" + TICK), timings);
    }

    @Test
    void anObserverHearsOfAPassOnlyAfterItRan() {
        final var order = new ArrayList<String>();
        final var passes = List.of(
                pass("a", (GenerationContext ctx) -> order.add("ran a")),
                pass("b", (GenerationContext ctx) -> order.add("ran b")));

        new GenerationPipeline().run(context, passes, (String name, long nanos) -> order.add("heard " + name));

        assertEquals(List.of("ran a", "heard a", "ran b", "heard b"), order);
    }

    @Test
    void aFailingPassStopsTheRunAndIsNotReported() {
        final var heard = new ArrayList<String>();
        final var failing = pass("boom", (GenerationContext ctx) -> {
            throw new IllegalStateException("boom");
        });
        final var passes = List.of(pass("a"), failing, pass("never"));

        assertThrows(IllegalStateException.class,
                () -> new GenerationPipeline().run(context, passes, (String name, long nanos) -> heard.add(name)));

        assertEquals(List.of("a"), events);
        assertEquals(List.of("a"), heard);
    }

    @Test
    void anEmptyListRunsNothing() {
        new GenerationPipeline().run(context, List.of(), (String name, long nanos) -> events.add(name));

        assertEquals(List.of(), events);
    }

    @Test
    void refusesMissingArguments() {
        final var pipeline = new GenerationPipeline();
        final List<GenerationPass> none = List.of();

        assertThrows(NullPointerException.class, () -> pipeline.run(null, none, PassObserver.NONE));
        assertThrows(NullPointerException.class, () -> pipeline.run(context, none, null));
        assertThrows(NullPointerException.class, () -> new GenerationPipeline(null));
    }
}
