package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

/**
 * Works out which sides of a fence are joined to a neighbour. A fence is a post with a half rail toward each of its four
 * edge neighbours that is a fence, or a gate that runs along the line between the two. The sides are named by the edge of
 * the tile diamond on the screen they face, and each is a bit of the mask:
 * <pre>
 *   upper left  (-1, 0)  UL     upper right (0, -1)  UR
 *   lower left  (0, +1)  LL     lower right (+1, 0)  LR
 * </pre>
 * The tile offsets follow from the isometric projection, where tile (x + 1, y) is drawn toward the lower right and
 * tile (x, y + 1) toward the lower left.
 */
public final class FenceConnections {
    /** The side toward the tile at (-1, 0), drawn toward the upper left edge. */
    public static final int UPPER_LEFT = 1;
    /** The side toward the tile at (0, -1), drawn toward the upper right edge. */
    public static final int UPPER_RIGHT = 2;
    /** The side toward the tile at (0, +1), drawn toward the lower left edge. */
    public static final int LOWER_LEFT = 4;
    /** The side toward the tile at (+1, 0), drawn toward the lower right edge. */
    public static final int LOWER_RIGHT = 8;

    /** The tile offset of each side, in the order of the bits above. */
    private static final int[][] OFFSETS = {{-1, 0}, {0, -1}, {0, 1}, {1, 0}};
    private static final int[] BITS = {UPPER_LEFT, UPPER_RIGHT, LOWER_LEFT, LOWER_RIGHT};

    /** Not instantiable: the class only holds the rules. */
    private FenceConnections() {
    }

    /**
     * Gives the sides of a tile that a fence standing on it would join.
     *
     * @param map the map
     * @param x   tile X
     * @param y   tile Y
     * @return the mask of the joined sides, 0 for a lone post
     */
    public static int maskAt(TileMap map, int x, int y) {
        int mask = 0;
        for (int i = 0; i < OFFSETS.length; i++) {
            final Tile neighbour = map.getTile(x + OFFSETS[i][0], y + OFFSETS[i][1]);
            if (neighbour != null && joins(neighbour.getBuilding(), OFFSETS[i][0], OFFSETS[i][1])) {
                mask |= BITS[i];
            }
        }
        return mask;
    }

    /**
     * Tells whether a fence next to a building joins it.
     *
     * @param neighbour the building on the neighbouring tile, may be null
     * @param dx        tile offset along X from the fence to the neighbour
     * @param dy        tile offset along Y from the fence to the neighbour
     * @return true for another fence, and for a gate whose axis runs through the fence
     */
    public static boolean joins(Building neighbour, int dx, int dy) {
        if (neighbour instanceof Fence) {
            return true;
        }
        return neighbour instanceof FenceGate gate && gate.runsToward(dx, dy);
    }

    /**
     * Works out again the sides of the fence on a tile and on the four tiles around it, after a fence or a gate was put
     * on the tile or taken off it. A fence whose sides changed tells the map, so what depends on it is drawn again.
     *
     * @param map the map
     * @param x   the tile that changed
     * @param y   the tile that changed
     */
    public static void refresh(TileMap map, int x, int y) {
        refreshOne(map, x, y);
        for (int[] offset : OFFSETS) {
            refreshOne(map, x + offset[0], y + offset[1]);
        }
    }

    private static void refreshOne(TileMap map, int x, int y) {
        final Tile tile = map.getTile(x, y);
        if (tile != null && tile.getBuilding() instanceof Fence fence) {
            final int mask = maskAt(map, x, y);
            if (mask != fence.getConnections()) {
                fence.setConnections(mask);
                tile.buildingChanged();
            }
        }
    }
}
