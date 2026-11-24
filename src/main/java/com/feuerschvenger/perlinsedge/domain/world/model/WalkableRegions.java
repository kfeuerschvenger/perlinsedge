package com.feuerschvenger.perlinsedge.domain.world.model;

import com.feuerschvenger.perlinsedge.domain.utils.IsometricUtils;

import java.util.Arrays;

/**
 * Labels the walkable tiles of a map by region, so a walk order to a tile that no path can ever reach is refused at
 * once instead of by a search that explores every reachable tile before giving up.
 *
 * <h2>What a region is</h2>
 * A region is a maximal set of walkable tiles in which any tile can be walked to from any other. Two tiles share a
 * region exactly when the A* search of the game can link them on the bare map. Every walkable tile belongs to one
 * region, numbered from 1, and a tile that cannot be walked on has {@link #NO_REGION}.
 *
 * <h2>Connectivity rule</h2>
 * Tiles are joined through their four edges. That is also what the 8-directional movement of the game amounts to: a
 * diagonal step is only allowed when both tiles it passes between are walkable, so the two tiles it joins are already
 * joined through either of them, and a diagonal step never links two tiles that edge steps do not. A diagonal that
 * would cut the corner of a blocked tile joins nothing, in the labelling as in the search.
 *
 * <h2>What blocks a region</h2>
 * Walkability is the one of {@link Tile#isWalkable()}: water without a bridge, solid buildings (a closed gate, a
 * fence, a workbench), and standing resources such as trees and rocks all split regions. A bridge or a floor makes a
 * water tile walkable and joins what it touches; an open gate, a harvested tree and a demolished building do the same.
 * Enemies are not part of the labelling. They move, and a tile an enemy stands on is only held for as long as the
 * enemy is there, so a search that has to go around enemies still runs and may still fail, but it is never refused
 * wrongly. The index therefore only answers "no path exists", never "a path exists".
 *
 * <h2>When it is rebuilt</h2>
 * The labelling is worked out lazily. Every query compares {@link TileMap#getRevision()} with the revision the labels
 * were worked out at, and relabels the whole map when they differ. Placing or demolishing a building or a bridge,
 * opening or closing a gate, harvesting a resource, changing a terrain type, putting a tile into the map and a world
 * rebuild all raise the revision, so no change of walkability can be missed, and many changes between two queries cost
 * one relabelling. A query on an unchanged map costs a comparison and an array read.
 *
 * <h2>Cost</h2>
 * A relabelling is one flood fill over primitive arrays, linear in the number of tiles and with no allocation per
 * tile: the two arrays it uses are allocated once with the index. On a 300x300 map it takes a couple of milliseconds.
 *
 * <p>The index is not thread safe, like the map it belongs to. Get it with {@link TileMap#getWalkableRegions()}.
 */
public final class WalkableRegions {

    /** The label of a tile that cannot be walked on, and of any position outside the map. */
    public static final int NO_REGION = 0;

    /** What the flood fill gets back for a tile it did not take, which is no tile index. */
    private static final int NOT_CLAIMED = -1;

    /** Revision no map has, so the first query always labels the map. */
    private static final long NEVER_BUILT = -1L;

    /** The four edge neighbours, as column and row offsets. */
    private static final int[][] EDGE_STEPS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    /** The eight tiles around a tile, as column and row offsets. */
    private static final int[][] SURROUNDING = IsometricUtils.allDirections();

    private final TileMap map;
    private final int width;
    /** The region of each tile, at {@code y * width + x}. */
    private final int[] regionOf;
    /** Tiles found by the flood fill and not expanded yet, reused by every relabelling. */
    private final int[] pending;
    private long builtRevision = NEVER_BUILT;
    private int regionCount;
    private int relabelCount;

    /**
     * Creates the index of a map, which labels nothing until the first query.
     *
     * @param map the map whose walkable tiles are labelled
     */
    WalkableRegions(TileMap map) {
        this.map = map;
        this.width = map.getWidth();
        final var tileCount = map.getWidth() * map.getHeight();
        this.regionOf = new int[tileCount];
        this.pending = new int[tileCount];
    }

