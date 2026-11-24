package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

import java.util.Arrays;
import java.util.random.RandomGenerator;

/**
 * Adds a resource to the free tiles of rocky ground, zone by zone from the largest. The zones are the 4-connected
 * regions of rocky ground, labelled once. A first round gives each zone, largest first, at most a share of its tiles;
 * when the zones are used up before the resource is, a second round takes whatever free tiles are left. The tiles of a
 * zone are taken in a random order.
 */
final class RockyFill {

    private static final int PER_MILLE = 1000;
    private static final int LABEL_BITS = 32;
    private static final long LABEL_MASK = 0xFFFF_FFFFL;

    private final GenerationContext context;
    private final RandomGenerator random;
    private final Regions zones;
    /** The zones as {@code size << 32 | label}, so sorting puts the largest last. */
    private final long[] bySize;

    RockyFill(GenerationContext context, RandomGenerator random) {
        this.context = context;
        this.random = random;
        this.zones = Regions.label(context.width(), context.height(),
                index -> context.groundAt(index) == TileType.ROCKY_GROUND);
        this.bySize = new long[zones.count()];
        for (var label = 1; label <= bySize.length; label++) {
            bySize[label - 1] = ((long) zones.sizeOf(label) << LABEL_BITS) | label;
        }
        Arrays.sort(bySize);
    }

    /**
     * Puts a resource on up to {@code missing} free rocky tiles.
     *
     * @param quota   the quota of the resource, which gives its type and how much of a zone it may take in the first
     *                round
     * @param missing how many tiles to add
     */
    void add(ResourceQuota quota, int missing) {
        final var free = new Free(context, zones);
        final var left = round(quota, missing, free, true);
        round(quota, left, free, false);
    }

    /** Fills the zones from the largest and returns how many tiles are still missing. */
    private int round(ResourceQuota quota, int missing, Free free, boolean capped) {
        var left = missing;
        for (var position = bySize.length - 1; position >= 0 && left > 0; position--) {
            final var label = (int) (bySize[position] & LABEL_MASK);
            final var open = free.count[label] - free.taken[label];
            final var room = capped ? Math.max(0, capOf(label, quota) - free.taken[label]) : open;
            final var take = Math.min(left, Math.min(room, open));
            place(quota, free, label, take);
            left -= take;
        }
        return left;
    }

    private int capOf(int label, ResourceQuota quota) {
        return Math.max(1, zones.sizeOf(label) * quota.zoneCapPerMille() / PER_MILLE);
    }

    /** Picks {@code take} random free tiles of a zone that are not used yet and gives them the resource. */
    private void place(ResourceQuota quota, Free free, int label, int take) {
        final var from = free.start[label] + free.taken[label];
        Shuffles.partial(free.tiles, from, free.count[label] - free.taken[label], take, random);
        for (var offset = 0; offset < take; offset++) {
            context.setResource(free.tiles[from + offset], quota.type());
        }
        free.taken[label] += take;
    }

    /**
     * The free tiles of the zones grouped by zone: the tiles of zone {@code label} are
     * {@code tiles[start[label]]} onwards, {@code count[label]} of them, and {@code taken[label]} of those are used.
     */
    private static final class Free {
        private final int[] tiles;
        private final int[] start;
        private final int[] count;
        private final int[] taken;

        private Free(GenerationContext context, Regions zones) {
            count = new int[zones.count() + 1];
            var total = 0;
            for (var index = 0; index < context.size(); index++) {
                if (zones.labelAt(index) != Regions.NO_LABEL && isFree(context, index)) {
                    count[zones.labelAt(index)]++;
                    total++;
                }
            }
            start = new int[count.length];
            for (var label = 1; label < count.length; label++) {
                start[label] = start[label - 1] + count[label - 1];
            }
            final var next = start.clone();
            tiles = new int[total];
            for (var index = 0; index < context.size(); index++) {
                final var label = zones.labelAt(index);
                if (label != Regions.NO_LABEL && isFree(context, index)) {
                    tiles[next[label]] = index;
                    next[label]++;
                }
            }
            taken = new int[count.length];
        }

        private static boolean isFree(GenerationContext context, int index) {
            return context.resourceAt(index) == null && !context.isReserved(index);
        }
    }
}
