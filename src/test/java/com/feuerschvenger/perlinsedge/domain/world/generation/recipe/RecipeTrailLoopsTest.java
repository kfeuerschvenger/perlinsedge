package com.feuerschvenger.perlinsedge.domain.world.generation.recipe;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.TrailNetworkPass;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.TestContexts;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The trails of the real recipes close a loop on a small map too, where the density of extra trails rounds to none, so
 * a map is not left a bare tree of trails that one trail cut by the player would split in two.
 */
class RecipeTrailLoopsTest {

    private static final long SEED = 5L;
    /** The four sites stand a quarter of the side in from the corners of the map. */
    private static final int QUARTERS = 4;
    private static final int THREE = 3;

    private static TrailNetworkPass trailsOf(MapType type) {
        return new MapRecipes().recipeFor(type).passes().stream()
                .filter(TrailNetworkPass.class::isInstance)
                .map(TrailNetworkPass.class::cast)
                .findFirst()
                .orElseThrow();
    }

    /** Counts the tiles that no step along the four directions over unreserved tiles joins to the border of the map. */
    private static int enclosedTiles(GenerationContext context) {
        final var outside = new boolean[context.size()];
        final var queue = new int[context.size()];
        var tail = 0;
        for (var index = 0; index < context.size(); index++) {
            final var onBorder = context.xOf(index) == 0 || context.yOf(index) == 0
                    || context.xOf(index) == context.width() - 1 || context.yOf(index) == context.height() - 1;
            if (onBorder && !context.isReserved(index)) {
                outside[index] = true;
                queue[tail] = index;
                tail++;
            }
        }
        for (var head = 0; head < tail; head++) {
            final var index = queue[head];
            final var x = context.xOf(index);
            final var y = context.yOf(index);
            final var neighbours = new int[]{x > 0 ? index - 1 : index,
                x < context.width() - 1 ? index + 1 : index,
                y > 0 ? index - context.width() : index,
                y < context.height() - 1 ? index + context.width() : index};
            for (var neighbour : neighbours) {
                if (!outside[neighbour] && !context.isReserved(neighbour)) {
                    outside[neighbour] = true;
                    queue[tail] = neighbour;
                    tail++;
                }
            }
        }
        var enclosed = 0;
        for (var index = 0; index < context.size(); index++) {
            enclosed += !outside[index] && !context.isReserved(index) ? 1 : 0;
        }
        return enclosed;
    }

    @ParameterizedTest
    @CsvSource({"DENSE_FOREST,64", "DENSE_FOREST,150", "MOUNTAINOUS,64", "MOUNTAINOUS,150"})
    void theTrailsOfTheRealRecipeEncloseGroundAtTheSizeWhereTheDensityRoundsToNoExtraTrail(MapType type, int side) {
        final var context = TestContexts.of(type, SEED, side, side, TestContexts.FLAT);
        TestContexts.fillGround(context, TileType.GRASS);
        final var near = side / QUARTERS;
        final var far = near * THREE;
        for (var site : new int[]{context.index(near, near), context.index(far, near), context.index(near, far),
            context.index(far, far)}) {
            context.sites().add(site);
            context.reserve(site);
        }

        trailsOf(type).apply(context);

        assertTrue(enclosedTiles(context) > 0, "the trails close a loop that fences in some ground");
    }
}
