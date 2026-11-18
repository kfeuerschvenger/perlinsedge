package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Slime;
import com.feuerschvenger.perlinsedge.domain.strategies.EnemyMovementStrategy;
import com.feuerschvenger.perlinsedge.domain.strategies.SlimeMovementStrategy;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.ShadowList;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.ZoomSteps;
import com.feuerschvenger.perlinsedge.infra.fx.utils.SilhouetteSprites;
import com.feuerschvenger.perlinsedge.infra.fx.utils.SpriteLayout;
import com.feuerschvenger.perlinsedge.infra.fx.utils.SpriteManager;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import com.feuerschvenger.perlinsedge.infra.fx.utils.FxColors;
import javafx.scene.paint.Color;

import java.util.function.IntFunction;

/**
 * Optimized helper for rendering slime enemies with proper shadow effects and animation.
 */
public final class FxEnemyDrawingHelper {
    // Animation constants
    private static final double JUMP_SCALE_FACTOR = 0.2;
    private static final double GROUNDED_WIDTH_FACTOR = 1.2;
    private static final double GROUNDED_HEIGHT_FACTOR = 0.8;

    // Cached configuration
    private static final AppConfig CONFIG = AppConfig.getInstance();
    private static final double BASE_SLIME_SIZE = 20.0;
    private static final double HALF = 0.5;
    /** Diameter of the circle drawn for an enemy without art, in pixels at zoom 1. */
    private static final double GENERIC_ENEMY_SIZE = 30.0;
    private static final double GENERIC_SHADOW_FRACTION = 0.8;

    private FxEnemyDrawingHelper() {} // Prevent instantiation

    /**
     * Renders a slime enemy with dynamic animation and proper shadow effects.
     *
     * @param gc GraphicsContext to render on
     * @param tileTopX Tile's top vertex X coordinate
     * @param tileTopY Tile's top vertex Y coordinate
     * @param zoom Current zoom factor
     * @param slime Slime entity to render
     * @param gameTime the game clock in seconds, which drives the breathing of a slime that stands still
     */
    public static void drawSlime(GraphicsContext gc, double tileTopX, double tileTopY, double zoom, Slime slime,
                                 double gameTime) {
        // Pre-calculate base position and size
        final double halfTileHeight = CONFIG.graphics().getTileHalfHeight() * zoom;
        // Use slime.getRenderOffsetY() to correctly position based on its internal jump state
        final double baseDrawY = tileTopY + (halfTileHeight * 1.5) + slime.getRenderOffsetY();

        final SlimeMovementStrategy slimeMove = movementOf(slime);
        final boolean jumping = slimeMove != null && slimeMove.isJumping();
        final Image frame = SpriteManager.getInstance().getSlimeFrame(
                SpriteLayout.slimeFrame(jumping, slimeMove != null ? slimeMove.getJumpPhase() : 0.0));

        if (frame != null) {
            // The sprite rests on the point the oval is centred on, whole pixels
            final double scale = ZoomSteps.integerArtScale(zoom);
            final double width = drawnSize(frame.getWidth(), scale, jumping ? 1.0 : restingWidthFactor(slime, gameTime));
            final double height = drawnSize(frame.getHeight(), scale,
                    jumping ? 1.0 : restingHeightFactor(slime, gameTime));
            gc.drawImage(frame, PixelSnap.round(tileTopX - width * HALF), PixelSnap.round(baseDrawY - height),
                    width, height);
            return;
        }

        final double slimeSize = BASE_SLIME_SIZE * zoom;
        drawSlimeBody(gc, tileTopX, baseDrawY, slimeSize, slime, slimeMove);
    }

