package com.feuerschvenger.perlinsedge.domain.world.generation;

import com.feuerschvenger.perlinsedge.domain.world.model.Resource;
import com.feuerschvenger.perlinsedge.domain.world.model.SpawnPoint;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

/**
 * Turns the arrays of a finished context into the tiles of a map. It is the only place that creates {@link Tile} and
 * {@link Resource} objects, so the passes before it allocate nothing per tile. It holds no state, so it is a set of
 * static methods.
 */
public final class TileMapAssembler {

    /** Salt of the visual seed of the resources, which the renderer uses to vary how each one looks. */
    private static final long VISUAL_SEED_SALT = 0x7E57_AB1E_5EED_0001L;

    /** Salt of the jitter added to the patch value of a tile. */
    private static final long PATCH_JITTER_SALT = 0x9A7C_4E55_0B1D_0002L;

    /** The largest nudge of the patch value of a tile, which makes the borders of a patch ragged instead of smooth. */
    private static final double PATCH_JITTER_AMPLITUDE = 0.04;

    /** Low bits of a hash dropped so the rest fills the 53 bits of a double mantissa. */
    private static final int UNIT_SHIFT = 11;

    /** Width of the interval [-1, 1) a unit hash is mapped to. */
    private static final double UNIT_SPAN = 2.0;

    /** Not instantiable: the class only holds static methods. */
    private TileMapAssembler() {
    }

    /**
     * Creates every tile and resource of the context once. The height of a tile is the value of the height field when
     * a pass computed it and 0 otherwise, and the patch of a tile is the value of the patch field when the recipe
     * declared it, with a small jitter per tile. The visual seed of a resource is a hash of its tile, so it does not
     * depend on the order the tiles are visited in.
     *
     * @param context the finished generation
     * @return a new map with the spawn point of the context, if a pass chose one
     * @throws IllegalStateException when a tile has no terrain, because no pass classified it
     */
    public static TileMap materialize(GenerationContext context) {
        final var width = context.width();
        final var height = context.height();
        final var map = new TileMap(width, height, spawnOf(context));
        final var heights = context.hasField(NoiseField.HEIGHT) ? context.field(NoiseField.HEIGHT) : null;
        final var patches = context.hasField(NoiseField.PATCH) ? context.field(NoiseField.PATCH) : null;
        for (var y = 0; y < height; y++) {
            for (var x = 0; x < width; x++) {
                final var index = context.index(x, y);
                final var type = context.groundAt(index);
                if (type == null) {
                    throw new IllegalStateException("no pass classified the tile " + x + "," + y);
                }
                final var tile = new Tile(x, y, type);
                if (heights != null) {
                    tile.setHeight(heights[index]);
                }
                if (patches != null) {
                    tile.setPatch(patchOf(context, patches[index], x, y));
                }
                final var resource = context.resourceAt(index);
                if (resource != null) {
                    tile.setWorldResource(new Resource(resource, context.tileHash(x, y, VISUAL_SEED_SALT)));
                }
                map.setTile(x, y, tile);
            }
        }
        return map;
    }

    /** Quantizes a patch field value, nudged by the jitter of its tile, to the 0..255 a tile stores. */
    private static int patchOf(GenerationContext context, float value, int x, int y) {
        final var unit = ((context.tileHash(x, y, PATCH_JITTER_SALT) >>> UNIT_SHIFT) * 0x1.0p-53) * UNIT_SPAN - 1.0;
        return (int) Math.round((value + unit * PATCH_JITTER_AMPLITUDE) * Tile.MAX_PATCH);
    }

    /** Returns the spawn point the context chose, or null when no pass chose one. */
    private static SpawnPoint spawnOf(GenerationContext context) {
        final var index = context.spawnIndex();
        return index == GenerationContext.NONE ? null : new SpawnPoint(context.xOf(index), context.yOf(index));
    }
}
