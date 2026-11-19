package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.domain.world.generation.IMapGenerator;
import com.feuerschvenger.perlinsedge.domain.world.generation.MapGenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.MapGeneratorFactory;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.Resource;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.infra.noise.FastNoiseLiteAdapter;
import com.feuerschvenger.perlinsedge.ui.view.FxToolkitSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapManagerTest {

    /** Builds real generators until told to fail. */
    private static final class SwitchableFactory extends MapGeneratorFactory {
        boolean failing;

        SwitchableFactory() {
            super(FastNoiseLiteAdapter.FACTORY);
        }

        @Override
        public IMapGenerator createGenerator(MapType mapType) {
            if (failing) {
                throw new IllegalStateException("generation failed");
            }
            return super.createGenerator(mapType);
        }
    }

    private GameStateManager state;
    private SwitchableFactory factory;
    private MapManager manager;

    @BeforeEach
    void setUp() {
        state = new GameStateManager();
        factory = new SwitchableFactory();
        manager = new MapManager(state, factory);
    }

    private void generate(MapGenerationContext context) {
        FxToolkitSupport.onFx(() -> {
            manager.generateNewMap(context);
            return null;
        });
    }

    @Test
    void aFailedGenerationLeavesTheContextAndTheMapOfTheLastSuccessfulOne() {
        MapGenerationContext first = manager.createMapContext(MapType.DESERT, 1L);
        generate(first);
        TileMap firstMap = state.getCurrentMap();

        factory.failing = true;
        MapGenerationContext second = manager.createMapContext(MapType.TUNDRA, 2L);
        assertThrows(IllegalStateException.class, () -> manager.generateNewMap(second));

        assertSame(first, manager.getCurrentMapContext(), "the next regenerate starts from the map type on screen");
        assertSame(firstMap, state.getCurrentMap());
    }

    @Test
    void aFailedFirstGenerationLeavesNoContext() {
        factory.failing = true;

        MapGenerationContext context = manager.createMapContext(MapType.DESERT, 1L);
        assertThrows(IllegalStateException.class, () -> manager.generateNewMap(context));

        assertNull(manager.getCurrentMapContext());
        assertNull(state.getCurrentMap());
    }

    @Test
    void aNewMapForgetsTheHitTimesOfTheOldOnesResources() {
        state.getResourceHitTimes().put(new Resource(ResourceType.STONE, 1), 5.0);

        generate(manager.createMapContext(MapType.DESERT, 1L));

        assertTrue(state.getResourceHitTimes().isEmpty());
    }

    @Test
    void aSuccessfulGenerationBecomesTheCurrentContext() {
        MapGenerationContext context = manager.createMapContext(MapType.TUNDRA, 5L);

        generate(context);

        assertSame(context, manager.getCurrentMapContext());
    }
}
