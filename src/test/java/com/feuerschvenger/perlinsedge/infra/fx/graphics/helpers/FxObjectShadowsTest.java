package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.ShadowList;
import com.feuerschvenger.perlinsedge.ui.view.FxToolkitSupport;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FxObjectShadowsTest {

    private static final double HALF_TILE_HEIGHT = 16.0;
    private static final double ART_SCALE = 2.0;

    /** A small opaque sprite whose bottom two rows are empty, like a rock with padding under it. */
    private static Image sprite() {
        return FxToolkitSupport.onFx(() -> {
            final WritableImage image = new WritableImage(6, 8);
            for (int y = 0; y < 6; y++) {
                for (int x = 0; x < 6; x++) {
                    image.getPixelWriter().setColor(x, y, Color.GREEN);
                }
            }
            return image;
        });
    }

    /** The shadow a tile gets the way the renderer asks for it, whether the tile is in the view or not. */
    private static ShadowList shadowsOf(ResourceType resource, Image image, int contact, BuildingType building,
                                        boolean water, boolean tileArt, double screenX, double screenY) {
        final ShadowList list = new ShadowList();
        final double drop = FxObjectShadows.waterDrop(water, tileArt, HALF_TILE_HEIGHT);
        FxObjectShadows.addForTile(list, resource, image, contact, building, screenX, screenY + drop,
                HALF_TILE_HEIGHT, ART_SCALE);
        return list;
    }

    @Test
    void aTileGetsTheSameShadowWhetherItIsInTheViewOrOff() {
        final Image image = sprite();
        for (boolean water : new boolean[]{false, true}) {
            for (boolean tileArt : new boolean[]{false, true}) {
                final ShadowList inView = shadowsOf(ResourceType.STONE, image, 2, BuildingType.CHEST, water, tileArt,
                        100, 50);
                final ShadowList offView = shadowsOf(ResourceType.STONE, image, 2, BuildingType.CHEST, water, tileArt,
                        100, 50);

                assertEquals(2, inView.size());
                assertTrue(inView.sameShadowsAs(offView), "water " + water + " art " + tileArt);
            }
        }
    }

    @Test
    void aWaterTileWithNoArtLowersWhatStandsOnItAndOtherTilesDoNot() {
        final Image image = sprite();
        final ShadowList land = shadowsOf(ResourceType.TREE, image, 0, null, false, false, 100, 50);
        final ShadowList flatWater = shadowsOf(ResourceType.TREE, image, 0, null, true, false, 100, 50);
        final ShadowList artWater = shadowsOf(ResourceType.TREE, image, 0, null, true, true, 100, 50);

        assertEquals(land.centreY(0) + HALF_TILE_HEIGHT * 0.5, flatWater.centreY(0), 1e-9);
        assertEquals(land.centreY(0), artWater.centreY(0), 1e-9);
        assertEquals(land.centreX(0), flatWater.centreX(0));
    }

    @Test
    void aTallSpriteStandsOnItsBottomEdgeAndALowOneOnItsLowestPaintedRow() {
        final Image image = sprite();
        final ShadowList tall = shadowsOf(ResourceType.TREE, image, 0, null, false, true, 100, 50);
        final ShadowList low = shadowsOf(ResourceType.STONE, image, 2, null, false, true, 100, 50);

        assertEquals(8, tall.groundRow(0), "the whole canvas height");
        assertEquals(6, low.groundRow(0), "the rows above the padding");
        assertEquals(50 + HALF_TILE_HEIGHT, tall.centreY(0), 1e-9);
        assertEquals(50 + HALF_TILE_HEIGHT + 2 * ART_SCALE, low.centreY(0), 1e-9, "the contact depth lowers it");
        assertNotNull(tall.silhouette(0));
    }

    @Test
    void aResourceWithoutArtCastsAnEllipseAndABuildingThatLiesFlatCastsNothing() {
        final ShadowList list = shadowsOf(ResourceType.TREE, null, 0, BuildingType.BRIDGE, false, true, 100, 50);

        assertEquals(1, list.size());
        assertNull(list.silhouette(0));
        assertTrue(list.widthArt(0) > 0);
        assertFalse(shadowsOf(null, null, 0, BuildingType.BRIDGE, false, true, 0, 0).size() > 0);
    }

    @Test
    void aBuildingWithArtCastsTheSilhouetteOfItsSpriteAndOneWithoutCastsTheEllipse() {
        final Image art = sprite();
        final ShadowList withArt = new ShadowList();
        final ShadowList withoutArt = new ShadowList();

        FxObjectShadows.addForTile(withArt, null, null, 0, BuildingType.WORKBENCH, art, 0, 0, HALF_TILE_HEIGHT, ART_SCALE);
        FxObjectShadows.addForTile(withoutArt, null, null, 0, BuildingType.WORKBENCH, (Image) null, 0, 0, HALF_TILE_HEIGHT, ART_SCALE);

        assertEquals(1, withArt.size());
        assertNotNull(withArt.silhouette(0));
        assertEquals(1, withoutArt.size());
        assertNull(withoutArt.silhouette(0));
    }

    @Test
    void aFlatBuildingCastsNoShadowEvenWithArt() {
        final ShadowList list = new ShadowList();

        FxObjectShadows.addForTile(list, null, null, 0, BuildingType.BRIDGE, sprite(), 0, 0, HALF_TILE_HEIGHT, ART_SCALE);

        assertEquals(0, list.size());
    }
}
