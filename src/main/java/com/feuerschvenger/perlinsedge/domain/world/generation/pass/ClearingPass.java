package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationPass;

import java.util.Arrays;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * Opens clearings in the vegetation: round areas where no resource stands, spread evenly over the walkable ground by
 * Poisson-disc sampling so none is far from another. Every clearing is a site that {@link TrailNetworkPass} joins, and
 * its tiles are reserved, so the passes after it neither fill them with resources nor floor them.
 * <p>
 * Sites keep a clearance from ground that cannot be walked on, water and mountains, so a clearing is a valley floor
 * or a glade and not a spot on a shore.
 * <p>
 * One of the clearings is the spawn clearing, which has its own larger radius and whose centre is set as the spawn
 * hint. It is centred on the walkable tile nearest to the middle of the map that has the most room around it, within
 * the largest piece of walkable ground so the player never starts on a small island. The room asked for starts at the
 * radius of that clearing and falls back as far as the clearance of the sites when no tile has it. When even that
 * room is nowhere on the map, as on a map too small for the clearance, there is no spawn clearing and no spawn hint,
 * and the spawn pass places the player on its own, possibly closer to rock or water than the clearance. Ordinary sites
 * closer than half the spacing to the spawn clearing are dropped.
 * <p>
 * A piece of walkable ground that the sampling left without a site, because every tile of it is too close to a site on
 * the other side of a ridge or too narrow for the clearance, gets a clearing of the smallest radius at the tile with
 * the most room around it when the piece is large enough, so every valley has a place to stand and not only the lucky
 * ones. A trail never crosses water, so a valley that water cuts off from the others gets its clearing but no trail to
 * it: reaching it is a bridge the player builds.
 * <p>
 * The spacing is at most a third of the shorter side of the map, and the radii of the clearings shrink in the same
 * proportion when it is, so a small or narrow map keeps most of its land under trees and does not turn into one big
 * glade. The spawn clearing shrinks too but never below {@value #MIN_SPAWN_RADIUS} tiles. The spacing also grows on a
 * map so large that the sites would pass {@value SiteEdges#MAX_SITES}, which the trails cannot join, and no site is
 * opened beyond that many.
 * <p>
 * The pass draws from one seeded stream and does nothing when no tile can host a site, so it repeats for a seed.
 */
public final class ClearingPass implements GenerationPass {

    private static final String NAME = "clearings";
    private static final long SALT = 0xC1EA_4126_5EED_0001L;
    /** An ordinary site closer than the spacing over this to the spawn clearing is dropped. */
    private static final int SEPARATION_DIVISOR = 2;
    /** The middle of the map is half the width across and half the height down. */
    private static final int HALF_DIVISOR = 2;
    /**
     * The spacing is at most this part of the shorter side of the map, so a small map still holds a few clearings that
     * the trails can join, and the spacing is never more than the diagonal the sampling accepts.
     */
    private static final int SIDES_PER_SPACING = 3;
    /** The least spacing, in tiles, and the least radius of a clearing that has one. */
    private static final int MIN_SPACING = 1;
    private static final int MIN_RADIUS = 1;
    /**
     * The smallest radius of the spawn clearing, in tiles, on a map that shrinks the clearings. The spawn clearing is
     * centred where the room around a tile, its Chebyshev distance to the nearest water or rock, is at least this, so
     * a disc one tile smaller than the radius around the spawn is clear of water and of resources.
     */
    public static final int MIN_SPAWN_RADIUS = 6;
    /**
     * Sites a map of a given area can hold at the most, as that area over the area of a disc of half the spacing, which
     * the discs around the sites cannot overlap in: four over pi, over the spacing squared.
     */
    private static final double DISC_PACKING = 4.0 / Math.PI;
    /** A random draw takes its bound one past the largest value it may return. */
    private static final int INCLUSIVE_BOUND = 1;
    /** The first label of a piece of walkable ground: the labels run on from the one of the tiles without a piece. */
    private static final int FIRST_LABEL = Regions.NO_LABEL + 1;
    /** The marker of a piece of ground with no tile picked yet. */
    private static final int NO_TILE = -1;

    private final ClearingParams params;

    /**
     * Creates the pass.
     *
     * @param params the spacing, the sizes and the clearance of the clearings
     */
    public ClearingPass(ClearingParams params) {
        this.params = Objects.requireNonNull(params, "params");
    }

    /** Returns the name the timings report this pass under. */
    @Override
    public String name() {
        return NAME;
    }

    /** Samples the sites, opens the spawn clearing and the ordinary ones and records them in the context. */
    @Override
    public void apply(GenerationContext context) {
        new Opening(context).run();
    }

    /** The state of one run: the room around every tile, the pieces of walkable ground and the random stream. */
    private final class Opening {

        private final GenerationContext context;
        private final int[] clearance;
        private final Regions land;
        private final boolean[] served;
        private final RandomGenerator random;
        private final int spacing;
        private final int minRadius;
        private final int maxRadius;
        private final int spawnRadius;

        /**
         * Measures the room around every tile, labels the pieces of walkable ground and works out the spacing and the
         * radii the size of the map allows.
         */
        Opening(GenerationContext context) {
            this.context = context;
            this.clearance = DistanceTransform.chebyshev(context.width(), context.height(),
                    index -> !context.groundAt(index).isWalkable());
            this.land = Regions.label(context.width(), context.height(),
                    index -> context.groundAt(index).isWalkable());
            this.served = new boolean[land.count() + FIRST_LABEL];
            this.random = context.random(SALT);
            final var fitting = Math.clamp(Math.min(context.width(), context.height()) / SIDES_PER_SPACING,
                    MIN_SPACING, params.spacing());
            final var scale = (double) fitting / params.spacing();
            this.spacing = Math.max(fitting, spacingForSiteLimit());
            this.minRadius = scaled(params.minRadius(), scale);
            this.maxRadius = scaled(params.maxRadius(), scale);
            this.spawnRadius = Math.max(Math.min(params.spawnRadius(), MIN_SPAWN_RADIUS),
                    scaled(params.spawnRadius(), scale));
        }

        /** Returns a radius shrunk by the scale, which is at least the least radius. */
        private static int scaled(int radius, double scale) {
            return Math.max(MIN_RADIUS, (int) Math.round(radius * scale));
        }

        /** Returns the least spacing at which the sites of this map cannot pass the most the trails can join. */
        private int spacingForSiteLimit() {
            final var area = (double) context.width() * context.height();
            return (int) Math.ceil(Math.sqrt(DISC_PACKING * area / SiteEdges.MAX_SITES));
        }

        /** Opens the spawn clearing, the ordinary clearings and one for each valley still without one. */
        void run() {
            final var sampled = PoissonDisc.sample(context.width(), context.height(), this::isHost, spacing, random);
            final var spawn = spawnCentre();
            if (spawn != GenerationContext.NONE) {
                open(spawn, spawnRadius);
                context.setSpawnHint(spawn);
            }
            final var separation = spacing / SEPARATION_DIVISOR;
            for (var position = 0; position < sampled.size(); position++) {
                final var site = sampled.get(position);
                if (isFarFrom(site, spawn, separation)) {
                    open(site, minRadius + random.nextInt(maxRadius - minRadius + INCLUSIVE_BOUND));
                }
            }
            openUnservedRegions();
        }

        /** Tells whether a clearing may be centred on a tile: free walkable ground with the clearance from the rest. */
        private boolean isHost(int index) {
            return context.groundAt(index).isWalkable() && !context.isReserved(index)
                    && clearance[index] >= params.clearance();
        }

        /** Tells whether a site is at least the separation from the spawn clearing, which every site is without one. */
        private boolean isFarFrom(int site, int spawn, int separation) {
            return spawn == GenerationContext.NONE
                    || squaredDistance(site, spawn) >= (long) separation * separation;
        }

        /**
         * Finds the centre of the spawn clearing: the tile nearest to the middle of the map among the walkable tiles of
         * the largest piece of walkable ground that have a given room around them, asking for the room of the spawn
         * radius first and for less each time no tile has it, down to the clearance of the sites.
         *
         * @return the tile, or {@link GenerationContext#NONE} when no tile qualifies
         */
        private int spawnCentre() {
            if (land.largest() == Regions.NO_LABEL) {
                return GenerationContext.NONE;
            }
            for (var room = spawnRadius; room >= params.clearance(); room--) {
                final var centre = nearestToMiddle(room);
                if (centre != GenerationContext.NONE) {
                    return centre;
                }
            }
            return GenerationContext.NONE;
        }

        /** Returns the tile of the largest piece with at least the given room that lies nearest to the middle. */
        private int nearestToMiddle(int room) {
            final var middle = context.index(context.width() / HALF_DIVISOR, context.height() / HALF_DIVISOR);
            var best = GenerationContext.NONE;
            var bestDistance = Long.MAX_VALUE;
            for (var index = 0; index < context.size(); index++) {
                if (land.labelAt(index) == land.largest() && clearance[index] >= room && !context.isReserved(index)) {
                    final var distance = squaredDistance(index, middle);
                    if (distance < bestDistance) {
                        best = index;
                        bestDistance = distance;
                    }
                }
            }
            return best;
        }

        /** Gives a clearing of the smallest radius to every large enough piece of ground that has none. */
        private void openUnservedRegions() {
            if (params.minRegionTiles() == 0) {
                return;
            }
            final var deepest = new int[served.length];
            Arrays.fill(deepest, NO_TILE);
            for (var index = 0; index < context.size(); index++) {
                final var label = land.labelAt(index);
                if (label != Regions.NO_LABEL && !served[label] && isDeeper(deepest[label], index)) {
                    deepest[label] = index;
                }
            }
            for (var label = FIRST_LABEL; label < deepest.length; label++) {
                if (deepest[label] != NO_TILE && !served[label] && land.sizeOf(label) >= params.minRegionTiles()) {
                    open(deepest[label], minRadius);
                }
            }
        }

        /** Tells whether a free tile has more room around it than the best one so far, or there is none so far. */
        private boolean isDeeper(int best, int index) {
            return !context.isReserved(index) && (best == NO_TILE || clearance[index] > clearance[best]);
        }

        /** Returns the squared distance in tiles between two tiles. */
        private long squaredDistance(int first, int second) {
            final var dx = (long) context.xOf(first) - context.xOf(second);
            final var dy = (long) context.yOf(first) - context.yOf(second);
            return dx * dx + dy * dy;
        }

        /**
         * Records a site, marks its piece of ground as served and clears the disc of its clearing. Nothing opens once
         * the sites reach the most the trails can join.
         */
        private void open(int centre, int radius) {
            if (context.sites().size() >= SiteEdges.MAX_SITES) {
                return;
            }
            context.sites().add(centre);
            served[land.labelAt(centre)] = true;
            final var centreX = context.xOf(centre);
            final var centreY = context.yOf(centre);
            final var squared = (long) radius * radius;
            final var lastY = Math.min(context.height() - 1, centreY + radius);
            final var lastX = Math.min(context.width() - 1, centreX + radius);
            for (var y = Math.max(0, centreY - radius); y <= lastY; y++) {
                for (var x = Math.max(0, centreX - radius); x <= lastX; x++) {
                    final var dx = (long) x - centreX;
                    final var dy = (long) y - centreY;
                    if (dx * dx + dy * dy <= squared) {
                        openTile(context.index(x, y));
                    }
                }
            }
        }

        /** Clears one tile of the disc when it is walkable ground, and leaves water and mountains as they are. */
        private void openTile(int index) {
            if (context.groundAt(index).isWalkable()) {
                context.setResource(index, null);
                context.reserve(index);
            }
        }
    }
}
