package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationPass;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;

import java.util.Objects;

/**
 * Puts stone on the rocky ground, a share of its tiles, and rare loose stones on any other walkable land that can hold
 * a resource, which gives a map with no rock, such as the islets of a swamp, a few stones to pick up. Water, ice and
 * mountains carry none, and a tile that already holds a resource keeps it, so the ore the rock pass placed is never
 * replaced. Reserved tiles get none. Both chances are hashes of the tile, so the result repeats for a seed.
 */
public final class StonePass implements GenerationPass {

    private static final String NAME = "stone";
    private static final long ROCKY_SALT = 0x510E_570E_A1B2_C3D4L;
    private static final long LOOSE_SALT = 0x1005_E570_E5EE_DF00L;

    private final StoneParams params;

    /**
     * Creates the pass.
     *
     * @param params the chance of stone on rocky ground and of a loose stone elsewhere
     */
    public StonePass(StoneParams params) {
        this.params = Objects.requireNonNull(params, "params");
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void apply(GenerationContext context) {
        for (var y = 0; y < context.height(); y++) {
            for (var x = 0; x < context.width(); x++) {
                final var index = context.index(x, y);
                if (context.resourceAt(index) == null && !context.isReserved(index)
                        && hasStone(context, index, x, y)) {
                    context.setResource(index, ResourceType.STONE);
                }
            }
        }
    }

    /** Tells whether a tile without a resource gets stone: by the rocky chance on rock, by the loose one on land. */
    private boolean hasStone(GenerationContext context, int index, int x, int y) {
        final var ground = context.groundAt(index);
        if (Terrain.isRocky(ground)) {
            return Chance.perMille(context, x, y, ROCKY_SALT, params.rockyPerMille());
        }
        return ground.isWalkable() && Terrain.canHoldResource(ground)
                && Chance.perTenThousand(context, x, y, LOOSE_SALT, params.loosePerTenThousand());
    }
}
