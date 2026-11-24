package com.feuerschvenger.perlinsedge.domain.world.generation.recipe;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationPass;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;

import java.util.List;
import java.util.Objects;

/**
 * How one type of map is built: the ordered list of configured passes that produce it.
 *
 * @param type   the kind of map the recipe builds
 * @param passes the passes, in the order they run, copied so the recipe cannot change afterwards
 */
public record MapRecipe(MapType type, List<GenerationPass> passes) {

    /** Checks the type and the passes are set and copies the list. */
    public MapRecipe {
        Objects.requireNonNull(type, "type");
        passes = List.copyOf(passes);
    }
}
