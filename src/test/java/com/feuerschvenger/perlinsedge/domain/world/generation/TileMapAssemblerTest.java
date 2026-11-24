package com.feuerschvenger.perlinsedge.domain.world.generation;

import com.feuerschvenger.perlinsedge.domain.world.generation.support.TestContexts;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.SpawnPoint;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TileMapAssemblerTest {

    private static final int WIDTH = 5;
    private static final int HEIGHT = 3;
    private static final int TREE_X = 1;
    private static final int TREE_Y = 2;
    private static final int STONE_X = 4;
    private static final int STONE_Y = 0;
    private static final float SAMPLE_HEIGHT = 0.25F;

    private final GenerationContext context = TestContexts.of(WIDTH, HEIGHT);
    private final TileMapAssembler assembler = new TileMapAssembler();

    private void classifyEverythingAs(TileType type) {
        Arrays.fill(context.ground(), type);
    }

    @Test
    void createsOneTileForEveryCellWithItsTerrain() {
        classifyEverythingAs(TileType.GRASS);
        context.ground()[context.index(TREE_X, TREE_Y)] = TileType.SAND;

        final var map = assembler.materialize(context);

        assertEquals(WIDTH, map.getWidth());
        assertEquals(HEIGHT, map.getHeight());
        for (var y = 0; y < HEIGHT; y++) {
            for (var x = 0; x < WIDTH; x++) {
                assertEquals(x, map.getTile(x, y).getX());
                assertEquals(y, map.getTile(x, y).getY());
            }
        }
        assertEquals(TileType.SAND, map.getTile(TREE_X, TREE_Y).getType());
        assertEquals(TileType.GRASS, map.getTile(0, 0).getType());
    }

    @Test
    void putsAResourceOnlyWhereTheContextHasOne() {
        classifyEverythingAs(TileType.GRASS);
        context.resources()[context.index(TREE_X, TREE_Y)] = ResourceType.TREE;
        context.resources()[context.index(STONE_X, STONE_Y)] = ResourceType.STONE;

        final var map = assembler.materialize(context);

        assertEquals(ResourceType.TREE, map.getTile(TREE_X, TREE_Y).getWorldResource().getType());
        assertEquals(ResourceType.STONE, map.getTile(STONE_X, STONE_Y).getWorldResource().getType());
        assertNull(map.getTile(0, 0).getWorldResource());
    }

    @Test
    void theVisualSeedOfAResourceIsAHashOfItsTileNotAnOrderOfDraws() {
        classifyEverythingAs(TileType.GRASS);
        context.resources()[context.index(TREE_X, TREE_Y)] = ResourceType.TREE;
        context.resources()[context.index(STONE_X, STONE_Y)] = ResourceType.TREE;

        final var first = assembler.materialize(context);
        final var second = assembler.materialize(context);

        final var seed = first.getTile(TREE_X, TREE_Y).getWorldResource().getVisualSeed();
        assertEquals(seed, second.getTile(TREE_X, TREE_Y).getWorldResource().getVisualSeed());
        assertNotEquals(seed, first.getTile(STONE_X, STONE_Y).getWorldResource().getVisualSeed());
    }

    @Test
    void copiesTheHeightFieldWhenThereIsOneAndLeavesZeroWhenThereIsNot() {
        classifyEverythingAs(TileType.GRASS);
        final var without = assembler.materialize(context);
        context.defineField(NoiseField.HEIGHT)[context.index(TREE_X, TREE_Y)] = SAMPLE_HEIGHT;

        final var with = assembler.materialize(context);

        assertEquals(0F, without.getTile(TREE_X, TREE_Y).getHeight());
        assertEquals(SAMPLE_HEIGHT, with.getTile(TREE_X, TREE_Y).getHeight());
        assertEquals(0F, with.getTile(0, 0).getHeight());
    }

    @Test
    void carriesTheSpawnIndexAsASpawnPoint() {
        classifyEverythingAs(TileType.GRASS);
        context.setSpawnIndex(context.index(TREE_X, TREE_Y));

        final var map = assembler.materialize(context);

        assertEquals(new SpawnPoint(TREE_X, TREE_Y), map.getSpawn().orElseThrow());
    }

    @Test
    void aMapWithoutASpawnIndexHasNoSpawnPoint() {
        classifyEverythingAs(TileType.GRASS);

        assertTrue(assembler.materialize(context).getSpawn().isEmpty());
    }

    @Test
    void refusesATileNoPassClassified() {
        classifyEverythingAs(TileType.GRASS);
        context.ground()[context.index(STONE_X, STONE_Y)] = null;

        final var error = assertThrows(IllegalStateException.class, () -> assembler.materialize(context));

        assertTrue(error.getMessage().contains(STONE_X + "," + STONE_Y));
    }
}