    /**
     * Gives the region of a tile.
     *
     * @param x tile column
     * @param y tile row
     * @return the region number from 1, or {@link #NO_REGION} when the tile cannot be walked on or is outside the map
     */
    public int regionAt(int x, int y) {
        if (!map.isValidCoordinate(x, y)) {
            return NO_REGION;
        }
        refresh();
        return regionOf[y * width + x];
    }

    /**
     * Counts the regions of the map as it is now.
     *
     * @return the number of separate regions of walkable tiles
     */
    public int getRegionCount() {
        refresh();
        return regionCount;
    }

    /**
     * Tells whether a walk to a tile can exist, so that a refusal is certain and an acceptance only means that a search
     * is worth running. A walk is ruled out when the tiles are in different regions or when the target cannot be
     * walked on. A start tile that cannot be walked on, such as one that became solid under the walker, is not ruled
     * out, because a search may leave such a tile.
     *
     * @param startX  column the walk starts from
     * @param startY  row the walk starts from
     * @param targetX column of the tile to walk to
     * @param targetY row of the tile to walk to
     * @return false when no path can exist, true when one may
     */
    public boolean mayReach(int startX, int startY, int targetX, int targetY) {
        final var startRegion = regionAt(startX, startY);
        if (startRegion == NO_REGION) {
            return true;
        }
        return startRegion == regionAt(targetX, targetY);
    }

    /**
     * Tells whether a walk to a tile next to a target can exist. The target itself does not need to be walkable, so a
     * tree or a building is reached from any walkable tile around it that lies in the region of the start. The same
     * guarantees as {@link #mayReach(int, int, int, int)} hold.
     *
     * @param startX  column the walk starts from
     * @param startY  row the walk starts from
     * @param targetX column of the tile to get next to
     * @param targetY row of the tile to get next to
     * @return false when no tile around the target can be reached, true when one may
     */
    public boolean mayReachNextTo(int startX, int startY, int targetX, int targetY) {
        final var startRegion = regionAt(startX, startY);
        if (startRegion == NO_REGION) {
            return true;
        }
        for (final int[] step : SURROUNDING) {
            if (regionAt(targetX + step[0], targetY + step[1]) == startRegion) {
                return true;
            }
        }
        return false;
    }

    /**
     * Counts the relabellings so far, which shows that the map is labelled once for any number of changes between two
     * queries and not at all while it stays as it is.
     *
     * @return how many times the whole map has been labelled
     */
    public int getRelabelCount() {
        return relabelCount;
    }

    /** Labels the map again when it changed since the labels were worked out. */
    private void refresh() {
        final var revision = map.getRevision();
        if (revision != builtRevision) {
            relabel();
            builtRevision = revision;
        }
    }

    /** Labels every walkable tile, one flood fill per region. */
    private void relabel() {
        Arrays.fill(regionOf, NO_REGION);
        regionCount = 0;
        relabelCount++;
        for (var y = 0; y < map.getHeight(); y++) {
            for (var x = 0; x < width; x++) {
                if (regionOf[y * width + x] == NO_REGION && isWalkable(x, y)) {
                    regionCount++;
                    fill(x, y, regionCount);
                }
            }
        }
    }

    /** Gives a region number to every walkable tile that can be reached from a tile through edge steps. */
    private void fill(int startX, int startY, int region) {
        var head = 0;
        var tail = 0;
        final var start = startY * width + startX;
        regionOf[start] = region;
        pending[tail++] = start;
        while (head < tail) {
            final var index = pending[head++];
            final var x = index % width;
            final var y = index / width;
            for (final int[] step : EDGE_STEPS) {
                final var neighbour = claim(x + step[0], y + step[1], region);
                if (neighbour != NOT_CLAIMED) {
                    pending[tail++] = neighbour;
                }
            }
        }
    }

    /**
     * Gives a tile to a region when it is walkable and still unlabelled.
     *
     * @return the index of the tile when it was claimed, {@link #NOT_CLAIMED} otherwise
     */
    private int claim(int x, int y, int region) {
        if (!map.isValidCoordinate(x, y)) {
            return NOT_CLAIMED;
        }
        final var index = y * width + x;
        if (regionOf[index] != NO_REGION || !isWalkable(x, y)) {
            return NOT_CLAIMED;
        }
        regionOf[index] = region;
        return index;
    }

    private boolean isWalkable(int x, int y) {
        final var tile = map.getTile(x, y);
        return tile != null && tile.isWalkable();
    }
}
