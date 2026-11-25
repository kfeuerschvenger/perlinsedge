package com.feuerschvenger.perlinsedge.infra.fx.utils;

import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpriteManagerTilesTest {

    private static final int SAMPLES = 40;
    private static final long SEED = 5L;

    private final SpriteManager sprites = SpriteManager.getInstance();

    private static Tile grassWithPatch(int patch) {
        final var tile = new Tile(0, 0, TileType.GRASS);
        tile.setPatch(patch);
        return tile;
    }

    @Test
    void tilesOfTheSamePatchShareTheirGrassSprite() {
        assertSame(sprites.getTileSprite(MapType.DENSE_FOREST, grassWithPatch(120)),
                sprites.getTileSprite(MapType.DENSE_FOREST, grassWithPatch(120)));
        assertSame(sprites.getTileSprite(MapType.DENSE_FOREST, grassWithPatch(100)),
                sprites.getTileSprite(MapType.DENSE_FOREST, grassWithPatch(150)));
    }

    @Test
    void theLowMiddleAndHighPatchesPickThreeDifferentGrassSprites() {
        final var low = sprites.getTileSprite(MapType.DENSE_FOREST, grassWithPatch(0));
        final var middle = sprites.getTileSprite(MapType.DENSE_FOREST, grassWithPatch(128));
        final var high = sprites.getTileSprite(MapType.DENSE_FOREST, grassWithPatch(Tile.MAX_PATCH));

        assertNotNull(low);
        assertNotSame(low, middle);
        assertNotSame(middle, high);
        assertNotSame(low, high);
    }

    @Test
    void anArtWithOneVariantIgnoresThePatch() {
        final var sand = new Tile(0, 0, TileType.SAND);
        final var first = sprites.getTileSprite(MapType.DENSE_FOREST, sand);
        sand.setPatch(Tile.MAX_PATCH);

        assertSame(first, sprites.getTileSprite(MapType.DENSE_FOREST, sand));
    }

    private Set<String> treeKinds(MapType type) {
        final Set<String> kinds = new HashSet<>();
        for (int x = 0; x < SAMPLES; x++) {
            for (int y = 0; y < SAMPLES; y++) {
                final var sprite = sprites.getResourceSprite(type, ResourceType.TREE, x, y, SEED);
                assertNotNull(sprite, type + " has tree art");
                kinds.add(sprite.getKind());
            }
        }
        return kinds;
    }

    @Test
    void theSwampDrawsItsTreesAsDeadTreesAndWillows() {
        assertEquals(Set.of("deadtree", "willow"), treeKinds(MapType.SWAMP));
    }

    @Test
    void theMediterraneanDrawsOlivesCypressesAndBushes() {
        final var kinds = treeKinds(MapType.MEDITERRANEAN);

        assertTrue(kinds.containsAll(Set.of("olive", "cypress", "bush")), kinds.toString());
        assertEquals(3, kinds.size());
    }

    @Test
    void theOtherMapsKeepTheirTreeArt() {
        assertTrue(treeKinds(MapType.RIVERS).contains("tree"));
        assertTrue(treeKinds(MapType.DENSE_FOREST).contains("tree"));
    }

    @Test
    void theSwampGroundShowsReedDecals() {
        for (final String reed : GroundDecals.SWAMP_DECALS) {
            assertNotNull(sprites.getDecalSprite(reed), reed);
        }
    }
}
