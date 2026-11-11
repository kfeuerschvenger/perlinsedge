package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Slime;
import com.feuerschvenger.perlinsedge.domain.strategies.EnemyMovementStrategy;
import com.feuerschvenger.perlinsedge.domain.strategies.SlimeMovementStrategy;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.ZoomSteps;
import com.feuerschvenger.perlinsedge.infra.fx.utils.SpriteLayout;
import com.feuerschvenger.perlinsedge.infra.fx.utils.SpriteManager;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import com.feuerschvenger.perlinsedge.infra.fx.utils.FxColors;
import javafx.scene.paint.Color;

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
    private static final double SPRITE_SHADOW_FRACTION = 0.6;

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

        final boolean jumping = slimeMove != null && slimeMove.isJumping();
        // Height above the resting position, which is where the shadow stays
        final double jumpHeight = jumping
                ? Math.sin(slimeMove.getJumpPhase() * Math.PI) * SlimeMovementStrategy.JUMP_HEIGHT_FACTOR : 0.0;
        final double groundY = baseDrawY + jumpHeight;
        final Image frame = SpriteManager.getInstance().getSlimeFrame(
                SpriteLayout.slimeFrame(jumping, slimeMove != null ? slimeMove.getJumpPhase() : 0.0));

        if (frame != null) {
            // The sprite rests on the point the oval is centred on, whole pixels, with a shadow sized to its width
            final double scale = ZoomSteps.artScale(zoom);
            final double width = frame.getWidth() * scale;
            final double height = frame.getHeight() * scale;
            drawSlimeShadow(gc, tileTopX, groundY, width * SPRITE_SHADOW_FRACTION, jumpHeight, slime);
            gc.drawImage(frame, Math.rint(tileTopX - width / 2), Math.rint(baseDrawY - height), width, height);
            return;
        }

        // Render shadow first, on the ground
        drawSlimeShadow(gc, tileTopX, groundY, slimeSize, jumpHeight, slime);
        // Render slime body on top
        drawSlimeBody(gc, tileTopX, baseDrawY, slimeSize, slime, slimeMove); // Pass slimeMove
    }

    /**
     * Renders the slime's shadow on the ground point, shrinking and fading it with the jump height.
     *
     * @param gc         GraphicsContext to render on
     * @param centerX    ground point X
     * @param groundY    ground point Y, which does not move while the slime jumps
     * @param size       size the shadow is proportioned to
     * @param jumpHeight how far the slime is above the ground, in pixels
     * @param slime      the slime, for its colour
     */
    private static void drawSlimeShadow(GraphicsContext gc, double centerX, double groundY,
                                        double size, double jumpHeight, Slime slime) {
        final SlimeShadow.Geometry shadow =
                SlimeShadow.at(size, jumpHeight, SlimeMovementStrategy.JUMP_HEIGHT_FACTOR);

        final Color baseShadowColor = FxColors.toFx(slime.getColor()).darker().darker();
        gc.setFill(new Color(
                baseShadowColor.getRed(),
                baseShadowColor.getGreen(),
                baseShadowColor.getBlue(),
                shadow.opacity()
        ));
        gc.fillOval(centerX + shadow.centreX() - shadow.width() * HALF,
                groundY + shadow.centreY() - shadow.height() * HALF,
                shadow.width(), shadow.height());
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
        gc.setFill(FxColors.toFx(slime.getColor()));
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
        gc.setFill(FxColors.toFx(enemy.getColor()));
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1 * zoom);
        gc.fillOval(screenX - size / 2, screenY - size / 2, size, size);
        gc.strokeOval(screenX - size / 2, screenY - size / 2, size, size);
    }

}