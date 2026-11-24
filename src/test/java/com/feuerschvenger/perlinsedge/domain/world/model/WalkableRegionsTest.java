package com.feuerschvenger.perlinsedge.domain.world.model;

import com.feuerschvenger.perlinsedge.domain.entities.buildings.Bridge;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Fence;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.FenceGate;
import com.feuerschvenger.perlinsedge.domain.pathfinding.Pathfinder;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The labelling of walkable tiles by region, on small hand-built maps where the regions can be counted by eye, and
 * against the search it stands in for on random maps.
 */
class WalkableRegionsTest {

    private static final int RANDOM_SIZE = 30;
    private static final int RANDOM_MAPS = 12;
    private static final int BLOCKED_PERCENT = 38;
    private static final int PAIRS_PER_MAP = 60;
    private static final long SEED = 20_240_611L;
    private static final int PERCENT = 100;
    private static final int LARGE_SIZE = 300;

    /**
     * Builds a map from rows of characters, one per tile row: a dot is grass, a tilde deep water, a hash a fence and a
     * T a tree.
     */
    private static TileMap map(String... rows) {
        final var map = new TileMap(rows[0].length(), rows.length);
        for (var y = 0; y < rows.length; y++) {
            for (var x = 0; x < rows[y].length(); x++) {
                final var glyph = rows[y].charAt(x);
                final var tile = new Tile(x, y, glyph == '~' ? TileType.DEEP_WATER : TileType.GRASS);
                map.setTile(x, y, tile);
                if (glyph == '#') {
                    tile.setBuilding(new Fence(x, y));
                } else if (glyph == 'T') {
                    tile.setWorldResource(new Resource(ResourceType.TREE, 1));
                }
            }
        }
        return map;
    }

    @Test
    void landSeparatedByWaterIsOneRegionPerIsland() {
        final var map = map(
                "..~..",
                "..~..",
                "..~..");

        final var regions = map.getWalkableRegions();

        assertEquals(2, regions.getRegionCount());
        assertNotEquals(regions.regionAt(0, 0), regions.regionAt(4, 0));
        assertEquals(regions.regionAt(0, 0), regions.regionAt(1, 2));
        assertEquals(regions.regionAt(3, 1), regions.regionAt(4, 2));
    }

    @Test
    void tilesThatCannotBeWalkedOnAndPositionsOutsideTheMapHaveNoRegion() {
        final var map = map(
                ".~#T");

        final var regions = map.getWalkableRegions();

        assertEquals(WalkableRegions.NO_REGION, regions.regionAt(1, 0));
        assertEquals(WalkableRegions.NO_REGION, regions.regionAt(2, 0));
        assertEquals(WalkableRegions.NO_REGION, regions.regionAt(3, 0));
        assertEquals(WalkableRegions.NO_REGION, regions.regionAt(-1, 0));
        assertEquals(WalkableRegions.NO_REGION, regions.regionAt(0, 9));
        assertEquals(1, regions.getRegionCount());
    }

    @Test
    void aDiagonalBetweenTwoBlockedTilesJoinsNothing() {
        final var map = map(
                ".#",
                "#.");

        final var regions = map.getWalkableRegions();

        assertEquals(2, regions.getRegionCount());
        assertFalse(regions.mayReach(0, 0, 1, 1));
        assertFalse(new Pathfinder(map).findPath(0, 0, 1, 1).reachable());
    }

    @Test
    void aDiagonalWithOneBlockedSideJoinsNothingEither() {
        final var map = map(
                "..#",
                ".#.");

        final var regions = map.getWalkableRegions();

        assertEquals(2, regions.getRegionCount());
        assertFalse(new Pathfinder(map).findPath(0, 0, 2, 1).reachable());
        assertFalse(regions.mayReach(0, 0, 2, 1));
    }

