package com.feuerschvenger.perlinsedge.domain.world.generation.support;

import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Covers the ways dumping the image of a failed invariant can itself fail. */
class InvariantRunnerTest {

    private static final int SIDE = 4;
    private static final String FAILURE_MESSAGE = "always fails";
    private static final MapSpec SPEC = new MapSpec(MapType.DESERT, 1L, SIDE);
    private static final MapSource EMPTY_MAPS = spec -> new TileMap(spec.size(), spec.size());
    private static final List<MapInvariant> FAILING = List.of((map, spec) -> {
        throw new AssertionError(FAILURE_MESSAGE);
    });
    private static final Function<TileMap, BufferedImage> BLANK = map -> new BufferedImage(SIDE, SIDE,
            BufferedImage.TYPE_INT_RGB);

    @TempDir
    Path folder;

    @Test
    void aPainterThatThrowsLeavesTheOriginalFailureWithTheErrorSuppressed() {
        final var broken = new IllegalStateException("painter broke");
        final var runner = new InvariantRunner(EMPTY_MAPS, folder, map -> {
            throw broken;
        }, "png");

        final var error = assertThrows(AssertionError.class, () -> runner.run(SPEC, FAILING));

        assertEquals(FAILURE_MESSAGE, error.getMessage());
        assertEquals(List.of(broken), Arrays.asList(error.getSuppressed()));
    }

    @Test
    void anImageFormatWithNoWriterIsAFailureOfTheDump() {
        final var runner = new InvariantRunner(EMPTY_MAPS, folder, BLANK, "no-such-format");

        final var error = assertThrows(AssertionError.class, () -> runner.run(SPEC, FAILING));

        assertEquals(FAILURE_MESSAGE, error.getMessage());
        assertEquals(1, error.getSuppressed().length);
        assertInstanceOf(IOException.class, error.getSuppressed()[0]);
        assertTrue(error.getSuppressed()[0].getMessage().contains("no-such-format"));
    }

    @Test
    void aFolderThatCannotBeCreatedIsAFailureOfTheDump() throws IOException {
        final var blocker = Files.createFile(folder.resolve("blocker"));
        final var runner = new InvariantRunner(EMPTY_MAPS, blocker.resolve("failures"), BLANK, "png");

        final var error = assertThrows(AssertionError.class, () -> runner.run(SPEC, FAILING));

        assertEquals(FAILURE_MESSAGE, error.getMessage());
        assertEquals(1, error.getSuppressed().length);
        assertInstanceOf(IOException.class, error.getSuppressed()[0]);
    }

    @Test
    void aRuntimeFailureOfAnInvariantIsRethrownAsItIsWhenTheDumpFails() {
        final var runner = new InvariantRunner(EMPTY_MAPS, folder, map -> {
            throw new UncheckedIOException(new IOException("disk gone"));
        }, "png");
        final List<MapInvariant> throwing = List.of((map, spec) -> {
            throw new IllegalArgumentException("bad map");
        });

        final var error = assertThrows(IllegalArgumentException.class, () -> runner.run(SPEC, throwing));

        assertEquals(1, error.getSuppressed().length);
    }

    @Test
    void aPassingRunReportsHowManyInvariantsHeld() {
        final var runner = new InvariantRunner(EMPTY_MAPS, folder, BLANK, "png");
        final List<MapInvariant> passing = List.of((map, spec) -> { }, (map, spec) -> { });

        assertEquals(2, runner.run(SPEC, passing));
    }
}
