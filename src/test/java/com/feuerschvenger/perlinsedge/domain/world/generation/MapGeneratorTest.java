package com.feuerschvenger.perlinsedge.domain.world.generation;

import com.feuerschvenger.perlinsedge.domain.world.generation.recipe.MapRecipe;
import com.feuerschvenger.perlinsedge.domain.world.generation.recipe.MapRecipes;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.MapStats;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import com.feuerschvenger.perlinsedge.infra.noise.FastNoiseLiteAdapter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapGeneratorTest {

    private static final int SIZE = 48;
    private static final int FULL_SIZE = 150;
    private static final int TINY = 8;
    private static final int SPAWN_RADIUS = 3;
    private static final int ROCKY_SEEDS = 6;
    private static final int ROCK_SEEDS_FOR_RATIO = 3;
    private static final double STONE_PER_IRON_ON_ROCK = 3.0;
    private static final int STONE_PER_IRON_ELSEWHERE = 2;

    private final MapGenerator generator = new MapGenerator(FastNoiseLiteAdapter.FACTORY);

    private TileMap generate(MapType type, long seed, int size) {
        return generator.generate(new MapRequest(type, seed, size, size));
    }

    private static boolean sameTerrain(TileMap a, TileMap b) {
        for (var y = 0; y < a.getHeight(); y++) {
            for (var x = 0; x < a.getWidth(); x++) {
                if (Float.compare(a.getTile(x, y).getHeight(), b.getTile(x, y).getHeight()) != 0
                        || a.getTile(x, y).getType() != b.getTile(x, y).getType()) {
                    return false;
                }
            }
        }
        return true;
    }

    @Test
    void sameSeedAndRequestProduceTheSameTerrainAndSpawn() {
        final var first = generate(MapType.DENSE_FOREST, 42L, SIZE);
        final var second = generate(MapType.DENSE_FOREST, 42L, SIZE);

        assertTrue(sameTerrain(first, second));
        assertEquals(first.getSpawn(), second.getSpawn());
    }

    @Test
    void seedsThatDifferOnlyInTheHighBitsProduceDifferentTerrain() {
        final var low = 12_345L;
        final var high = low | (7L << 40);

        assertEquals((int) low, (int) high, "precondition: a narrowing cast cannot tell these seeds apart");
        assertFalse(sameTerrain(generate(MapType.DENSE_FOREST, low, SIZE), generate(MapType.DENSE_FOREST, high, SIZE)));
    }

    @Test
    void theFullLongSeedReachesEveryNoiseSource() {
        final var seeds = new ArrayList<Long>();
        final NoiseSourceFactory recording = (long seed, NoiseSettings settings) -> {
            seeds.add(seed);
            return (float x, float y) -> 0F;
        };
        final var seed = 0x1234_5678_9ABC_DEF0L;

        new MapGenerator(recording).generate(new MapRequest(MapType.DENSE_FOREST, seed, TINY, TINY));

        assertEquals(List.of(seed, seed + NoiseField.ROCK.seedOffset(),
                seed + NoiseField.CRYSTALS.seedOffset(), seed + NoiseField.TREE.seedOffset()), seeds);
    }

    @Test
    void everyMapHasASpawnOnFreeGroundInsideTheMap() {
        for (var type : MapType.values()) {
            final var map = generate(type, 1L, SIZE);

            final var spawn = map.getSpawn().orElseThrow();
            assertTrue(MapStats.isFree(map.getTile(spawn.x(), spawn.y())), type.name());
        }
    }

    @Test
    void aRecipeWithoutASpawnPassIsRefused() {
        final GenerationPass grass = new GenerationPass() {
            @Override
            public String name() {
                return "grass";
            }

            @Override
            public void apply(GenerationContext context) {
                for (var index = 0; index < context.size(); index++) {
                    context.setGround(index, TileType.GRASS);
                }
            }
        };
        final var recipes = new MapRecipes((MapType type) -> new MapRecipe(type, List.of(grass)));
        final var request = new MapRequest(MapType.DESERT, 1L, TINY, TINY);
        final var withoutSpawn = new MapGenerator(FastNoiseLiteAdapter.FACTORY, recipes);

        assertThrows(IllegalStateException.class, () -> withoutSpawn.generate(request));
    }

    @Test
    void theObserverHearsOfEveryPassOfTheRecipeInOrder() {
        final var heard = new ArrayList<String>();
        final var request = new MapRequest(MapType.DESERT, 1L, SIZE, SIZE);

        generator.generate(request, (String name, long nanos) -> heard.add(name));

        final var expected = new MapRecipes().recipeFor(MapType.DESERT).passes().stream()
                .map(GenerationPass::name).toList();
        assertEquals(expected, heard);
    }

    @Test
    void theGeneratorRequiresItsCollaboratorsAndARequest() {
        assertThrows(NullPointerException.class, () -> new MapGenerator(null));
        assertThrows(NullPointerException.class, () -> new MapGenerator(FastNoiseLiteAdapter.FACTORY, null));
        assertThrows(NullPointerException.class, () -> generator.generate(null));
    }

    @Test
    void ironOreIsGeneratedOnRockyMapsNextToStone() {
        var iron = 0;
        var stone = 0;
        for (var seed = 1L; seed <= ROCKY_SEEDS; seed++) {
            final var map = generate(MapType.MOUNTAINOUS, seed, FULL_SIZE);
            iron += MapStats.count(map, ResourceType.IRON_ORE);
            stone += MapStats.count(map, ResourceType.STONE);
        }

        assertTrue(iron > 0, "iron ore must be obtainable");
        assertTrue(stone > 0, "stone is still generated");
        assertTrue((double) stone / iron >= STONE_PER_IRON_ON_ROCK,
                "there are " + stone + " stones for " + iron + " iron");
    }

    @Test
    void ironIsClearlyRarerThanStoneOnEveryMapType() {
        for (var type : MapType.values()) {
            var iron = 0;
            var stone = 0;
            for (var seed = 1L; seed <= ROCK_SEEDS_FOR_RATIO; seed++) {
                final var map = generate(type, seed, FULL_SIZE);
                iron += MapStats.count(map, ResourceType.IRON_ORE);
                stone += MapStats.count(map, ResourceType.STONE);
            }

            assertTrue(stone > 0, "stone is generated on " + type);
            assertTrue(iron * STONE_PER_IRON_ELSEWHERE < stone,
                    type + ": " + iron + " iron against " + stone + " stone");
        }
    }

    @Test
    void theSpawnKeepsTheResourcesOfItsRadiusAway() {
        for (var type : MapType.values()) {
            final var map = generate(type, 2L, SIZE);
            final var spawn = map.getSpawn().orElseThrow();

            for (var dy = -SPAWN_RADIUS; dy <= SPAWN_RADIUS; dy++) {
                for (var dx = -SPAWN_RADIUS; dx <= SPAWN_RADIUS; dx++) {
                    final var tile = map.getTile(spawn.x() + dx, spawn.y() + dy);
                    assertTrue(tile == null || !tile.hasResource(), type.name());
                }
            }
        }
    }
}
