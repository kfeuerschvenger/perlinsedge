package com.feuerschvenger.perlinsedge.domain.world.generation.support;

import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Runs the invariants of the harness against the generator the game uses, for every type over the fixed seeds. */
class CommonInvariantsTest {

    private static final int SIZE = 150;
    private static final int TINY = 16;

    @ParameterizedTest
    @EnumSource(MapType.class)
    void everyTypeKeepsTheCommonAndPerTypeInvariantsOnTheFixedSeeds(MapType type) {
        final var source = MapSource.current();
        final var runner = new InvariantRunner(source);
        final var invariants = new ArrayList<>(MapInvariants.common(source));
        invariants.addAll(MapInvariants.perType().get(type));

        for (var seed : FixedSeeds.ALL) {
            runner.run(new MapSpec(type, seed, SIZE), invariants);
        }
    }

    @Test
    void aFailingInvariantDumpsAnImageAndNamesIt() {
        final var runner = new InvariantRunner(MapSource.current());
        final var spec = new MapSpec(MapType.DESERT, FixedSeeds.ALL.get(0), TINY);
        final List<MapInvariant> failing = List.of((map, ignored) -> {
            throw new AssertionError("always fails");
        });

        final var error = assertThrows(AssertionError.class, () -> runner.run(spec, failing));

        assertTrue(error.getMessage().contains("always fails"));
        assertTrue(error.getMessage().contains(spec.label() + ".png"));
    }

    @Test
    void thePainterGivesOnePixelPerTileAndColoursSpotsApart() {
        final var map = MapSource.current().generate(new MapSpec(MapType.RIVERS, FixedSeeds.ALL.get(1), TINY));
        final var plain = MapPainter.paint(map);
        final var marked = MapPainter.paint(map, Optional.of(new Spot(TINY / 2, TINY / 2)));

        assertEquals(TINY, plain.getWidth());
        assertEquals(TINY, plain.getHeight());
        assertNotEquals(plain.getRGB(TINY / 2, TINY / 2), marked.getRGB(TINY / 2, TINY / 2));
    }
}
