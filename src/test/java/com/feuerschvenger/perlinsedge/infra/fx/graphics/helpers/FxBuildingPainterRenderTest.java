package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.domain.entities.buildings.Building;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingFactory;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Fence;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.FenceConnections;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.FenceGate;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import com.feuerschvenger.perlinsedge.ui.view.CanvasProbe;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Renders every building the painter knows into a small canvas and checks what lands on it. */
class FxBuildingPainterRenderTest {

    private static final int SIZE = 240;
    private static final int ANCHOR_X = SIZE / 2;
    private static final int ANCHOR_Y = SIZE / 2;
    private static final int TOLERANCE = 8;
    /** Zoom of the bridge shot, where the plank stroke is two whole pixels wide. */
    private static final double BRIDGE_ZOOM = 2.0;

    private static CanvasProbe draw(Building building, double zoom) {
        return CanvasProbe.render(SIZE, SIZE, gc -> FxBuildingPainter.draw(gc, building, ANCHOR_X, ANCHOR_Y, zoom));
    }

    private static CanvasProbe draw(BuildingType type) {
        return draw(BuildingFactory.create(type, 0, 0), 1.0);
    }

    @Test
    void everyBuildingTypePaintsSomethingAroundItsAnchor() {
        for (BuildingType type : BuildingType.values()) {
            final CanvasProbe shot = draw(type);

            assertTrue(shot.painted() > 0, type + " painted nothing");
            assertTrue(shot.painted(ANCHOR_X - 60, ANCHOR_Y - 120, ANCHOR_X + 60, ANCHOR_Y + 60) > 0,
                    type + " is not near its anchor");
            assertEquals(0, shot.painted(0, 0, SIZE, 4), type + " bleeds into the top rows");
        }
    }

    @Test
    void aBiggerZoomPaintsMorePixels() {
        for (BuildingType type : BuildingType.values()) {
            final Building building = BuildingFactory.create(type, 0, 0);

            assertTrue(draw(building, 2.0).painted() > draw(building, 1.0).painted(), type + " did not grow");
        }
    }

    @Test
    void theStandingTorchHasAnOrangeFlameWithAYellowCoreAboveABrownPost() {
        final CanvasProbe shot = draw(BuildingType.STANDING_TORCH);

        assertTrue(shot.near(Color.ORANGE, TOLERANCE) > 0, "outer flame");
        assertTrue(shot.near(Color.YELLOW, TOLERANCE) > 0, "inner flame");
        assertTrue(shot.near(Color.SADDLEBROWN, TOLERANCE) > 0, "post");
        assertEquals(0, shot.painted(0, ANCHOR_Y + 2, SIZE, SIZE), "nothing below the base");
    }

    @Test
    void theBridgeIsAWoodenDeckWithPlankLines() {
        // Zoom 2 gives the plank lines a two pixel stroke that covers whole pixels; at zoom 1 the one pixel stroke on
        // an integer coordinate straddles two pixels and the software pipelines in CI blend it into the deck
        final CanvasProbe shot = draw(BuildingFactory.create(BuildingType.BRIDGE, 0, 0), BRIDGE_ZOOM);

        assertTrue(shot.near(Color.SADDLEBROWN, TOLERANCE) > 500, "deck");
        assertTrue(shot.near(Color.DARKGOLDENROD, TOLERANCE) > 0, "plank outline");
    }

    @Test
    void theFurnaceShowsAGlowingOpeningInAGreyBody() {
        final CanvasProbe shot = draw(BuildingType.FURNACE);

        assertTrue(shot.near(Color.DARKGRAY, TOLERANCE) > 1000, "body");
        assertTrue(shot.near(Color.ORANGERED, TOLERANCE) > 100, "opening");
    }

    @Test
    void theAnvilIsBuiltFromThreeGreyBlocks() {
        final CanvasProbe shot = draw(BuildingType.ANVIL);

        assertTrue(shot.near(Color.DARKGRAY.darker(), TOLERANCE) > 0, "base");
        assertTrue(shot.near(Color.DARKGRAY, TOLERANCE) > 0, "waist");
        assertTrue(shot.near(Color.GRAY, TOLERANCE) > 0, "top");
    }

    @Test
    void theWorkbenchHasABrownTopAndDarkerLegs() {
        final CanvasProbe shot = draw(BuildingType.WORKBENCH);

        assertTrue(shot.near(Color.SADDLEBROWN, TOLERANCE) > 0, "table top");
        assertTrue(shot.near(Color.BROWN, TOLERANCE) > 0, "legs");
    }

    @Test
    void theCampfireHasLogsAndTwoFlameColours() {
        final CanvasProbe shot = draw(BuildingType.CAMPFIRE);

        assertTrue(shot.near(Color.BROWN.darker().darker(), TOLERANCE) > 0, "logs");
        assertTrue(shot.near(Color.ORANGE, TOLERANCE) > 0, "outer flame");
        assertTrue(shot.near(Color.YELLOW, TOLERANCE) > 0, "inner flame");
    }

