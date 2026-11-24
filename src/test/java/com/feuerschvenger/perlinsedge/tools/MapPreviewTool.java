package com.feuerschvenger.perlinsedge.tools;

import com.feuerschvenger.perlinsedge.domain.world.generation.PassObserver;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.FixedSeeds;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.MapPainter;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.MapSource;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.MapSpec;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Generates every map type over a few seeds and writes one PNG per map into {@code docs/private/map-previews}, with one
 * pixel per tile and the spawn marked in red, so a change to the generator can be judged by eye. It also logs the time
 * each pass took, per map and as the average of each map type, and the time of the whole generation. It is not a test
 * and never runs in the build; run its {@code main} from the IDE with the repository root as the working directory,
 * which is where the output folder is resolved. Arguments, all optional: {@code --types=DESERT,ISLANDS},
 * {@code --seeds=1,2,3}, {@code --size=300}.
 */
public final class MapPreviewTool {

    private static final Logger LOG = LoggerFactory.getLogger(MapPreviewTool.class);
    private static final Path OUTPUT = Path.of("docs", "private", "map-previews");
    private static final int DEFAULT_SIZE = 300;
    private static final String TYPES_FLAG = "--types=";
    private static final String SEEDS_FLAG = "--seeds=";
    private static final String SIZE_FLAG = "--size=";
    private static final String SEPARATOR = ",";
    private static final double NANOS_PER_MILLI = 1_000_000.0;

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
        Files.createDirectories(OUTPUT);
        var totalNanos = 0L;
        for (var type : types) {
            final var timings = new LinkedHashMap<String, Long>();
            final PassObserver observer = (String name, long nanos) -> timings.merge(name, nanos, Long::sum);
            final var source = MapSource.observed(observer);
            var typeNanos = 0L;
            for (var seed : seeds) {
                typeNanos += preview(source, new MapSpec(type, seed, size));
            }
            logTimings(type, seeds.size(), typeNanos, timings);
            totalNanos += typeNanos;
        }
        LOG.info("{} maps in {} ms, written to {}", types.size() * seeds.size(),
                TimeUnit.NANOSECONDS.toMillis(totalNanos), OUTPUT.toAbsolutePath());
    }

    private static long preview(MapSource source, MapSpec spec) throws IOException {
        final var start = System.nanoTime();
        final var map = source.generate(spec);
        final var elapsed = System.nanoTime() - start;
        ImageIO.write(MapPainter.paint(map), "png", OUTPUT.resolve(spec.label() + ".png").toFile());
        return elapsed;
    }

    /** Logs the average time of each pass and of the whole generation over the seeds of one map type. */
    private static void logTimings(MapType type, int maps, long totalNanos, Map<String, Long> timings) {
        LOG.info("{} average over {} seeds: {} ms per map", type, maps,
                format(totalNanos / (double) maps));
        for (var entry : timings.entrySet()) {
            LOG.info("    {} {} ms", String.format("%-15s", entry.getKey()), format(entry.getValue() / (double) maps));
        }
    }

    private static String format(double nanos) {
        return String.format("%.1f", nanos / NANOS_PER_MILLI);
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
