package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 * Helper class for rendering Player entities with sprite transformations.
 * Provides fallback rendering when sprite drawing fails.
 */
public final class FxPlayerDrawingHelper {
    private static final double FALLBACK_SIZE_FACTOR = 20.0;
    private static final double SPRITE_CENTER_OFFSET = 0.5;
    private static final double ATTACK_ROTATION_ANGLE = 15.0;
    private static final AppConfig CONFIG = AppConfig.getInstance();

    private FxPlayerDrawingHelper() {} // Prevent instantiation

    /**
     * Draws the player sprite with proper positioning and attack animation.
     * Uses fallback rendering if sprite drawing fails.
     *
     * @param gc            GraphicsContext to draw on
     * @param tileTopX      Screen X coordinate of tile's top vertex
     * @param tileTopY      Screen Y coordinate of tile's top vertex
     * @param zoom          Current zoom level (scaling factor)
     * @param player        Player entity to render
     * @param playerSprite  Sprite image for the player
     */
    public static void drawPlayer(GraphicsContext gc, double tileTopX, double tileTopY,
                                  double zoom, Player player, Image playerSprite) {
        try {
            renderPlayerSprite(gc, tileTopX, tileTopY, zoom, player, playerSprite);
        } catch (Exception e) {
            renderFallbackPlayer(gc, tileTopX, tileTopY, zoom);
        }
    }

    /**
     * Renders the player sprite with scaling and optional attack rotation.
     *
     * @param gc            GraphicsContext to draw on
     * @param tileTopX      X coordinate of tile's top vertex
     * @param tileTopY      Y coordinate of tile's top vertex
     * @param zoom          Current zoom factor
     * @param player        Player entity
     * @param playerSprite  Sprite image
     */
    private static void renderPlayerSprite(GraphicsContext gc, double tileTopX, double tileTopY,
                                           double zoom, Player player, Image playerSprite) {
        final double halfTileHeight = CONFIG.graphics().getTileHalfHeight() * zoom;
        final double spriteWidth = playerSprite.getWidth() * zoom;
        final double spriteHeight = playerSprite.getHeight() * zoom;

        final double drawX = tileTopX - (spriteWidth * SPRITE_CENTER_OFFSET);
        final double drawY = tileTopY - spriteHeight + halfTileHeight;

        if (player.isAttacking()) {
            applyAttackTransformation(gc, drawX, drawY, spriteWidth, spriteHeight);
        }

        gc.drawImage(playerSprite, drawX, drawY, spriteWidth, spriteHeight);

        if (player.isAttacking()) {
            gc.restore(); // Reset transformation state
        }
    }

    /**
     * Applies rotation transformation for attack animation.
     *
     * @param gc            GraphicsContext to transform
     * @param spriteX       Sprite's top-left X coordinate
     * @param spriteY       Sprite's top-left Y coordinate
     * @param spriteWidth   Scaled sprite width
     * @param spriteHeight  Scaled sprite height
     */
    private static void applyAttackTransformation(GraphicsContext gc, double spriteX, double spriteY,
                                                  double spriteWidth, double spriteHeight) {
        gc.save();
        final double pivotX = spriteX + (spriteWidth * SPRITE_CENTER_OFFSET);
        final double pivotY = spriteY + spriteHeight; // Bottom center
        gc.translate(pivotX, pivotY);
        gc.rotate(ATTACK_ROTATION_ANGLE);
        gc.translate(-pivotX, -pivotY);
    }

    /**
     * Renders a fallback representation when sprite rendering fails.
     *
     * @param gc        GraphicsContext to draw on
     * @param centerX   X coordinate of tile's top vertex
     * @param centerY   Y coordinate of tile's top vertex
     * @param zoom      Current zoom factor
     */
    private static void renderFallbackPlayer(GraphicsContext gc, double centerX, double centerY, double zoom) {
        final double size = FALLBACK_SIZE_FACTOR * zoom;
        final double halfSize = size * SPRITE_CENTER_OFFSET;

        gc.setFill(Color.BLUE);
        gc.fillOval(centerX - halfSize, centerY - halfSize, size, size);
    }

}