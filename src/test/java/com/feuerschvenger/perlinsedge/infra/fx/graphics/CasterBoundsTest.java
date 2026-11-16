package com.feuerschvenger.perlinsedge.infra.fx.graphics;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The reach of an off-view caster is sized from constants, which must cover the largest sprite that casts. */
class CasterBoundsTest {

    private static final Path RESOURCES = Path.of("src", "main", "resources", "assets", "resources");

    private static List<Path> resourceSprites() throws IOException {
        try (Stream<Path> files = Files.walk(RESOURCES)) {
            return files.filter(path -> path.toString().endsWith(".png")).toList();
        }
    }

    @Test
    void theCasterConstantsCoverTheLargestResourceSprite() throws IOException {
        final List<Path> sprites = resourceSprites();
        assertFalse(sprites.isEmpty());
        int tallest = 0;
        int widest = 0;
        for (Path path : sprites) {
            final BufferedImage image = ImageIO.read(path.toFile());
            tallest = Math.max(tallest, image.getHeight());
            widest = Math.max(widest, image.getWidth());
        }

        assertTrue(FxRenderer.MAX_CASTER_ART_HEIGHT >= tallest,
                "a sprite " + tallest + " high casts further than the reach allows for");
        assertTrue(FxRenderer.MAX_CASTER_ART_HALF_WIDTH >= widest / 2.0,
                "a sprite " + widest + " wide casts further sideways than the reach allows for");
    }

    @Test
    void theCasterConstantsAreNotMuchLargerThanTheLargestSprite() throws IOException {
        int tallest = 0;
        for (Path path : resourceSprites()) {
            tallest = Math.max(tallest, ImageIO.read(path.toFile()).getHeight());
        }

        assertTrue(FxRenderer.MAX_CASTER_ART_HEIGHT <= tallest * 1.5,
                "an oversized reach walks far more tiles than it needs to");
    }
}
