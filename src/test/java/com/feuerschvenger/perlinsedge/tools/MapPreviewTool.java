package com.feuerschvenger.perlinsedge.tools;

import com.feuerschvenger.perlinsedge.domain.world.generation.PassObserver;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.ResourceBalancePass;
import com.feuerschvenger.perlinsedge.domain.world.generation.recipe.MapRecipes;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.Components;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.FixedSeeds;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.MapPainter;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.MapSource;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.MapSpec;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.MapStats;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.WaterStats;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
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
 * each pass took, per map and as the average of each map type, the time of the whole generation, and the lowest and
 * highest count of each resource and the share of rocky ground over the seeds of each map type, and warns for each
 * map whose resources fall short of a quota because the terrain had no room for them. It is not a test and never runs
 * in the build; run its {@code main} from the IDE with the repository root as the working directory,
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
    private static final MapRecipes RECIPES = new MapRecipes();
    private static final double NANOS_PER_MILLI = 1_000_000.0;
    private static final long PERCENT = 100L;
    /** A river component smaller than this is a puddle or a stub and is left out of the report. */
    private static final int MIN_REPORTED_RIVER = 20;

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
            final var counts = new Counts();
            var typeNanos = 0L;
            for (var seed : seeds) {
                typeNanos += preview(source, new MapSpec(type, seed, size), counts);
            }
            logTimings(type, seeds.size(), typeNanos, timings);
            LOG.info("    {}", counts);
            totalNanos += typeNanos;
        }
        LOG.info("{} maps in {} ms, written to {}", types.size() * seeds.size(),
                TimeUnit.NANOSECONDS.toMillis(totalNanos), OUTPUT.toAbsolutePath());
    }

    private static long preview(MapSource source, MapSpec spec, Counts counts) throws IOException {
        final var start = System.nanoTime();
        final var map = source.generate(spec);
        final var elapsed = System.nanoTime() - start;
        counts.add(map);
        reportShortfalls(spec, map);
        reportShapes(spec, map);
        ImageIO.write(MapPainter.paint(map), "png", OUTPUT.resolve(spec.label() + ".png").toFile());
        return elapsed;
    }

    /** Warns when the map holds fewer of a resource than its quota asks for, which the balancing pass reports. */
    private static void reportShortfalls(MapSpec spec, TileMap map) {
        RECIPES.recipeFor(spec.type()).passes().stream()
                .filter(ResourceBalancePass.class::isInstance)
                .map(ResourceBalancePass.class::cast)
                .map(balance -> balance.shortfalls(type -> MapStats.count(map, type), spec.size() * spec.size()))
                .filter(missing -> !missing.isEmpty())
                .forEach(missing -> LOG.warn("{} is short of its quota: {}", spec.label(), missing));
    }

    /** Logs the sizes of the river components of a rivers map and of the islands of an islands map. */
    private static void reportShapes(MapSpec spec, TileMap map) {
        if (spec.type() == MapType.RIVERS) {
            final var rivers = WaterStats.rivers(map);
            LOG.info("    {} rivers: {} components, the {} of at least {} tiles span {}", spec.label(),
                    rivers.count(), rivers.countAtLeast(MIN_REPORTED_RIVER), MIN_REPORTED_RIVER, spans(rivers));
        }
        if (spec.type() == MapType.ISLANDS) {
            final var land = WaterStats.landMasses(map);
            final var sizes = Arrays.stream(land.sizes()).filter(size -> size >= WaterStats.MIN_LAND_TILES).sorted()
                    .toArray();
            LOG.info("    {} islands of at least {} tiles: {} sizes {}", spec.label(), WaterStats.MIN_LAND_TILES,
                    sizes.length, Arrays.toString(sizes));
        }
    }

    private static String spans(Components components) {
        final var text = new StringBuilder();
        for (var label = 1; label <= components.count(); label++) {
            if (components.sizeOf(label) >= MIN_REPORTED_RIVER) {
                text.append(components.spanX(label)).append('x').append(components.spanY(label)).append(" (")
                        .append(components.sizeOf(label)).append(" tiles) ");
            }
        }
        return text.toString().trim();
    }

    /** The lowest and highest count of the resources and of the rocky ground over the maps of one type. */
    private static final class Counts {
        private static final String[] NAMES = {"iron", "crystals", "stone", "trees", "rocky%", "forest%", "water%",
                "bodies", "spawnRegion%", "riverTiles"};
        private final long[] low = new long[NAMES.length];
        private final long[] high = new long[NAMES.length];
        private boolean empty = true;

        private void add(TileMap map) {
            final var land = Math.max(1, MapStats.landTiles(map));
            final long[] values = {MapStats.count(map, ResourceType.IRON_ORE),
                    MapStats.count(map, ResourceType.CRYSTALS), MapStats.count(map, ResourceType.STONE),
                    MapStats.count(map, ResourceType.TREE),
                    PERCENT * MapStats.count(map, TileType.ROCKY_GROUND) / land,
                    PERCENT * MapStats.count(map, TileType.FOREST) / land,
                    Math.round(PERCENT * WaterStats.waterShare(map)),
                    WaterStats.waterBodies(map).countAtLeast(WaterStats.MIN_BODY_TILES),
                    Math.round(PERCENT * WaterStats.spawnRegionShareOfLand(map)),
                    MapStats.count(map, TileType.RIVER)};
            for (var metric = 0; metric < values.length; metric++) {
                low[metric] = empty ? values[metric] : Math.min(low[metric], values[metric]);
                high[metric] = empty ? values[metric] : Math.max(high[metric], values[metric]);
            }
            empty = false;
        }

        @Override
        public String toString() {
            final var text = new StringBuilder();
            for (var metric = 0; metric < NAMES.length; metric++) {
                text.append(NAMES[metric]).append(' ').append(low[metric]).append('-').append(high[metric])
                        .append("  ");
            }
            return text.toString();
        }
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
