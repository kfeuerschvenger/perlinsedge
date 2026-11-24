package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationPass;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

import java.util.Objects;

/**
 * Scatters the palms and cacti of the desert over open sand, thinly in the dry sand and thickly within
 * {@link #PALM_REACH} tiles of any water, so the trees gather around the oases, lakes and rivers. The renderer draws
 * these tree resources as palms near water and as cacti elsewhere.
 * <p>
 * Which sand lies near water is worked out once for the whole map, by widening the water mask one axis at a time,
 * instead of scanning the neighbourhood of every sand tile.
 */
public final class DesertFloraPass implements GenerationPass {

    /** Sand within this many tiles of water carries palms, thickly; the rest carries cacti, thinly. */
    public static final int PALM_REACH = 2;

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
                if (context.groundAt(index) == TileType.SAND && context.resourceAt(index) == null) {
                    final var chance = nearWater[index] ? wetChance : dryChance;
                    if (Chance.unit(context, x, y, SALT) < chance) {
                        context.setResource(index, ResourceType.TREE);
                    }
                }
            }
        }
    }

    /** Returns the chance of a tree for a factor of the density, never above the largest chance. */
    private double chance(float factor) {
        return Math.min((double) params.maxChance(), (double) params.density() * factor);
    }

    /** Marks every tile that has a water tile within {@link #PALM_REACH} tiles in both axes, water included. */
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

    /** Tells whether a mask is set within {@link #PALM_REACH} tiles of a tile along the axis given by the step. */
    private static boolean anyWithin(boolean[] mask, GenerationContext context, int x, int y, int stepX, int stepY) {
        for (var offset = -PALM_REACH; offset <= PALM_REACH; offset++) {
            final var nx = x + offset * stepX;
            final var ny = y + offset * stepY;
            if (nx >= 0 && ny >= 0 && nx < context.width() && ny < context.height()
                    && mask[context.index(nx, ny)]) {
                return true;
            }
        }
        return false;
    }
}