    /**
     * Adds the slime's ground shadow to the shadow pass, shrinking and fading it with the jump height.
     * The shadow is centred on the ground point under the sprite, which does not move while the slime jumps.
     *
     * @param shadows   the shadows of the frame
     * @param tileTopX  tile's top vertex X coordinate
     * @param tileTopY  tile's top vertex Y coordinate
     * @param zoom      current zoom factor
     * @param slime     the slime
     * @param gameTime  the game clock in seconds, which drives the breathing of a slime that stands still
     */
    public static void addSlimeShadow(ShadowList shadows, double tileTopX, double tileTopY, double zoom, Slime slime,
                                      double gameTime) {
        addSlimeShadow(shadows, tileTopX, tileTopY, zoom, slime, gameTime, SpriteManager.getInstance()::getSlimeFrame);
    }

    /**
     * Adds the shadow of a slime, taking its frames from a source. The shadow is cast from the frame the body is drawn
     * with, at the size and with the rounding the body is drawn at, so the two stand on the same point and have the same
     * form while the slime breathes, winds up or jumps.
     *
     * @param shadows   the shadows of the frame
     * @param tileTopX  tile's top vertex X coordinate
     * @param tileTopY  tile's top vertex Y coordinate
     * @param zoom      current zoom factor
     * @param slime     the slime
     * @param gameTime  the game clock in seconds
     * @param frames    gives the image of a slime frame, null when there is none, see {@link SpriteLayout#slimeFrame}
     */
    static void addSlimeShadow(ShadowList shadows, double tileTopX, double tileTopY, double zoom, Slime slime,
                               double gameTime, IntFunction<Image> frames) {
        final double halfTileHeight = CONFIG.graphics().getTileHalfHeight() * zoom;
        final double baseDrawY = tileTopY + (halfTileHeight * 1.5) + slime.getRenderOffsetY();
        final SlimeMovementStrategy slimeMove = movementOf(slime);
        final boolean jumping = slimeMove != null && slimeMove.isJumping();
        // Height above the resting position, which is where the shadow stays
        final double jumpHeight = jumping
                ? Math.sin(slimeMove.getJumpPhase() * Math.PI) * SlimeMovementStrategy.JUMP_HEIGHT_FACTOR : 0.0;
        final double scale = ZoomSteps.integerArtScale(zoom);
        final Image frame = frames.apply(
                SpriteLayout.slimeFrame(jumping, slimeMove != null ? slimeMove.getJumpPhase() : 0.0));
        final double fade = SlimeShadow.opacity(jumpHeight, SlimeMovementStrategy.JUMP_HEIGHT_FACTOR);
        if (frame != null) {
            // The body is drawn at whole screen pixels, and the shadow is anchored and shaped from that same size
            final double size = frame.getWidth() * scale;
            final double drawnWidth = drawnSize(frame.getWidth(), scale,
                    jumping ? 1.0 : restingWidthFactor(slime, gameTime));
            final double drawnHeight = drawnSize(frame.getHeight(), scale,
                    jumping ? 1.0 : restingHeightFactor(slime, gameTime));
            // The shadow narrows with the height of the jump by the same share the oval would, in steps so a jump only
            // takes a few shapes, and keeps the form of the slime
            final double narrowing = SlimeShadow.quantiseFactor(
                    SlimeShadow.width(size, jumpHeight, SlimeMovementStrategy.JUMP_HEIGHT_FACTOR)
                            / SlimeShadow.width(size, 0.0, SlimeMovementStrategy.JUMP_HEIGHT_FACTOR));
            final var silhouette = SilhouetteSprites.of(frame);
            shadows.addSilhouette(tileTopX, baseDrawY + jumpHeight, silhouette, silhouette.height(),
                    narrowing * drawnWidth / size, new ShadowList.SpriteSize(drawnWidth / scale, drawnHeight / scale),
                    fade);
            return;
        }
        shadows.add(tileTopX, baseDrawY + jumpHeight,
                SlimeShadow.width(BASE_SLIME_SIZE * zoom, jumpHeight, SlimeMovementStrategy.JUMP_HEIGHT_FACTOR) / scale,
                fade);
    }

