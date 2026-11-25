package com.feuerschvenger.perlinsedge.infra.fx.utils;

import com.feuerschvenger.perlinsedge.domain.utils.Rgba;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TilePaletteTest {

    private static final int SPRITE_WIDTH = 32;
    private static final int SPRITE_HEIGHT = 21;
    private static final int TOP_ROWS = 16;
    private static final int OPAQUE_GREEN = 0xFF40A040;
    private static final int OPAQUE_RED = 0xFFA04040;
    private static final int FACE_BROWN = 0xFF7A4A3A;
    private static final int SAND = 0xFFF0D48E;
    private static final int WATER_BLUE = 0x4D18A8C8;
    /** A pixel written to an image is stored premultiplied, which costs a level or two when it is read back. */
    private static final int CHANNEL_TOLERANCE = 3;
    private static final int RGB_MASK = 0xFFFFFF;

    /** A block sprite: the top rows are painted with one colour, the rest with another, the corners stay empty. */
    private static Image block(int top, int face) {
        final var image = new WritableImage(SPRITE_WIDTH, SPRITE_HEIGHT);
        final var writer = image.getPixelWriter();
        for (var y = 0; y < SPRITE_HEIGHT; y++) {
            for (var x = 0; x < SPRITE_WIDTH; x++) {
                writer.setArgb(x, y, y < TOP_ROWS ? top : face);
            }
        }
        writer.setArgb(0, 0, 0);
        return image;
    }

    private static Image sheet(int argb) {
        final var image = new WritableImage(SPRITE_WIDTH, TOP_ROWS);
        final var writer = image.getPixelWriter();
        for (var y = 0; y < TOP_ROWS; y++) {
            for (var x = 0; x < SPRITE_WIDTH; x++) {
                writer.setArgb(x, y, argb);
            }
        }
        return image;
    }

    @Test
    void theAverageCoversTheTopDiamondAndLeavesTheFacesOut() {
        assertEquals(OPAQUE_GREEN, TilePalette.averageTop(List.of(block(OPAQUE_GREEN, FACE_BROWN))));
    }

    @Test
    void theAverageIsTakenOverEveryVariantOfTheArt() {
        final var average = TilePalette.averageTop(List.of(block(0xFF000000 | 0x20, 0), block(0xFF000000 | 0x40, 0)));

        assertEquals(0xFF000030, average);
    }

    @Test
    void anArtWithNoOpaquePixelAveragesToNothing() {
        assertEquals(0, TilePalette.averageTop(List.of(block(0, 0))));
    }

    @Test
    void aTranslucentPixelDoesNotCountAsAnOpaqueOne() {
        assertEquals(0, TilePalette.averageTop(List.of(block(WATER_BLUE, WATER_BLUE))));
    }

    @Test
    void aTileTypeTakesTheAverageOfItsArt() {
        final var palette = TilePalette.of(Map.of("grass", List.of(block(OPAQUE_GREEN, FACE_BROWN))), null);

        assertEquals(OPAQUE_GREEN, palette.argb(MapType.DENSE_FOREST, TileType.GRASS));
    }

    @Test
    void aBiomeThatRedrawsGrassyLandTakesTheColourOfItsOwnArt() {
        final var palette = TilePalette.of(Map.of(
                "grass", List.of(block(OPAQUE_GREEN, FACE_BROWN)),
                "dune", List.of(block(OPAQUE_RED, FACE_BROWN))), null);

        assertEquals(OPAQUE_GREEN, palette.argb(MapType.DENSE_FOREST, TileType.GRASS));
        assertEquals(OPAQUE_RED, palette.argb(MapType.DESERT, TileType.GRASS));
    }

    @Test
    void aTypeWithoutArtKeepsItsFlatColour() {
        final var palette = TilePalette.of(Map.of(), null);

        for (MapType mapType : MapType.values()) {
            for (TileType type : TileType.values()) {
                assertEquals(TilePalette.toArgb(type.getColor()), palette.argb(mapType, type), mapType + " " + type);
            }
        }
    }

    @Test
    void aWaterTileIsItsBedSeenThroughTheSheet() {
        final var palette = TilePalette.of(Map.of("bed", List.of(block(SAND, SAND))), sheet(WATER_BLUE));

        final var colour = palette.argb(MapType.DENSE_FOREST, TileType.SHALLOW_WATER);

        final var expected = TilePalette.composite(SAND, WATER_BLUE);
        for (var shift = 0; shift <= 16; shift += 8) {
            assertEquals((float) ((expected >>> shift) & 0xFF),
                    (float) ((colour >>> shift) & 0xFF), (float) CHANNEL_TOLERANCE, "shift " + shift);
        }
        assertNotEquals(SAND, colour);
        assertEquals(0xFF, colour >>> 24);
    }

    @Test
    void aWaterTileWithoutASheetShowsItsBedAlone() {
        final var palette = TilePalette.of(Map.of("bed", List.of(block(SAND, SAND))), null);

        assertEquals(SAND, palette.argb(MapType.DENSE_FOREST, TileType.SHALLOW_WATER));
    }

    @Test
    void compositingMixesEachChannelByTheOpacityOfTheOverlay() {
        // 30 percent of 0x18a8c8 over 0xf0d48e, rounded per channel
        final var mixed = TilePalette.composite(SAND, WATER_BLUE);

        assertEquals(Math.round(0xF0 * (1 - 77 / 255.0) + 0x18 * (77 / 255.0)), (mixed >>> 16) & 0xFF);
        assertEquals(Math.round(0xD4 * (1 - 77 / 255.0) + 0xA8 * (77 / 255.0)), (mixed >>> 8) & 0xFF);
        assertEquals(Math.round(0x8E * (1 - 77 / 255.0) + 0xC8 * (77 / 255.0)), mixed & 0xFF);
    }

    @Test
    void aFullyOpaqueOverlayHidesTheBase() {
        assertEquals(OPAQUE_RED & RGB_MASK, TilePalette.composite(SAND, OPAQUE_RED) & RGB_MASK);
    }

    @Test
    void aColourPacksIntoArgbWithRoundingAndClamping() {
        assertEquals(0xFFFF0000, TilePalette.toArgb(new Rgba(1, 0, 0, 1)));
        assertEquals(0x80102030, TilePalette.toArgb(Rgba.rgb(16, 32, 48).withOpacityFactor(128 / 255.0)));
        assertEquals(0xFF5A5A5A, TilePalette.toArgb(Rgba.rgb(90, 90, 90)));
    }

    @Test
    void theShippedArtGivesEveryTileTypeAnOpaqueColour() {
        final var palette = SpriteManager.getInstance().getTilePalette();

        for (MapType mapType : MapType.values()) {
            for (TileType type : TileType.values()) {
                assertEquals(0xFF, palette.argb(mapType, type) >>> 24, mapType + " " + type);
            }
        }
    }

    @Test
    void theShippedDesertDrawsGrassyLandInTheWarmColourOfDunes() {
        final var palette = SpriteManager.getInstance().getTilePalette();

        final var dune = palette.argb(MapType.DESERT, TileType.GRASS);
        final var grass = palette.argb(MapType.DENSE_FOREST, TileType.GRASS);

        assertNotEquals(grass, dune);
        assertTrue(((dune >>> 16) & 0xFF) > (dune & 0xFF) + 40, "dunes are warm");
        assertTrue(((grass >>> 8) & 0xFF) > ((grass >>> 16) & 0xFF), "grass is green");
    }
}
