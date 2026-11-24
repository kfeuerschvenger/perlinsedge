package com.feuerschvenger.perlinsedge.domain.world.generation.support;

import com.feuerschvenger.perlinsedge.domain.world.generation.MapGenerator;
import com.feuerschvenger.perlinsedge.domain.world.generation.PassObserver;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.infra.noise.FastNoiseLiteAdapter;

/**
 * The seam between the harness and whatever generates maps. The invariants, the painter and the preview tool only know
 * this interface, so a different generator means writing one more source and changing {@link #current()}.
 */
@FunctionalInterface
public interface MapSource {

    /**
     * Generates the map a spec describes.
     *
     * @param spec the type, seed and size
     * @return a freshly generated map
     */
    TileMap generate(MapSpec spec);

    /** @return the source the game uses, which is the pipeline generator with the real noise */
    static MapSource current() {
        return observed(PassObserver.NONE);
    }

    /**
     * Creates the source of the game that also tells an observer how long each pass of every map took.
     *
     * @param observer told the name and the time of each pass
     * @return the source
     */
    static MapSource observed(PassObserver observer) {
        final var generator = new MapGenerator(FastNoiseLiteAdapter.FACTORY);
        return (MapSpec spec) -> generator.generate(spec.toRequest(), observer);
    }
}
