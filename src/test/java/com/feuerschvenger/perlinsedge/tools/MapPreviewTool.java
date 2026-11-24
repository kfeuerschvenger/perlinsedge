package com.feuerschvenger.perlinsedge.tools;

import com.feuerschvenger.perlinsedge.domain.world.generation.support.FixedSeeds;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.MapPainter;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.MapSource;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.MapSpec;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.MapStats;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Generates every map type over a few seeds and writes one PNG per map into {@code docs/private/map-previews}, with one
 * pixel per tile, so a change to the generator can be judged by eye. It also logs the time each map took to generate.
 * It is not a test and never runs in the build; run it from the IDE or with {@code exec:java} from the repository root.
 * Arguments, all optional: {@code --types=DESERT,ISLANDS}, {@code --seeds=1,2,3}, {@code --size=300}.
 */
public final class MapPreviewTool {

    private static final Logger LOG = LoggerFactory.getLogger(MapPreviewTool.class);
    private static final Path OUTPUT = Path.of("docs", "private", "map-previews");
    private static final int DEFAULT_SIZE = 300;
    private static final String TYPES_FLAG = "--types=";
    private static final String SEEDS_FLAG = "--seeds=";
    private static final String SIZE_FLAG = "--size=";
    private static final String SEPARATOR = ",";

    private MapPreviewTool() {
    }

    /**
     * Runs the tool.
     *
     * @param args the optional flags described on the class
     * @throws IOException when an image cannot be written
     */
    public static void main(String[] args) throws IOException {
        final var types = types(args);
        final var seeds = seeds(args);
        final var size = size(args);
        final var source = MapSource.current();
        Files.createDirectories(OUTPUT);
        var totalNanos = 0L;
        for (var type : types) {
            for (var seed : seeds) {
                totalNanos += preview(source, new MapSpec(type, seed, size));
            }
        }
        LOG.info("{} maps in {} ms, written to {}", types.size() * seeds.size(),
                TimeUnit.NANOSECONDS.toMillis(totalNanos), OUTPUT.toAbsolutePath());
    }

    private static long preview(MapSource source, MapSpec spec) throws IOException {
        final var start = System.nanoTime();
        final var map = source.generate(spec);
        final var elapsed = System.nanoTime() - start;
        final var spot = MapStats.nearestFreeTileToCentre(map);
        ImageIO.write(MapPainter.paint(map, spot), "png", OUTPUT.resolve(spec.label() + ".png").toFile());
        LOG.info("{} generated in {} ms (total, per pass timings arrive with the pipeline)", spec.label(),
                TimeUnit.NANOSECONDS.toMillis(elapsed));
        return elapsed;
    }

    private static List<MapType> types(String[] args) {
        return value(args, TYPES_FLAG)
                .map(text -> Arrays.stream(text.split(SEPARATOR)).map(MapType::valueOf).toList())
                .orElseGet(() -> List.of(MapType.values()));
    }

    private static List<Long> seeds(String[] args) {
        return value(args, SEEDS_FLAG)
                .map(text -> Arrays.stream(text.split(SEPARATOR)).map(Long::valueOf).toList())
                .orElse(FixedSeeds.ALL);
    }

    private static int size(String[] args) {
        return value(args, SIZE_FLAG).map(Integer::valueOf).orElse(DEFAULT_SIZE);
    }

    private static Optional<String> value(String[] args, String flag) {
        return Arrays.stream(args).filter(arg -> arg.startsWith(flag)).map(arg -> arg.substring(flag.length()))
                .findFirst();
    }
}
