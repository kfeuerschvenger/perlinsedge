package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationPass;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * Joins the clearings that {@link ClearingPass} left in the context with trails: a minimum spanning tree of the
 * sites, and a few more trails that close loops.
 * <p>
 * Candidate trails are the pairs of sites no farther apart than the longest edge of the parameters. They are taken
 * from the shortest and kept when their ends are not joined yet and a route exists between them, which is Kruskal's
 * algorithm with a path search deciding what an edge is worth.
 * <p>
 * A trail is the cheapest path over the ground by {@link LeastCostPath}. A tile costs more the thicker the tree noise
 * is and when a resource has to be cleared, water is closed, and a mountain is closed or costs a fixed amount, so with
 * a finite mountain cost the trail cuts a narrow pass through the ridge where that is cheaper than going round it.
 * <p>
 * A carved tile loses its resource, is reserved so no later pass refills or floors it, and a mountain tile on the way
 * becomes rocky ground. A tile that is carved costs the least from then on, so later trails join the earlier ones and
 * the network has trunks and branches. The width of each trail, one or two tiles, is drawn for the trail.
 * <p>
 * The pass labels the ground its trails can cross once, so a pair of sites on pieces of ground that no route joins
 * costs no search. It draws from one seeded stream and does nothing with fewer than two sites, so it repeats for a
 * seed.
 */
public final class TrailNetworkPass implements GenerationPass {

    private static final String NAME = "trails";
    private static final long SALT = 0x7241_1F00_5EED_0001L;
    /** What a tile of open ground without a tree costs, and the lowest cost of any tile. */
    private static final float BASE_COST = 1F;
    /** The search may find a trail that costs up to this many times the cheapest one, in exchange for speed. */
    private static final float SEARCH_SLACK = 2F;
    /** The area, in tiles, that the number of extra trails is counted in. */
    private static final double DENSITY_AREA_TILES = 100_000.0;
    /** A trail joins two sites, so fewer than this many sites need none. */
    private static final int MIN_SITES = 2;

    private final TrailParams params;

    /**
     * Creates the pass.
     *
     * @param params the costs of the ground, the number of extra trails and the widths
     */
    public TrailNetworkPass(TrailParams params) {
        this.params = Objects.requireNonNull(params, "params");
    }

    /** Returns the name the timings report this pass under. */
    @Override
    public String name() {
        return NAME;
    }

    /** Cuts the trails between the sites of the context. */
    @Override
    public void apply(GenerationContext context) {
        if (context.sites().size() >= MIN_SITES) {
            new Network(context).run();
        }
    }

    /** The state of one run: the costs and the searcher, the groups of joined sites and the random stream. */
    private final class Network {

        private final GenerationContext context;
        private final int width;
        private final int height;
        private final float[] cost;
        private final LeastCostPath search;
        private final Regions ground;
        private final int[] path;
        private final RandomGenerator random;

        /** Prepares the arrays of a run: the cost of every tile and the pieces of ground a trail can cross. */
        Network(GenerationContext context) {
            this.context = context;
            this.width = context.width();
            this.height = context.height();
            this.cost = costs();
            this.search = new LeastCostPath(width, height, cost, BASE_COST, SEARCH_SLACK);
            this.ground = Regions.label(width, height, index -> Float.isFinite(cost[index]));
            this.path = new int[context.size()];
            this.random = context.random(SALT);
        }

        /** Cuts the tree of trails and then the extra ones. */
        void run() {
            final var sites = context.sites();
            final var edges = SiteEdges.within(sites, width, params.maxEdgeLength());
            final var groups = new UnionFind(sites.size());
            final var loops = new int[edges.length];
            var loopCount = 0;
            for (var position = 0; position < edges.length; position++) {
                final var from = SiteEdges.from(edges[position]);
                final var to = SiteEdges.to(edges[position]);
                if (groups.find(from) == groups.find(to)) {
                    loops[loopCount] = position;
                    loopCount++;
                } else if (join(sites.get(from), sites.get(to))) {
                    groups.union(from, to);
                }
            }
            cutLoops(edges, loops, loopCount);
        }

        /** Cuts a few of the edges that would close a loop, chosen at random, until the wanted number ran. */
        private void cutLoops(long[] edges, int[] loops, int loopCount) {
            final var wanted = (int) Math.round(context.size() * (double) params.extraEdgesPer100k()
                    / DENSITY_AREA_TILES);
            Shuffles.shuffle(loops, loopCount, random);
            var cut = 0;
            for (var position = 0; position < loopCount && cut < wanted; position++) {
                final var edge = edges[loops[position]];
                if (join(context.sites().get(SiteEdges.from(edge)), context.sites().get(SiteEdges.to(edge)))) {
                    cut++;
                }
            }
        }

        /** Cuts a trail between two tiles when a route joins them, and tells whether it did. */
        private boolean join(int start, int goal) {
            if (ground.labelAt(start) != ground.labelAt(goal)) {
                return false;
            }
            final var length = search.find(start, goal, path);
            if (length == LeastCostPath.NOT_FOUND) {
                return false;
            }
            Carve.path(width, height, path, length, 1 + random.nextInt(params.maxWidth()), this::carve);
            return true;
        }

        /** Opens one tile of a trail: clears its resource, reserves it, turns a mountain into rock, makes it cheap. */
        private void carve(int index) {
            final var type = context.groundAt(index);
            if (type == TileType.MOUNTAIN) {
                context.setGround(index, TileType.ROCKY_GROUND);
            } else if (!type.isWalkable()) {
                return;
            }
            context.setResource(index, null);
            context.reserve(index);
            search.setCost(index, BASE_COST);
        }

        /** Returns the cost of every tile: infinite for water, and for a mountain when no trail may cut one. */
        private float[] costs() {
            final var trees = context.hasField(NoiseField.TREE) ? context.field(NoiseField.TREE)
                    : new float[context.size()];
            final var costs = new float[context.size()];
            for (var index = 0; index < costs.length; index++) {
                costs[index] = costOf(index, trees[index]);
            }
            return costs;
        }

        /** The cost of a tile from its ground, whether it is reserved, the tree noise and the resource it holds. */
        private float costOf(int index, float treeNoise) {
            final var type = context.groundAt(index);
            if (type == TileType.MOUNTAIN) {
                return params.mountainCost();
            }
            if (!type.isWalkable()) {
                return Float.POSITIVE_INFINITY;
            }
            if (context.isReserved(index)) {
                return BASE_COST;
            }
            final var resource = context.resourceAt(index) == null ? 0F : params.treeCost();
            return BASE_COST + params.noiseWeight() * treeNoise + resource;
        }
    }
}
