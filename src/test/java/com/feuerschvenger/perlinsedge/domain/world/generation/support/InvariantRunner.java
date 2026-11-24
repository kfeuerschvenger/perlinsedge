package com.feuerschvenger.perlinsedge.domain.world.generation.support;

import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Generates a map and checks invariants against it. On the first failure the map is painted to a PNG under
 * {@code target/map-invariant-failures}, so the failure can be looked at, and the error says where the file is.
 */
public final class InvariantRunner {

    private static final Path FAILURES = Path.of("target", "map-invariant-failures");

    private final MapSource source;

    /**
     * Creates a runner.
     *
     * @param source where the maps come from
     */
    public InvariantRunner(MapSource source) {
        this.source = source;
    }

    /**
     * Generates the map of a spec and checks every invariant against it.
     *
     * @param spec       what to generate
     * @param invariants what must hold
     * @throws AssertionError naming the spec, the failure and the dumped image, when an invariant fails
     */
    public void run(MapSpec spec, List<MapInvariant> invariants) {
        final var map = source.generate(spec);
        for (var invariant : invariants) {
            try {
                invariant.check(map, spec);
            } catch (AssertionError failure) {
                final var image = dump(spec, map);
                throw new AssertionError(spec.label() + ": " + failure.getMessage() + " (image: " + image + ")",
                        failure);
            }
        }
    }

    private static Path dump(MapSpec spec, TileMap map) {
        final var file = FAILURES.resolve(spec.label() + ".png");
        try {
            Files.createDirectories(FAILURES);
            ImageIO.write(MapPainter.paint(map, MapStats.nearestFreeTileToCentre(map)), "png", file.toFile());
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException("could not write " + file, e);
        }
    }
}
