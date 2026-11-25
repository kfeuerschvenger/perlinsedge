package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.TestContexts;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/** The maps are drawn with letters: {@code ~} is deep water, {@code g} grass and {@code m} a mountain. */
class LandmassHintPassTest {

    private static GenerationContext map(String... rows) {
        final var context = TestContexts.of(rows[0].length(), rows.length);
        for (var y = 0; y < rows.length; y++) {
            for (var x = 0; x < rows[y].length(); x++) {
                context.setGround(context.index(x, y), switch (rows[y].charAt(x)) {
                    case 'g' -> TileType.GRASS;
                    case 'm' -> TileType.MOUNTAIN;
                    default -> TileType.DEEP_WATER;
                });
            }
        }
        return context;
    }

    private static void apply(GenerationContext context) {
        new LandmassHintPass().apply(context);
    }

    @Test
    void theHintIsTheMiddleOfTheLargestPieceOfLand() {
        final var context = map(
                "gg~~~~~~~",
                "gg~~ggggg",
                "~~~~ggggg",
                "~~~~ggggg",
                "~~~~ggggg",
                "~~~~ggggg");

        apply(context);

        assertEquals(context.index(6, 3), context.spawnHint());
    }

    @Test
    void whenTheCentroidLiesInABayTheHintIsTheNearestTileOfThePieceItself() {
        final var context = map(
                "ggggg",
                "g~~~g",
                "g~~~g",
                "g~~~g",
                "ggggg");

        apply(context);

        assertNotEquals(TileType.DEEP_WATER, context.groundAt(context.spawnHint()));
        assertEquals(TileType.GRASS, context.groundAt(context.spawnHint()));
    }

    @Test
    void mountainsAreNotWalkableSoTheyDoNotCountAsLand() {
        final var context = map(
                "mmmmmm~gg",
                "mmmmmm~gg",
                "mmmmmm~gg");

        apply(context);

        assertEquals(TileType.GRASS, context.groundAt(context.spawnHint()));
        assertEquals(context.index(7, 1), context.spawnHint());
    }

    @Test
    void resourcesDoNotMatterBecauseTheLandIsMeasuredNotTheFreeGround() {
        final var context = map(
                "ggggg~gg",
                "ggggg~gg");
        for (var x = 0; x < 5; x++) {
            context.setResource(context.index(x, 0), ResourceType.TREE);
            context.setResource(context.index(x, 1), ResourceType.TREE);
        }

        apply(context);

        assertEquals(context.index(2, 0), context.spawnHint());
    }

    @Test
    void aMapWithoutWalkableGroundLeavesTheHintAlone() {
        final var context = map(
                "~~~",
                "~m~");

        apply(context);

        assertEquals(GenerationContext.NONE, context.spawnHint());
    }

    @Test
    void thePassIsNamed() {
        assertEquals("landmass-hint", new LandmassHintPass().name());
    }
}
