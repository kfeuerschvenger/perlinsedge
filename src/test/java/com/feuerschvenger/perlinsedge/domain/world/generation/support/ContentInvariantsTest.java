package com.feuerschvenger.perlinsedge.domain.world.generation.support;

import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.Resource;
import com.feuerschvenger.perlinsedge.domain.world.model.SpawnPoint;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The maps are drawn with letters: {@code g} is grass, {@code t} grass with a tree, {@code m} a mountain and {@code ~}
 * deep water, so each check can be shown to pass on a good picture and to fail on a bad one.
 */
class ContentInvariantsTest {

    private static final long VISUAL_SEED = 1L;
    private static final int GLADE = 11;
    private static final int HALF = 2;

    private static TileMap map(SpawnPoint spawn, String... rows) {
        final var map = new TileMap(rows[0].length(), rows.length, spawn);
        for (var y = 0; y < rows.length; y++) {
            for (var x = 0; x < rows[y].length(); x++) {
                final var glyph = rows[y].charAt(x);
                final var tile = new Tile(x, y, typeOf(glyph));
                if (glyph == 't') {
                    tile.setWorldResource(new Resource(ResourceType.TREE, VISUAL_SEED));
                }
                map.setTile(x, y, tile);
            }
        }
        return map;
    }

    private static TileType typeOf(char glyph) {
        return switch (glyph) {
            case 'm' -> TileType.MOUNTAIN;
            case '~' -> TileType.DEEP_WATER;
            default -> TileType.GRASS;
        };
    }

    /** Builds a square of free grass of the given side with a spawn in the middle. */
    private static TileMap glade(int side) {
        final var rows = new String[side];
        Arrays.fill(rows, "g".repeat(side));
        return map(new SpawnPoint(side / HALF, side / HALF), rows);
    }

    @Test
    void aFreeDiscAroundTheSpawnPassesAndATreeInsideItFails() {
        final var open = glade(GLADE);
        final var blocked = map(new SpawnPoint(GLADE / HALF, GLADE / HALF),
                "ggggggggggg", "ggggggggggg", "ggggggggggg", "ggggggggggg", "ggggggggggg", "gggggtggggg",
                "ggggggggggg", "ggggggggggg", "ggggggggggg", "ggggggggggg", "ggggggggggg");

        assertDoesNotThrow(() -> ContentInvariants.denseForestSpawnHasAFreeDisc(open));
        assertThrows(AssertionError.class, () -> ContentInvariants.denseForestSpawnHasAFreeDisc(blocked));
    }

    @Test
    void aDiscThatLeavesTheMapOrHoldsWaterFails() {
        final var cornered = map(new SpawnPoint(0, 0), "ggg", "ggg", "ggg");
        final var wet = map(new SpawnPoint(GLADE / HALF, GLADE / HALF),
                "ggggggggggg", "ggggggggggg", "ggggggggggg", "ggggggggggg", "ggggggggggg", "ggggg~ggggg",
                "ggggggggggg", "ggggggggggg", "ggggggggggg", "ggggggggggg", "ggggggggggg");

        assertThrows(AssertionError.class, () -> ContentInvariants.denseForestSpawnHasAFreeDisc(cornered));
        assertThrows(AssertionError.class, () -> ContentInvariants.denseForestSpawnHasAFreeDisc(wet));
    }

    @Test
    void theFreeRegionOfTheSpawnMustHoldMostOfTheFreeLand() {
        final var joined = map(new SpawnPoint(0, 0),
                "gggg",
                "gggg");
        final var cut = map(new SpawnPoint(0, 0),
                "gtgg",
                "gtgg",
                "gtgg");

        assertDoesNotThrow(() -> ContentInvariants.denseForestSpawnRegionHoldsMostFreeLand(joined));
        assertThrows(AssertionError.class, () -> ContentInvariants.denseForestSpawnRegionHoldsMostFreeLand(cut));
    }

    @Test
    void mountainsMustCoverAFifthToAThirdOfTheMap() {
        final var good = map(new SpawnPoint(0, 0),
                "mmmmm",
                "ggggg",
                "ggggg",
                "ggggg");
        final var none = map(new SpawnPoint(0, 0), "gggg", "gggg");
        final var all = map(new SpawnPoint(0, 0), "mmmm", "mmmm");

        assertDoesNotThrow(() -> ContentInvariants.mountainsCoverPartOfTheMap(good));
        assertThrows(AssertionError.class, () -> ContentInvariants.mountainsCoverPartOfTheMap(none));
        assertThrows(AssertionError.class, () -> ContentInvariants.mountainsCoverPartOfTheMap(all));
    }

    @Test
    void theWalkableRegionOfTheSpawnMustHoldMostOfTheWalkableGround() {
        final var joined = map(new SpawnPoint(0, 0),
                "gggg",
                "gmmg",
                "gggg");
        final var split = map(new SpawnPoint(0, 0),
                "ggmgg",
                "ggmgg");

        assertDoesNotThrow(() -> ContentInvariants.mountainousSpawnRegionHoldsTheGround(joined));
        assertThrows(AssertionError.class, () -> ContentInvariants.mountainousSpawnRegionHoldsTheGround(split));
    }

    @Test
    void theSpawnOfTheMountainsMustBeAFewTilesFromTheNearestMountain() {
        final var valley = map(new SpawnPoint(0, 0), "ggggm");
        final var foot = map(new SpawnPoint(0, 0), "gggm");

        assertDoesNotThrow(() -> ContentInvariants.mountainousSpawnIsInAValley(valley));
        assertThrows(AssertionError.class, () -> ContentInvariants.mountainousSpawnIsInAValley(foot));
    }

    @Test
    void aMapWithNoMountainHasASpawnFarFromAnyMountain() {
        assertDoesNotThrow(() -> ContentInvariants.mountainousSpawnIsInAValley(glade(GLADE)));
    }
}