    @Test
    void theBedHasAFrameABlanketAndAHeadboard() {
        final CanvasProbe shot = draw(BuildingType.BED);

        assertTrue(shot.near(Color.PERU, TOLERANCE + 60) > 0, "frame tinted by the blanket");
        assertTrue(shot.near(Color.SADDLEBROWN, TOLERANCE) > 0, "headboard");
        assertTrue(shot.near(Color.LIGHTGRAY, TOLERANCE + 40) > 0, "blanket");
    }

    @Test
    void theChestHasABrownBodyAndAGreyLock() {
        final CanvasProbe shot = draw(BuildingType.CHEST);
        final Color brown = Color.rgb(139, 69, 19);

        assertTrue(shot.near(Color.DARKGRAY, TOLERANCE) > 0, "lock");
        assertTrue(shot.near(brown.deriveColor(0, 1, 0.9, 1), TOLERANCE) > 0, "left face");
        assertTrue(shot.near(brown.deriveColor(0, 1, 0.8, 1), TOLERANCE) > 0, "right face");
        assertTrue(shot.near(brown.deriveColor(0, 1, 1.2, 1), TOLERANCE) > 0, "lid");
    }

    @Test
    void aLoneFencePostIsDrawnWithoutRails() {
        final CanvasProbe lone = draw(new Fence(0, 0), 1.0);

        assertTrue(lone.near(Color.DARKGOLDENROD, TOLERANCE) > 0, "post");
        assertEquals(0, lone.near(Color.BROWN, TOLERANCE), "no rail");
    }

    @Test
    void everyJoinedSideAddsRailsTowardItsOwnDirection() {
        final int[] sides = {FenceConnections.UPPER_LEFT, FenceConnections.UPPER_RIGHT, FenceConnections.LOWER_LEFT,
                FenceConnections.LOWER_RIGHT};
        final boolean[] toTheLeft = {true, false, true, false};
        for (int i = 0; i < sides.length; i++) {
            final Fence fence = new Fence(0, 0);
            fence.setConnections(sides[i]);
            final CanvasProbe shot = draw(fence, 1.0);

            final int left = shot.near(Color.BROWN, TOLERANCE, 0, 0, ANCHOR_X - 4, SIZE);
            final int right = shot.near(Color.BROWN, TOLERANCE, ANCHOR_X + 4, 0, SIZE, SIZE);
            assertTrue((toTheLeft[i] ? left : right) > 0, "rails on the joined side " + sides[i]);
            assertEquals(0, toTheLeft[i] ? right : left, "no rail on the other side " + sides[i]);
        }
    }

    @Test
    void aFenceJoinedOnAllSidesPaintsMoreThanOneJoinedOnOne() {
        final Fence one = new Fence(0, 0);
        one.setConnections(FenceConnections.UPPER_LEFT);
        final Fence all = new Fence(0, 0);
        all.setConnections(FenceConnections.UPPER_LEFT | FenceConnections.UPPER_RIGHT
                | FenceConnections.LOWER_LEFT | FenceConnections.LOWER_RIGHT);

        assertTrue(draw(all, 1.0).painted() > draw(one, 1.0).painted());
    }

    @Test
    void aClosedGateHasRailsBetweenItsPostsAndAnOpenGateOnlyThePosts() {
        final FenceGate closed = new FenceGate(0, 0);
        final FenceGate open = new FenceGate(0, 0);
        final Tile gateTile = new Tile(0, 0, TileType.GRASS);
        gateTile.setBuilding(open);
        open.toggleOpen(gateTile);

        final CanvasProbe closedShot = draw(closed, 1.0);
        final CanvasProbe openShot = draw(open, 1.0);

        assertTrue(closedShot.near(Color.BROWN, TOLERANCE) > 0, "rails");
        assertEquals(0, openShot.near(Color.BROWN, TOLERANCE), "open gate has no rails");
        assertTrue(openShot.near(Color.DARKGOLDENROD, TOLERANCE) > 0, "posts remain");
        assertFalse(closedShot.sameAs(openShot));
    }

    @Test
    void aRotatedGateRunsTheOtherWayAcrossTheTile() {
        final FenceGate straight = new FenceGate(0, 0);
        final FenceGate turned = new FenceGate(0, 0);
        turned.setRotation(1);

        final CanvasProbe a = draw(straight, 1.0);
        final CanvasProbe b = draw(turned, 1.0);

        assertFalse(a.sameAs(b), "rotation changes the picture");
        assertEquals(a.painted(), b.painted(), a.painted() / 20.0, "the same pieces, mirrored");
    }
}
