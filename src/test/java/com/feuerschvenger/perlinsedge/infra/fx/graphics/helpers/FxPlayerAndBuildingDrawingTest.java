package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Building;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingFactory;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import com.feuerschvenger.perlinsedge.domain.utils.Rgba;
import com.feuerschvenger.perlinsedge.ui.view.CanvasProbe;
import com.feuerschvenger.perlinsedge.ui.view.FxToolkitSupport;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FxPlayerAndBuildingDrawingTest {

    private static final int SIZE = 160;
    private static final int TOLERANCE = 8;

    private static Image solid(int width, int height, Color color) {
        return FxToolkitSupport.onFx(() -> {
            final WritableImage image = new WritableImage(width, height);
            final PixelWriter writer = image.getPixelWriter();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    writer.setColor(x, y, color);
                }
            }
            return image;
        });
    }

    @Test
    void aPlayerWithNoSpriteIsABlueDiscOnTheTileTop() {
        final CanvasProbe shot = CanvasProbe.render(SIZE, SIZE,
                gc -> FxPlayerDrawingHelper.drawPlayer(gc, 80, 80, 1.0, new Player(1, 1), null));

        assertTrue(shot.near(Color.BLUE, TOLERANCE, 70, 70, 90, 90) > 100, "disc around the tile top");
        assertEquals(0, shot.painted(0, 0, SIZE, 60));
    }

    @Test
    void aPlayerSpriteStandsBottomCentredAboveTheTileCentre() {
        final Image sprite = solid(10, 20, Color.RED);

        final CanvasProbe shot = CanvasProbe.render(SIZE, SIZE,
                gc -> FxPlayerDrawingHelper.drawPlayer(gc, 80, 80, 1.0, new Player(1, 1), sprite));

        assertTrue(shot.near(Color.RED, TOLERANCE) >= 200, "the whole sprite is drawn");
        assertTrue(shot.near(Color.RED, TOLERANCE, 75, 60, 85, 100) > 0, "centred on the tile top vertex");
        assertEquals(0, shot.painted(0, 0, SIZE, 50), "nothing above the sprite");
    }

    @Test
    void anAttackingPlayerIsDrawnTilted() {
        final Image sprite = solid(10, 20, Color.RED);
        final Player attacking = new Player(1, 1);
        assertTrue(attacking.startAttack(0));

        final CanvasProbe resting = CanvasProbe.render(SIZE, SIZE,
                gc -> FxPlayerDrawingHelper.drawPlayer(gc, 80, 80, 1.0, new Player(1, 1), sprite));
        final CanvasProbe tilted = CanvasProbe.render(SIZE, SIZE,
                gc -> FxPlayerDrawingHelper.drawPlayer(gc, 80, 80, 1.0, attacking, sprite));

        assertFalse(resting.sameAs(tilted));
        assertTrue(tilted.near(Color.RED, TOLERANCE) > 100);
    }

    @Test
    void aBuildingWithoutASpriteIsDrawnProcedurally() {
        final Building torch = BuildingFactory.create(BuildingType.STANDING_TORCH, 0, 0);

        final CanvasProbe shot = CanvasProbe.render(SIZE, SIZE, gc -> FxBuildingDrawingHelper.drawBuilding(gc, torch, null,
                80, 120, new CameraView(80, 120, 1.0)));

        assertTrue(shot.near(Color.ORANGE, TOLERANCE) > 0);
    }

    @Test
    void aBuildingWithASpriteIsDrawnFromThatSpriteOnly() {
        final Building torch = BuildingFactory.create(BuildingType.STANDING_TORCH, 0, 0);
        final Image sprite = solid(8, 16, Color.RED);

        final CanvasProbe shot = CanvasProbe.render(SIZE, SIZE, gc -> FxBuildingDrawingHelper.drawBuilding(gc, torch, sprite,
                80, 120, new CameraView(0, 0, 1.0)));

        assertTrue(shot.near(Color.RED, TOLERANCE) >= 100);
        assertEquals(0, shot.near(Color.ORANGE, TOLERANCE));
    }

    @Test
    void layersAreDrawnOneOverTheOther() {
        final Image back = solid(16, 16, Color.RED);
        final Image front = solid(8, 8, Color.BLUE);

        final CanvasProbe shot = CanvasProbe.render(SIZE, SIZE,
                gc -> FxBuildingDrawingHelper.drawLayers(gc, new Image[]{back, front}, 80, 120, 1.0));

        assertTrue(shot.near(Color.RED, TOLERANCE) > 0, "back layer");
        assertTrue(shot.near(Color.BLUE, TOLERANCE) > 0, "front layer");
    }

    @Test
    void aGhostWithSpritesIsTintedAndTranslucent() {
        final Image sprite = solid(16, 16, Color.RED);
        final BuildingPreview preview = new BuildingPreview(0, 0, BuildingType.CHEST, 0, 0, Rgba.RED, Color.RED);

        final CanvasProbe shot = CanvasProbe.render(SIZE, SIZE,
                gc -> FxBuildingDrawingHelper.drawGhost(gc, preview, new Image[]{sprite}, 80, 120, 1.0));

        assertTrue(shot.painted() > 0);
        assertEquals(0, shot.near(Color.RED, 0), "the ghost is never fully opaque");
    }

    @Test
    void aGhostWithNoArtShowsATintedFootprintAndTheProceduralShape() {
        final BuildingPreview preview = new BuildingPreview(0, 0, BuildingType.CHEST, 0, 0, Rgba.RED, Color.RED);

        final CanvasProbe none = CanvasProbe.render(SIZE, SIZE,
                gc -> FxBuildingDrawingHelper.drawGhost(gc, preview, null, 80, 120, 1.0));
        final CanvasProbe empty = CanvasProbe.render(SIZE, SIZE,
                gc -> FxBuildingDrawingHelper.drawGhost(gc, preview, new Image[0], 80, 120, 1.0));

        assertTrue(none.painted() > 0);
        assertTrue(none.sameAs(empty), "null and empty art are the same case");
        assertEquals(0, none.near(Color.DARKGRAY, 0), "the ghost is translucent, so no shape colour is exact");
    }

    @Test
    void aFenceGhostKeepsItsConnections() {
        final BuildingPreview joined = new BuildingPreview(0, 0, BuildingType.FENCE, 0, 15, Rgba.RED, Color.RED);
        final BuildingPreview lone = new BuildingPreview(0, 0, BuildingType.FENCE, 0, 0, Rgba.RED, Color.RED);

        final CanvasProbe a = CanvasProbe.render(SIZE, SIZE,
                gc -> FxBuildingDrawingHelper.drawGhost(gc, joined, null, 80, 120, 1.0));
        final CanvasProbe b = CanvasProbe.render(SIZE, SIZE,
                gc -> FxBuildingDrawingHelper.drawGhost(gc, lone, null, 80, 120, 1.0));

        assertTrue(a.painted() > b.painted());
    }
}