    @Test
    void aClosedGateSplitsTheMapAndAnOpenGateJoinsItAgain() {
        final var map = map(
                "..#..",
                "..#..",
                "..#..");
        final var gate = new FenceGate(2, 1);
        map.getTile(2, 1).setBuilding(gate);
        final var regions = map.getWalkableRegions();
        assertEquals(2, regions.getRegionCount());
        assertFalse(regions.mayReach(0, 0, 4, 2));

        gate.onInteract(map.getTile(2, 1));

        assertEquals(1, regions.getRegionCount());
        assertTrue(regions.mayReach(0, 0, 4, 2));

        gate.onInteract(map.getTile(2, 1));

        assertEquals(2, regions.getRegionCount());
        assertFalse(regions.mayReach(0, 0, 4, 2));
    }

    @Test
    void togglingAGateDirectlyMovesTheRevisionAndRelabelsTheMap() {
        final var map = map(
                "..#..",
                "..#..",
                "..#..");
        final var gate = new FenceGate(2, 1);
        map.getTile(2, 1).setBuilding(gate);
        final var regions = map.getWalkableRegions();
        assertEquals(2, regions.getRegionCount());
        final var revision = map.getRevision();

        gate.toggleOpen(map.getTile(2, 1));

        assertNotEquals(revision, map.getRevision());
        assertEquals(1, regions.getRegionCount());
    }

    @Test
    void aBridgeOverWaterJoinsTheShoresAndDemolishingItSplitsThemAgain() {
        final var map = map(
                "..~..",
                "..~..");
        final var regions = map.getWalkableRegions();
        assertEquals(2, regions.getRegionCount());

        map.getTile(2, 1).setFloor(new Bridge(2, 1));

        assertEquals(1, regions.getRegionCount());
        assertTrue(regions.mayReach(0, 0, 4, 0));

        map.getTile(2, 1).setFloor(null);

        assertEquals(2, regions.getRegionCount());
    }

    @Test
    void aHarvestedTreeOpensAPathAndAPlacedBuildingClosesIt() {
        final var map = map(
                "..T..");
        final var regions = map.getWalkableRegions();
        assertEquals(2, regions.getRegionCount());

        map.getTile(2, 0).setWorldResource(null);

        assertEquals(1, regions.getRegionCount());
        assertTrue(regions.mayReach(0, 0, 4, 0));

        map.getTile(2, 0).setBuilding(new Fence(2, 0));

        assertEquals(2, regions.getRegionCount());
    }

    @Test
    void aChangeOfTerrainTypeIsSeenByTheNextQuery() {
        final var map = map(
                "..~..");
        final var regions = map.getWalkableRegions();
        assertEquals(2, regions.getRegionCount());

        map.getTile(2, 0).setType(TileType.GRASS);

        assertEquals(1, regions.getRegionCount());
    }

    @Test
    void replacingATileIsSeenByTheNextQuery() {
        final var map = map(
                "..~..");
        final var regions = map.getWalkableRegions();
        assertEquals(2, regions.getRegionCount());

        map.setTile(2, 0, new Tile(2, 0, TileType.GRASS));

        assertEquals(1, regions.getRegionCount());
    }

    @Test
    void theIndexIsOnePerMap() {
        final var map = map("..");

        assertSame(map.getWalkableRegions(), map.getWalkableRegions());
    }

    @Test
    void theMapIsLabelledOnlyWhenAQueryFindsItChanged() {
        final var map = map(
                "..~..");
        final var regions = map.getWalkableRegions();
        assertEquals(0, regions.getRelabelCount());

        regions.regionAt(0, 0);
        regions.getRegionCount();
        regions.mayReach(0, 0, 4, 0);
        assertEquals(1, regions.getRelabelCount());

        map.getTile(2, 0).setType(TileType.GRASS);
        map.getTile(2, 0).setType(TileType.DEEP_WATER);
        map.getTile(2, 0).setType(TileType.GRASS);
        assertEquals(1, regions.getRelabelCount());

        regions.regionAt(0, 0);
        regions.regionAt(4, 0);
        assertEquals(2, regions.getRelabelCount());
    }

    @Test
    void aWalkCannotBeRuledOutFromATileThatIsNotWalkable() {
        final var map = map(
                ".#.",
                ".#.");
        final var regions = map.getWalkableRegions();

        assertTrue(regions.mayReach(1, 0, 0, 0));
        assertTrue(regions.mayReachNextTo(1, 0, 0, 0));
    }

