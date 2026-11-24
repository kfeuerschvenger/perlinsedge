package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.world.generation.MapGenerator;
import com.feuerschvenger.perlinsedge.domain.world.generation.MapRequest;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseSettings;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseSource;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseSourceFactory;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.Resource;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.infra.noise.FastNoiseLiteAdapter;
import com.feuerschvenger.perlinsedge.ui.view.FxToolkitSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapManagerTest {

    /** Samples the real noise until told to fail. */
    private static final class SwitchableNoise implements NoiseSourceFactory {
        boolean failing;

        @Override
        public NoiseSource create(long seed, NoiseSettings settings) {
            if (failing) {
                throw new IllegalStateException("generation failed");
            }
            return FastNoiseLiteAdapter.FACTORY.create(seed, settings);
        }
    }

    private GameStateManager state;
    private SwitchableNoise noise;
    private MapManager manager;

    @BeforeEach
    void setUp() {
        state = new GameStateManager();
        noise = new SwitchableNoise();
        manager = new MapManager(state, new MapGenerator(noise));
    }

    private void generate(MapRequest context) {
        FxToolkitSupport.onFx(() -> {
            manager.generateNewMap(context);
            return null;
        });
    }

    @Test
    void aFailedGenerationLeavesTheContextAndTheMapOfTheLastSuccessfulOne() {
        MapRequest first = manager.createMapRequest(MapType.DESERT, 1L);
        generate(first);
        TileMap firstMap = state.getCurrentMap();

        noise.failing = true;
        MapRequest second = manager.createMapRequest(MapType.TUNDRA, 2L);
        assertThrows(IllegalStateException.class, () -> manager.generateNewMap(second));

        assertSame(first, manager.getCurrentRequest(), "the next regenerate starts from the map type on screen");
        assertSame(firstMap, state.getCurrentMap());
    }

    @Test
    void aFailedFirstGenerationLeavesNoContext() {
        noise.failing = true;

        MapRequest context = manager.createMapRequest(MapType.DESERT, 1L);
        assertThrows(IllegalStateException.class, () -> manager.generateNewMap(context));

        assertNull(manager.getCurrentRequest());
        assertNull(state.getCurrentMap());
    }

    @Test
    void aNewMapForgetsTheHitTimesOfTheOldOnesResources() {
        state.getResourceHitTimes().put(new Resource(ResourceType.STONE, 1), 5.0);

        generate(manager.createMapRequest(MapType.DESERT, 1L));

        assertTrue(state.getResourceHitTimes().isEmpty());
    }

    @Test
    void theRequestTakesItsTypeFromTheArgumentAndItsSizeFromTheWorld() {
        final var world = AppConfig.getInstance().world();
        final var before = world.getCurrentMapType();
        final var other = before == MapType.DESERT ? MapType.TUNDRA : MapType.DESERT;

        final var request = manager.createMapRequest(other, 3L);

        assertEquals(other, request.type());
        assertEquals(3L, request.seed());
        assertEquals(world.getWidth(), request.width());
        assertEquals(world.getHeight(), request.height());
        assertEquals(before, world.getCurrentMapType(), "making a request does not touch the world config");
    }

    @Test
    void aGeneratedMapKeepsItsSpawnPointAndTheSeedIsRemembered() {
        final var request = manager.createMapRequest(MapType.DESERT, 9L);

        generate(request);

        assertTrue(state.getCurrentMap().getSpawn().isPresent());
        assertEquals(9L, state.getWorldSeed());
    }

    @Test
    void aSuccessfulGenerationBecomesTheCurrentContext() {
        MapRequest context = manager.createMapRequest(MapType.TUNDRA, 5L);

        generate(context);

        assertSame(context, manager.getCurrentRequest());
    }
}
