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

    /** Not instantiable: the class only holds static methods. */
    private TileMapAssembler() {
    }

    /**
     * Creates every tile and resource of the context once. The height of a tile is the value of the height field when
     * a pass computed it and 0 otherwise. The visual seed of a resource is a hash of its tile, so it does not depend
     * on the order the tiles are visited in.
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
                final var resource = context.resourceAt(index);
                if (resource != null) {
                    tile.setWorldResource(new Resource(resource, context.tileHash(x, y, VISUAL_SEED_SALT)));
                }
                map.setTile(x, y, tile);
            }
        }
        return map;
    }

    /** Returns the spawn point the context chose, or null when no pass chose one. */
    private static SpawnPoint spawnOf(GenerationContext context) {
        final var index = context.spawnIndex();
        return index == GenerationContext.NONE ? null : new SpawnPoint(context.xOf(index), context.yOf(index));
    }
}