    @Test
    void aTargetNextToTheStartRegionIsReachableEvenWhenItIsNotWalkable() {
        final var map = map(
                "..T..",
                "..#..");
        final var regions = map.getWalkableRegions();

        assertTrue(regions.mayReachNextTo(0, 0, 2, 0));
        assertTrue(regions.mayReachNextTo(4, 1, 2, 0));
        assertFalse(regions.mayReach(0, 0, 2, 0));
    }

    @Test
    void aTargetWithNoWalkableTileOfTheStartRegionAroundItIsRefused() {
        final var map = map(
                ".....",
                ".###.",
                ".#T#.",
                ".###.");
        final var regions = map.getWalkableRegions();

        assertFalse(regions.mayReachNextTo(0, 0, 2, 2));
        assertTrue(regions.mayReachNextTo(0, 0, 2, 1));
    }

    @Test
    void aTargetOutsideTheMapIsRefused() {
        final var map = map("...");

        assertFalse(map.getWalkableRegions().mayReach(0, 0, 9, 9));
        assertFalse(map.getWalkableRegions().mayReachNextTo(0, 0, 9, 9));
    }

    @Test
    void aMapWithoutTilesHasNoRegions() {
        assertEquals(0, new TileMap(4, 4).getWalkableRegions().getRegionCount());
    }

    @Test
    void theLabelsAgreeWithTheSearchOnRandomMaps() {
        final var random = new Random(SEED);
        for (var m = 0; m < RANDOM_MAPS; m++) {
            final var map = randomMap(random);
            final var regions = map.getWalkableRegions();
            final var pathfinder = new Pathfinder(map);
            for (var i = 0; i < PAIRS_PER_MAP; i++) {
                final var sx = random.nextInt(RANDOM_SIZE);
                final var sy = random.nextInt(RANDOM_SIZE);
                final var tx = random.nextInt(RANDOM_SIZE);
                final var ty = random.nextInt(RANDOM_SIZE);
                if (map.getTile(sx, sy).isWalkable() && map.getTile(tx, ty).isWalkable()) {
                    final var found = (sx == tx && sy == ty) || pathfinder.findPath(sx, sy, tx, ty).reachable();
                    assertEquals(found, regions.mayReach(sx, sy, tx, ty),
                            "(" + sx + ", " + sy + ") to (" + tx + ", " + ty + ")");
                }
                if (map.getTile(sx, sy).isWalkable()) {
                    final var foundNextTo = pathfinder.findPathNextTo(sx, sy, tx, ty, Integer.MAX_VALUE).reachable();
                    assertEquals(foundNextTo, regions.mayReachNextTo(sx, sy, tx, ty),
                            "next to (" + tx + ", " + ty + ") from (" + sx + ", " + sy + ")");
                }
            }
        }
    }

    @Test
    void relabellingAMapOfTheDefaultSizeIsOneFastPass() {
        final var map = new TileMap(LARGE_SIZE, LARGE_SIZE);
        for (var x = 0; x < LARGE_SIZE; x++) {
            for (var y = 0; y < LARGE_SIZE; y++) {
                map.setTile(x, y, new Tile(x, y, x == LARGE_SIZE / 2 ? TileType.DEEP_WATER : TileType.GRASS));
            }
        }

        final var regions = map.getWalkableRegions();

        assertEquals(2, regions.getRegionCount());
        assertEquals(1, regions.getRelabelCount());
    }

    private static TileMap randomMap(Random random) {
        final var map = new TileMap(RANDOM_SIZE, RANDOM_SIZE);
        for (var x = 0; x < RANDOM_SIZE; x++) {
            for (var y = 0; y < RANDOM_SIZE; y++) {
                final var tile = new Tile(x, y, TileType.GRASS);
                map.setTile(x, y, tile);
                if (random.nextInt(PERCENT) < BLOCKED_PERCENT) {
                    tile.setBuilding(new Fence(x, y));
                }
            }
        }
        return map;
    }
}
