package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationPass;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;

import java.util.Objects;

/**
 * Scatters iron ore and crystals thinly over the rock, so they stay a find. Only rocky ground and mountains hold them.
 * Each rocky tile is ore with a small chance, and a rocky tile inside a crystal patch, where the crystal noise passes
 * its threshold, is crystals with a chance of its own; a tile that passes both is crystals. Both counts are proportional to
 * the area of rocky terrain, so a map with a little rock still has a few and a mountain range has some dozens.
 * <p>
 * The chances are hashes of the tile, the seed and a salt of each resource, so a map is the same every time it is
 * generated from a seed and the two chances are independent of each other. The pass runs before stone and trees, so how
 * much stone there is never changes how much ore there is.
 */
public final class RockResourcePass implements GenerationPass {

    private static final String NAME = "rock-resources";
    private static final long IRON_SALT = 0x5DEE_CE66_D1CE_4E5BL;
    private static final long CRYSTALS_SALT = 0xC0DE_D00D_B16B_00B5L;

    private final RockResourceParams params;

    /**
     * Creates the pass.
     *
     * @param params the chance of each resource and the crystal patch threshold
     */
    public RockResourcePass(RockResourceParams params) {
        this.params = Objects.requireNonNull(params, "params");
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void apply(GenerationContext context) {
        final var crystalNoise = context.field(NoiseField.CRYSTALS);
        for (var y = 0; y < context.height(); y++) {
            for (var x = 0; x < context.width(); x++) {
                final var index = context.index(x, y);
                if (Terrain.isRocky(context.groundAt(index))) {
                    context.setResource(index, resourceOf(context, x, y, crystalNoise[index]));
                }
            }
        }
    }

    /** Returns what a rocky tile holds, crystals before iron, or null for nothing. */
    private ResourceType resourceOf(GenerationContext context, int x, int y, float crystalValue) {
        if (crystalValue > params.crystalsThreshold()
                && Chance.perMille(context, x, y, CRYSTALS_SALT, params.crystalsPerMille())) {
            return ResourceType.CRYSTALS;
        }
        return Chance.perMille(context, x, y, IRON_SALT, params.ironPerMille()) ? ResourceType.IRON_ORE : null;
    }
}
