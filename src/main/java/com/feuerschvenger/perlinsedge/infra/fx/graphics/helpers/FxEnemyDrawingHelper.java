package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Slime;
import com.feuerschvenger.perlinsedge.domain.strategies.EnemyMovementStrategy;
import com.feuerschvenger.perlinsedge.domain.strategies.SlimeMovementStrategy;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Optimized helper for rendering slime enemies with proper shadow effects and animation.
 */
public final class FxEnemyDrawingHelper {
    // Animation constants
    private static final double JUMP_SCALE_FACTOR = 0.2;
    private static final double SHADOW_OFFSET_FACTOR = 0.1;
    private static final double SHADOW_OPACITY = 0.8;
    private static final double SHADOW_SIZE_FACTOR = 1.2;
    private static final double SHADOW_HEIGHT_FACTOR = 0.8;
    private static final double GROUNDED_WIDTH_FACTOR = 1.2;
    private static final double GROUNDED_HEIGHT_FACTOR = 0.8;

    // Cached configuration
    private static final AppConfig CONFIG = AppConfig.getInstance();
    private static final double BASE_SLIME_SIZE = 20.0;
    private static final double HALF = 0.5;

    private FxEnemyDrawingHelper() {} // Prevent instantiation

    /**
     * Renders a slime enemy with dynamic animation and proper shadow effects.
     *
     * @param gc GraphicsContext to render on
     * @param tileTopX Tile's top vertex X coordinate
     * @param tileTopY Tile's top vertex Y coordinate
     * @param zoom Current zoom factor
     * @param slime Slime entity to render
     */
    public static void drawSlime(GraphicsContext gc, double tileTopX, double tileTopY, double zoom, Slime slime) {
        // Pre-calculate base position and size
        final double halfTileHeight = CONFIG.graphics().getTileHalfHeight() * zoom;
        // Use slime.getRenderOffsetY() to correctly position based on its internal jump state
        final double baseDrawY = tileTopY + (halfTileHeight * 1.5) + slime.getRenderOffsetY();
        final double slimeSize = BASE_SLIME_SIZE * zoom;

        // Get the movement strategy to check jump state
        EnemyMovementStrategy movementStrategy = slime.getMovementStrategy();
        SlimeMovementStrategy slimeMove = null;
        if (movementStrategy instanceof SlimeMovementStrategy) {
            slimeMove = (SlimeMovementStrategy) movementStrategy;
        }

        // Render shadow first (fixed positioning)
        drawSlimeShadow(gc, tileTopX, baseDrawY, slimeSize, slime, slimeMove); // Pass slimeMove
        // Render slime body on top
        drawSlimeBody(gc, tileTopX, baseDrawY, slimeSize, slime, slimeMove); // Pass slimeMove
    }

    /**
     * Renders the slime's shadow with proper positioning and opacity.
     * @param slimeMove The SlimeMovementStrategy instance to get jump state.
     */
    private static void drawSlimeShadow(GraphicsContext gc, double centerX, double centerY,
                                        double size, Slime slime, SlimeMovementStrategy slimeMove) {
        final double shadowWidth = size * SHADOW_SIZE_FACTOR;
        final double shadowHeight = size * SHADOW_HEIGHT_FACTOR;
        final double shadowOffset = size * SHADOW_OFFSET_FACTOR;

        // Calculate shadow position (centered with offset)
        // If jumping, the shadow should remain on the ground.
        final double shadowX = centerX - (shadowWidth * HALF) + shadowOffset;
        final double shadowY = centerY - (shadowHeight * HALF) + shadowOffset; // This centerY already includes getRenderOffsetY

        // Create shadow color with proper opacity
        final Color baseShadowColor = slime.getColor().darker().darker();
        final Color shadowColor = new Color(
                baseShadowColor.getRed(),
                baseShadowColor.getGreen(),
                baseShadowColor.getBlue(),
                SHADOW_OPACITY
        );

        // Render shadow
        gc.setFill(shadowColor);
        gc.fillOval(shadowX, shadowY, shadowWidth, shadowHeight);
    }

    /**
     * Renders the slime body with dynamic shape based on movement state.
     * @param slimeMove The SlimeMovementStrategy instance to get jump state and phase.
     */
    private static void drawSlimeBody(GraphicsContext gc, double centerX, double centerY,
                                      double size, Slime slime, SlimeMovementStrategy slimeMove) {
        double width, height;

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
        gc.setFill(slime.getColor());
        gc.fillOval(bodyX, bodyY, width, height);
    }

    /**
     * Draws a generic enemy as a fallback or for unhandled enemy types.
     * This method was assumed to exist in a previous turn.
     * @param gc GraphicsContext to render on.
     * @param screenX Screen X coordinate for drawing.
     * @param screenY Screen Y coordinate for drawing.
     * @param zoom Current zoom factor.
     * @param enemy The Enemy entity to draw.
     */
    public static void drawGenericEnemy(GraphicsContext gc, double screenX, double screenY, double zoom, Enemy enemy) {
        double size = 30 * zoom;
        gc.setFill(enemy.getColor());
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1 * zoom);
        gc.fillOval(screenX - size / 2, screenY - size / 2, size, size);
        gc.strokeOval(screenX - size / 2, screenY - size / 2, size, size);
    }

}