    /**
     * Adds the ground shadow of an enemy drawn as a generic circle, centred under the circle.
     *
     * @param shadows the shadows of the frame
     * @param screenX screen X of the enemy
     * @param screenY screen Y of the enemy
     * @param zoom    current zoom factor
     */
    public static void addGenericShadow(ShadowList shadows, double screenX, double screenY, double zoom) {
        final double size = GENERIC_ENEMY_SIZE * zoom;
        shadows.add(screenX, screenY + size * HALF, size * GENERIC_SHADOW_FRACTION / ZoomSteps.integerArtScale(zoom), 1.0);
    }

    /**
     * Returns the size a slime frame is drawn at along one axis, in whole screen pixels.
     *
     * @param frameSize the size of the frame in art pixels along the axis
     * @param scale     screen pixels per art pixel
     * @param factor    how much the slime is stretched along the axis
     * @return the drawn size
     */
    private static double drawnSize(double frameSize, double scale, double factor) {
        return Math.rint(frameSize * scale * factor);
    }

    /** Returns how wide a slime that stands still is drawn: breathing, or spread by the squash before a strike. */
    private static double restingWidthFactor(Slime slime, double gameTime) {
        return SlimeSquash.width(slime.getWindupProgress(), SlimeSquash.breath(gameTime, slime.getBreathPhase()));
    }

    /** Returns how tall a slime that stands still is drawn: breathing, or squashed before a strike. */
    private static double restingHeightFactor(Slime slime, double gameTime) {
        return SlimeSquash.height(slime.getWindupProgress(), SlimeSquash.breath(gameTime, slime.getBreathPhase()));
    }

    /** Returns the slime movement strategy of a slime, or null when it moves by another strategy. */
    private static SlimeMovementStrategy movementOf(Slime slime) {
        final EnemyMovementStrategy movementStrategy = slime.getMovementStrategy();
        return movementStrategy instanceof SlimeMovementStrategy move ? move : null;
    }

    /**
     * Renders the slime body with dynamic shape based on movement state.
     * @param slimeMove The SlimeMovementStrategy instance to get jump state and phase.
     */
    private static void drawSlimeBody(GraphicsContext gc, double centerX, double centerY,
                                      double size, Slime slime, SlimeMovementStrategy slimeMove) {
        double width;
        double height;

        // Check if movement strategy is available and jumping
        if (slimeMove != null && slimeMove.isJumping()) {
            // Dynamic squash/stretch during jumps
            final double jumpPhase = slimeMove.getJumpPhase();
            final double scaleFactor = 1.0 + Math.sin(jumpPhase * Math.PI) * JUMP_SCALE_FACTOR;
            width = size * scaleFactor;
            height = size / scaleFactor;
        } else {
            // Oval shape when grounded or if movement strategy is not SlimeMovementStrategy
            width = size * GROUNDED_WIDTH_FACTOR;
            height = size * GROUNDED_HEIGHT_FACTOR;
        }

        // Calculate body position (centered)
        final double bodyX = centerX - (width * HALF);
        final double bodyY = centerY - (height * HALF); // centerY already includes offset for jumping

        // Render body
        gc.setFill(FxColors.toFx(slime.getColor()));
        gc.fillOval(bodyX, bodyY, width, height);
    }

    /**
     * Draws a generic enemy as a fallback or for unhandled enemy types.
     * @param gc GraphicsContext to render on.
     * @param screenX Screen X coordinate for drawing.
     * @param screenY Screen Y coordinate for drawing.
     * @param zoom Current zoom factor.
     * @param enemy The Enemy entity to draw.
     */
    public static void drawGenericEnemy(GraphicsContext gc, double screenX, double screenY, double zoom, Enemy enemy) {
        double size = GENERIC_ENEMY_SIZE * zoom;
        gc.setFill(FxColors.toFx(enemy.getColor()));
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1 * zoom);
        gc.fillOval(screenX - size * HALF, screenY - size * HALF, size, size);
        gc.strokeOval(screenX - size * HALF, screenY - size * HALF, size, size);
    }

}
