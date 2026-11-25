package com.feuerschvenger.perlinsedge.domain.world.generation.support;

import com.feuerschvenger.perlinsedge.domain.world.model.SpawnPoint;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The maps are drawn with letters: {@code ~} is deep water, {@code r} a river, {@code g} grass, {@code i} ice and
 * {@code m} a mountain, so the expected counts can be read off the picture.
 */
class WaterStatsTest {

    private static final double TOLERANCE = 1e-9;
    private static final int MIN_FIVE = 5;
    private static final int RIVER_COLUMN_SPAN = 1;
    private static final int RIVER_ROW_SPAN = 4;

    private static TileMap map(SpawnPoint spawn, String... rows) {
        final var map = new TileMap(rows[0].length(), rows.length, spawn);
        for (var y = 0; y < rows.length; y++) {
            for (var x = 0; x < rows[y].length(); x++) {
                map.setTile(x, y, new Tile(x, y, typeOf(rows[y].charAt(x))));
            }
        }
        return map;
    }

    private static TileType typeOf(char glyph) {
        return switch (glyph) {
            case '~' -> TileType.DEEP_WATER;
            case 'r' -> TileType.RIVER;
            case 'i' -> TileType.ICE;
            case 'm' -> TileType.MOUNTAIN;
            default -> TileType.GRASS;
        };
    }

    @Test
    void waterIsEverythingThatIsNotSolidGroundAndIceCountsAsLand() {
        final var map = map(new SpawnPoint(0, 0),
                "gg~~",
                "gir~",
                "gggg");

        assertEquals(4.0 / 12.0, WaterStats.waterShare(map), TOLERANCE);
        assertTrue(WaterStats.isWater(TileType.RIVER));
        assertTrue(WaterStats.isWater(TileType.SHALLOW_WATER));
    }

    @Test
    void waterBodiesJoinRiversAndSeaAndSkipTheSmallOnes() {
        final var map = map(new SpawnPoint(0, 0),
                "gg~~g~",
                "grr~gg",
                "gggggg",
                "g~gggg");

        final var bodies = WaterStats.waterBodies(map);

        assertEquals(3, bodies.count());
        assertEquals(1, bodies.countAtLeast(MIN_FIVE));
        assertEquals(1, bodies.sizeOf(bodies.labelAt(1, 3)));
        assertEquals(Components.NONE, bodies.labelAt(0, 0));
    }

    @Test
    void riverComponentsReportTheirSizeAndTheirBoundingBox() {
        final var map = map(new SpawnPoint(0, 0),
                "grggg",
                "grggg",
                "grgrr",
                "grggg",
                "gggg~");

        final var rivers = WaterStats.rivers(map);

        assertEquals(2, rivers.count());
        final var tall = rivers.labelAt(1, 0);
        assertEquals(RIVER_ROW_SPAN, rivers.sizeOf(tall));
        assertEquals(RIVER_COLUMN_SPAN, rivers.spanX(tall));
        assertEquals(RIVER_ROW_SPAN, rivers.spanY(tall));
        assertEquals(RIVER_ROW_SPAN, rivers.largest());
        assertEquals(2, rivers.sizes()[1]);
    }

    @Test
    void landMassesSplitWhereWaterCutsTheLand() {
        final var map = map(new SpawnPoint(0, 0),
                "gg~gg",
                "gg~gg",
                "gg~gg");

        final var land = WaterStats.landMasses(map);

        assertEquals(2, land.count());
        assertEquals(6, land.largest());
    }

    @Test
    void componentsCanBeCountedByAnExactSizeRange() {
        final var map = map(new SpawnPoint(0, 0),
                "~g~~g~",
                "~g~~g~",
                "~g~~gg",
                "gggggg");

        final var land = WaterStats.landMasses(map);

        assertEquals(1, land.count());
        assertEquals(1, land.countWithin(1, Integer.MAX_VALUE));
        assertEquals(0, land.countWithin(1, MIN_FIVE));
        final var water = WaterStats.waterBodies(map);
        assertEquals(3, water.count());
        assertEquals(2, water.countWithin(1, MIN_FIVE));
        assertEquals(1, water.countWithin(MIN_FIVE + 1, Integer.MAX_VALUE));
    }

    @Test
    void inlandComponentsAreTheOnesThatTouchNoEdgeWithinASizeRange() {
        final var map = map(new SpawnPoint(0, 0),
                "~gggggg",
                "ggg~~gg",
                "ggg~~gg",
                "ggggggg",
                "gg~gggg",
                "ggggg~~");

        final var bodies = WaterStats.waterBodies(map);

        assertTrue(bodies.touchesEdge(bodies.labelAt(0, 0)));
        assertTrue(bodies.touchesEdge(bodies.labelAt(6, 5)));
        assertEquals(2, bodies.countInland(1, MIN_FIVE));
        assertEquals(1, bodies.countInland(2, MIN_FIVE));
        assertEquals(0, bodies.countInland(MIN_FIVE, MIN_FIVE + 1));
    }

    @Test
    void theSpawnRegionIsTheWalkableLandConnectedToTheSpawn() {
        final var map = map(new SpawnPoint(0, 0),
                "gg~gg",
                "gg~mg",
                "gg~gg");

        // The left part holds 6 of the 12 land tiles, and the mountain is land that cannot be walked
        assertEquals(6.0 / 12.0, WaterStats.spawnRegionShareOfLand(map), TOLERANCE);
    }

    @Test
    void theWettestEdgeIsTheSideWithTheHighestShareOfWater() {
        final var map = map(new SpawnPoint(0, 0),
                "gggg",
                "gggg",
                "gggg",
                "~~~g");

        assertEquals(0.75, WaterStats.wettestEdgeShare(map), TOLERANCE);
    }

    @Test
    void aSideOfTheMapThatIsAllWaterIsFound() {
        final var map = map(new SpawnPoint(0, 0),
                "g~",
                "g~",
                "g~");

        assertEquals(1.0, WaterStats.wettestEdgeShare(map), TOLERANCE);
    }
}
