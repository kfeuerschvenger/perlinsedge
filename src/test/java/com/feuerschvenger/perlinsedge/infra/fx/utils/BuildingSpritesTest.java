package com.feuerschvenger.perlinsedge.infra.fx.utils;

import com.feuerschvenger.perlinsedge.domain.entities.buildings.Building;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingFactory;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildingSpritesTest {

    private static Building of(BuildingType type) {
        return BuildingFactory.create(type, 3, 4);
    }

    @Test
    void aPieceWithoutArtAnswersNullSoTheProceduralShapeIsDrawn() {
        final BuildingSprites sprites = new BuildingSprites(name -> null);

        for (BuildingType type : BuildingType.values()) {
            assertNull(sprites.imageFor(of(type), 0), type.name());
            assertNull(sprites.restingImage(type), type.name());
        }
    }

    @Test
    void aPieceWithArtGetsItsImageByTheNameOfTheFile() {
        final List<String> asked = new ArrayList<>();
        final Image image = new WritableImage(32, 24);
        final BuildingSprites sprites = new BuildingSprites(name -> {
            asked.add(name);
            return image;
        });

        assertSame(image, sprites.imageFor(of(BuildingType.WORKBENCH), 0));
        assertSame(image, sprites.imageFor(of(BuildingType.WORKBENCH), 3));
        assertEquals(List.of("workbench_0"), asked, "the image is loaded once and kept");
    }

    @Test
    void theFurnaceIsAlwaysShownColdUntilItCanBeLit() {
        assertEquals("furnace_off_0", BuildingSprites.spriteName(of(BuildingType.FURNACE), 2));
    }

    @Test
    void theCampfireAndTheStandingTorchAnimateFourFramesAndWrap() {
        for (BuildingType type : new BuildingType[]{BuildingType.CAMPFIRE, BuildingType.STANDING_TORCH}) {
            final String base = type.name().toLowerCase();
            for (int frame = 0; frame < 4; frame++) {
                assertEquals(base + "_" + frame, BuildingSprites.spriteName(of(type), frame));
            }
            assertEquals(base + "_0", BuildingSprites.spriteName(of(type), 4));
            assertEquals(base + "_3", BuildingSprites.spriteName(of(type), -1));
        }
    }

    @Test
    void everyOtherPieceIsNamedAfterItsType() {
        assertEquals("workbench_0", BuildingSprites.spriteName(of(BuildingType.WORKBENCH), 0));
        assertEquals("bridge_0", BuildingSprites.spriteName(of(BuildingType.BRIDGE), 0));
    }

    @Test
    void theFlameFramesFollowTheGameClockAtEightFramesPerSecond() {
        assertEquals(0, BuildingSprites.flameFrame(0.0));
        assertEquals(1, BuildingSprites.flameFrame(0.13));
        assertEquals(3, BuildingSprites.flameFrame(0.4));
        assertEquals(0, BuildingSprites.flameFrame(0.5));
    }

    @Test
    void theBottomCentreOfTheImageSitsOnTheBottomVertexOfTheTile() {
        // A 32 by 36 wall at art scale 2: centred on the tile, its bottom edge 8 art pixels under the tile centre
        assertEquals(100 - 32, BuildingSprites.drawX(100, 32, 2), 1e-9);
        assertEquals(200 + (8 - 36) * 2, BuildingSprites.drawY(200, 36, 2), 1e-9);
        assertEquals(200 + 8 * 2, BuildingSprites.drawY(200, 36, 2) + 36 * 2, 1e-9, "the bottom edge");
    }

    @Test
    void aSpriteAsTallAsItsFootprintHasItsTopOnTheTopVertexOfTheTile() {
        // The footprint diamond is the bottom 16 rows, so a 16 row image has its top 8 rows above the tile centre
        assertEquals(200 - 8 * 3, BuildingSprites.drawY(200, 16, 3), 1e-9);
    }

    @Test
    void everyBuildingFileThatExistsIsThirtyTwoPixelsWide() throws IOException {
        final Path folder = Path.of("src/main/resources/assets/buildings");
        if (!Files.isDirectory(folder)) {
            return;
        }
        try (Stream<Path> files = Files.list(folder)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".png")).toList()) {
                try (InputStream in = Files.newInputStream(file)) {
                    final byte[] header = in.readNBytes(24);
                    final int width = ((header[16] & 0xFF) << 24) | ((header[17] & 0xFF) << 16)
                            | ((header[18] & 0xFF) << 8) | (header[19] & 0xFF);
                    assertEquals(32, width, file.getFileName().toString());
                }
            }
        }
    }

    @Test
    void theClasspathLoaderAnswersNullForAMissingFileAndAnImageForAPresentOne() {
        assertNotNull(BuildingSprites.getInstance());
        // The shipped art may be absent: the answer is null then, and the caller draws the procedural shape
        final Image image = BuildingSprites.getInstance().restingImage(BuildingType.WORKBENCH);
        final boolean shipped = BuildingSpritesTest.class.getResource("/assets/buildings/workbench_0.png") != null;
        assertTrue(shipped == (image != null));
    }

    @Test
    void aRotatablePieceHasOneFilePerWayItFaces() {
        for (int rotation = 0; rotation < 4; rotation++) {
            final Building chest = BuildingFactory.create(BuildingType.CHEST, 1, 1, rotation);
            assertEquals("chest_" + rotation, BuildingSprites.spriteName(chest, 0));
            assertEquals("chest_" + rotation, BuildingSprites.restingName(BuildingType.CHEST, rotation));
            assertEquals("anvil_" + rotation, BuildingSprites.restingName(BuildingType.ANVIL, rotation));
            assertEquals("workbench_" + rotation, BuildingSprites.restingName(BuildingType.WORKBENCH, rotation));
            assertEquals("bed_" + rotation, BuildingSprites.restingName(BuildingType.BED, rotation));
            assertEquals("dock_" + rotation, BuildingSprites.restingName(BuildingType.DOCK, rotation));
            assertEquals("furnace_off_" + rotation, BuildingSprites.spriteName(
                    BuildingFactory.create(BuildingType.FURNACE, 1, 1, rotation), 0));
        }
        assertEquals("bridge_0", BuildingSprites.restingName(BuildingType.BRIDGE, 0));
        assertEquals("bridge_1", BuildingSprites.restingName(BuildingType.BRIDGE, 1));
    }

    @Test
    void theGhostAsksForTheArtOfTheRotationItShows() {
        final List<String> asked = new ArrayList<>();
        final BuildingSprites sprites = new BuildingSprites(name -> {
            asked.add(name);
            return null;
        });

        sprites.restingImage(BuildingType.BED, 2);

        assertEquals(List.of("bed_2"), asked);
    }

    @Test
    void aPieceThatDoesNotTurnHasNoRotationInItsName() {
        assertEquals("campfire_0", BuildingSprites.restingName(BuildingType.CAMPFIRE, 3));
        assertEquals("fence_post", BuildingSprites.restingName(BuildingType.FENCE, 2));
    }

    @Test
    void theGateHasOneFilePerAxisAndStateAndTheFenceIsComposed() {
        final com.feuerschvenger.perlinsedge.domain.entities.buildings.FenceGate gate =
                new com.feuerschvenger.perlinsedge.domain.entities.buildings.FenceGate(1, 1);
        gate.setRotation(1);

        assertEquals("fence_gate_closed_1", BuildingSprites.spriteName(gate, 0));
        gate.toggleOpen();
        assertEquals("fence_gate_open_1", BuildingSprites.spriteName(gate, 0));
        assertEquals("fence_gate_closed_0", BuildingSprites.restingName(BuildingType.FENCE_GATE, 0));
    }

    private static BuildingSprites fenceSprites(List<String> asked, boolean withPost) {
        return new BuildingSprites(name -> {
            asked.add(name);
            return name.equals("fence_post") && !withPost ? null : new WritableImage(32, 24);
        });
    }

    @Test
    void aFenceIsComposedOfBackRailsThePostAndFrontRailsInThatOrder() {
        final List<String> asked = new ArrayList<>();
        final BuildingSprites sprites = fenceSprites(asked, true);

        final Image[] layers = sprites.fenceLayers(15);

        assertEquals(5, layers.length);
        assertEquals(java.util.Set.of("fence_post", "fence_rail_ul", "fence_rail_ur", "fence_rail_ll", "fence_rail_lr"),
                new java.util.HashSet<>(asked));
    }

    @Test
    void aLoneFencePostIsOneLayerAndARunHasOnlyTheRailsItJoins() {
        final BuildingSprites sprites = fenceSprites(new ArrayList<>(), true);

        assertEquals(1, sprites.fenceLayers(0).length);
        assertEquals(3, sprites.fenceLayers(com.feuerschvenger.perlinsedge.domain.entities.buildings.FenceConnections.UPPER_LEFT
                | com.feuerschvenger.perlinsedge.domain.entities.buildings.FenceConnections.LOWER_RIGHT).length);
    }

    @Test
    void aFenceWithoutPostArtFallsBackToTheProceduralShape() {
        final BuildingSprites sprites = fenceSprites(new ArrayList<>(), false);

        assertNull(sprites.fenceLayers(15));
        assertEquals(0, sprites.shadowImages(BuildingFactory.create(BuildingType.FENCE, 1, 1)).length);
    }

    @Test
    void aFenceCastsOneShadowImagePerLayer() {
        final BuildingSprites sprites = fenceSprites(new ArrayList<>(), true);
        final com.feuerschvenger.perlinsedge.domain.entities.buildings.Fence fence =
                new com.feuerschvenger.perlinsedge.domain.entities.buildings.Fence(1, 1);
        fence.setConnections(com.feuerschvenger.perlinsedge.domain.entities.buildings.FenceConnections.LOWER_LEFT);

        assertEquals(2, sprites.shadowImages(fence).length);
        assertEquals(1, sprites.shadowImages(BuildingFactory.create(BuildingType.CHEST, 1, 1)).length);
    }
}
