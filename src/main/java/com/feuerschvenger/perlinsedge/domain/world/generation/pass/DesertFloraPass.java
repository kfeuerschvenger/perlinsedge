package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationPass;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

import java.util.Objects;

import static com.feuerschvenger.perlinsedge.domain.world.model.ResourceType.PALM_REACH;

/**
 * Scatters the palms and cacti of the desert over open sand, thinly in the dry sand and thickly within
 * {@link ResourceType#PALM_REACH} tiles of any water, so the trees gather around the oases, lakes and rivers. The
 * renderer draws these tree resources as palms near water and as cacti elsewhere.
 * <p>
 * Which sand lies near water is worked out once for the whole map, by widening the water mask one axis at a time,
 * instead of scanning the neighbourhood of every sand tile.
 */
public final class DesertFloraPass implements GenerationPass {

    private static final String NAME = "desert-flora";
    private static final long SALT = 0x0DE5_E27F_10A4_0001L;

    private final DesertFloraParams params;

    /**
     * Creates the pass.
     *
     * @param params the base density, the factors for dry and wet sand and the largest chance
     */
    public DesertFloraPass(DesertFloraParams params) {
        this.params = Objects.requireNonNull(params, "params");
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void apply(GenerationContext context) {
        final var nearWater = nearWater(context);
        final var dryChance = chance(params.dryFactor());
        final var wetChance = chance(params.wetFactor());
        for (var y = 0; y < context.height(); y++) {
            for (var x = 0; x < context.width(); x++) {
                final var index = context.index(x, y);
                if (isOpenSand(context, index)
                        && Chance.unit(context, x, y, SALT) < (nearWater[index] ? wetChance : dryChance)) {
                    context.setResource(index, ResourceType.TREE);
                }
            }
        }
    }

    /** Tells whether a tile is sand with no resource on it. */
    private static boolean isOpenSand(GenerationContext context, int index) {
        return context.groundAt(index) == TileType.SAND && context.resourceAt(index) == null;
    }

    /** Returns the chance of a tree for a factor of the density, never above the largest chance. */
    private double chance(float factor) {
        return Math.min(params.maxChance(), (double) params.density() * factor);
    }

    /**
     * Marks every tile that has a water tile within {@link ResourceType#PALM_REACH} tiles in both axes, water
     * included.
     */
    private static boolean[] nearWater(GenerationContext context) {
        final var width = context.width();
        final var height = context.height();
        final var water = new boolean[context.size()];
        for (var index = 0; index < water.length; index++) {
            water[index] = context.groundAt(index).isWater();
        }
        final var widenedAcross = new boolean[water.length];
        for (var y = 0; y < height; y++) {
            for (var x = 0; x < width; x++) {
                widenedAcross[context.index(x, y)] = anyWithin(water, context, x, y, 1, 0);
            }
        }
        final var near = new boolean[water.length];
        for (var y = 0; y < height; y++) {
            for (var x = 0; x < width; x++) {
                near[context.index(x, y)] = anyWithin(widenedAcross, context, x, y, 0, 1);
            }
        }
        return near;
    }

    /**
     * Tells whether a mask is set within {@link ResourceType#PALM_REACH} tiles of a tile along the axis given by the
     * step.
     */
    private static boolean anyWithin(boolean[] mask, GenerationContext context, int x, int y, int stepX, int stepY) {
        for (var offset = -PALM_REACH; offset <= PALM_REACH; offset++) {
            final var nx = x + offset * stepX;
            final var ny = y + offset * stepY;
            if (context.isOnMap(nx, ny) && mask[context.index(nx, ny)]) {
                return true;
            }
        }
        return false;
    }
}
