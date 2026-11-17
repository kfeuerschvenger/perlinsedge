package com.feuerschvenger.perlinsedge.infra.fx.graphics;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.FxShadowHelper;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.SlimeSquash;
import com.feuerschvenger.perlinsedge.infra.fx.utils.AssetPaths;
import com.feuerschvenger.perlinsedge.infra.fx.utils.GroundShadow;
import com.feuerschvenger.perlinsedge.infra.fx.utils.SpriteLayout;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    void theCasterConstantsCoverTheFramesOfThePlayer() {
        final AppConfig.PlayerConfig.SpriteSheetConfig sheet = AppConfig.getInstance().player().spriteSheet;

        assertTrue(FxRenderer.MAX_CASTER_ART_HEIGHT >= sheet.spriteHeight,
                "a player " + sheet.spriteHeight + " high casts further than the reach allows for");
        assertTrue(FxRenderer.MAX_CASTER_ART_HALF_WIDTH >= sheet.spriteWidth / 2.0,
                "a player " + sheet.spriteWidth + " wide casts further sideways than the reach allows for");
    }

    @Test
    void theCasterConstantsCoverTheSlimeFramesAtTheirMostStretchedAndSpread() throws IOException {
        final BufferedImage strip;
        try (InputStream in = getClass().getResourceAsStream(AssetPaths.SLIME_STRIP)) {
            assertNotNull(in, "the slime strip ships with the game");
            strip = ImageIO.read(in);
        }
        final double frameWidth = strip.getWidth() / SpriteLayout.SLIME_FRAMES;
        // The most the body is drawn taller is a breath in, the most it is drawn wider is the wind up of a strike
        final double tallestFactor = Math.max(1.0, SlimeSquash.height(0.0, 1.0));
        final double widestFactor = Math.max(1.0, SlimeSquash.width(1.0, 0.0));

        assertTrue(FxRenderer.MAX_CASTER_ART_HEIGHT >= strip.getHeight() * tallestFactor,
                "a stretched slime casts further than the reach allows for");
        assertTrue(FxRenderer.MAX_CASTER_ART_HALF_WIDTH >= frameWidth * widestFactor / 2.0,
                "a spread slime casts further sideways than the reach allows for");
    }

    @Test
    void theCasterConstantsCoverTheWidestBuildingEllipseAtTheLongestShadow() {
        final double longest = AppConfig.getInstance().dayCycle().settings().maxShadowLength();
        double widest = 0.0;
        for (BuildingType type : BuildingType.values()) {
            widest = Math.max(widest, GroundShadow.widthFor(type));
        }
        final double radius = widest / 2.0;

        assertTrue(FxRenderer.MAX_CASTER_ART_HALF_WIDTH >= radius,
                "a building shadow " + widest + " wide spreads further sideways than the reach allows for");
        // The ellipse is stretched along the shadow, so it reaches radius times one plus the length factor from its anchor
        final double alongReach = radius * (1.0 + longest);
        assertTrue(FxShadowHelper.reachMargin(FxRenderer.MAX_CASTER_ART_HEIGHT, FxRenderer.MAX_CASTER_ART_HALF_WIDTH,
                longest, 1.0) >= alongReach, "a building shadow reaches " + alongReach + " art pixels along");
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
