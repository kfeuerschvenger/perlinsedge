package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationPass;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;

import java.util.Objects;

/**
 * Grows trees on fertile ground that holds no resource yet, in the groves where the tree noise is high. The density is
 * the share of the noise range that grows a tree: a tree grows where the noise exceeds one minus the density.
 */
public final class VegetationPass implements GenerationPass {

    private static final String NAME = "vegetation";

    private final VegetationParams params;

    /**
     * Creates the pass.
     *
     * @param params how thickly the trees grow
     */
    public VegetationPass(VegetationParams params) {
        this.params = Objects.requireNonNull(params, "params");
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void apply(GenerationContext context) {
        final var treeNoise = context.field(NoiseField.TREE);
        final var threshold = 1.0 - params.density();
        for (var index = 0; index < context.size(); index++) {
            if (context.resourceAt(index) == null && context.groundAt(index).isFertile()
                    && treeNoise[index] > threshold) {
                context.setResource(index, ResourceType.TREE);
            }
        }
    }
}
