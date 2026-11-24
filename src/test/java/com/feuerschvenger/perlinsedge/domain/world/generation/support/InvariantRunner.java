package com.feuerschvenger.perlinsedge.domain.world.generation.support;

import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Generates a map and checks invariants against it. On the first failure the map is painted to a PNG under
 * {@code target/map-invariant-failures}, so the failure can be looked at, and the error says where the file is.
 */
public final class InvariantRunner {

    private static final Path FAILURES = Path.of("target", "map-invariant-failures");

    private static final String IMAGE_FORMAT = "png";

    private final MapSource source;
    private final Path failures;
    private final Function<TileMap, BufferedImage> painter;
    private final String format;

    /**
     * Creates a runner that dumps failures under {@code target/map-invariant-failures}.
     *
     * @param source where the maps come from
     */
    public InvariantRunner(MapSource source) {
        this(source, FAILURES, map -> MapPainter.paint(map, MapStats.nearestFreeTileToCentre(map)), IMAGE_FORMAT);
    }

    /**
     * Creates a runner with its own failure folder, painter and image format, so a test can make the dump fail.
     *
     * @param source   where the maps come from
     * @param failures the folder the images are written to
     * @param painter  turns the failing map into the image
     * @param format   the image format name given to {@link ImageIO}
     */
    InvariantRunner(MapSource source, Path failures, Function<TileMap, BufferedImage> painter, String format) {
        this.source = source;
        this.failures = failures;
        this.painter = painter;
        this.format = format;
    }

    /**
     * Generates the map of a spec and checks every invariant against it.
     *
     * @param spec       what to generate
     * @param invariants what must hold
     * @return the number of invariants that held
     * @throws AssertionError naming the spec, the failure and the dumped image, when an invariant fails; when the image
     *                        cannot be written, the original failure is thrown with the write error suppressed on it
     * @throws RuntimeException the original failure of an invariant that throws one, with the error of the painter or
     *                          of the image writer suppressed on it when the dump fails
     */
    public int run(MapSpec spec, List<MapInvariant> invariants) {
        final var map = source.generate(spec);
        for (var invariant : invariants) {
            try {
                invariant.check(map, spec);
            } catch (AssertionError | RuntimeException failure) {
                final var image = dump(spec, map, failure);
                if (image.isEmpty()) {
                    throw failure;
                }
                final var message = String.valueOf(failure.getMessage());
                throw new AssertionError(spec.label() + ": " + message + " (image: " + image.get() + ")", failure);
            }
        }
        return invariants.size();
    }

    private Optional<Path> dump(MapSpec spec, TileMap map, Throwable failure) {
        final var file = failures.resolve(spec.label() + "." + format);
        try {
            Files.createDirectories(failures);
            if (ImageIO.write(painter.apply(map), format, file.toFile())) {
                return Optional.of(file);
            }
            failure.addSuppressed(new IOException("no writer for the image format " + format));
        } catch (IOException | RuntimeException e) {
            failure.addSuppressed(e);
        }
        return Optional.empty();
    }
}
