package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationPass;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;

import java.util.Objects;

/**
 * Puts stone on the land where the stone noise passes its threshold. Water and ice carry none, and a tile that already
 * holds a resource keeps it, so the ore the rock pass placed is never replaced.
 */
public final class StonePass implements GenerationPass {

    private static final String NAME = "stone";

    private final StoneParams params;

    /**
     * Creates the pass.
     *
     * @param params the threshold of the stone noise
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
        final var stoneNoise = context.field(NoiseField.STONE);
        for (var index = 0; index < context.size(); index++) {
            if (context.resourceAt(index) == null && Terrain.canHoldResource(context.groundAt(index))
                    && stoneNoise[index] > params.threshold()) {
                context.setResource(index, ResourceType.STONE);
            }
        }
    }
}
