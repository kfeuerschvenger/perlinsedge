package com.feuerschvenger.perlinsedge.infra.fx.graphics;

import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Building;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Slime;
import com.feuerschvenger.perlinsedge.domain.entities.items.DroppedItem;
import com.feuerschvenger.perlinsedge.domain.world.model.*;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.*;
import com.feuerschvenger.perlinsedge.domain.utils.Rgba;
import com.feuerschvenger.perlinsedge.infra.fx.utils.FxColors;
import com.feuerschvenger.perlinsedge.infra.fx.utils.SpriteManager;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * Renders an isometric tile map with frustum culling optimization.
 * Handles tiles, resources, entities, and buildings with depth sorting.
 * The drawables of a frame live in a {@link RenderQueue} whose instances are recycled, and the flash effect and the
 * tile highlight are kept between frames, so a steady frame allocates almost nothing.
 */
public class FxRenderer {

    private final GraphicsContext gc;
    private final Canvas canvas;
    private final FxCamera camera;
    private final SpriteManager spriteManager;
    private final GameStateManager gameState;
    private final RenderQueue renderQueue = new RenderQueue();
    private final FrameStats frameStats = new FrameStats();
    private final ColorAdjust flashEffect = createFlashEffect();
    private double currentFrameGameTime;
    private int previewTileX = -1;
    private int previewTileY = -1;
    private BuildingType previewBuildingType;
    private Rgba previewRgba;
    private Color previewColor;
    private BuildingPreview cachedPreview;
    private TileHighlight cachedHighlight;

    /**
     * Constructs the renderer with the given GraphicsContext and camera.
     *
     * @param gc     The GraphicsContext for canvas drawing
     * @param camera The camera controlling the viewport
     */
    public FxRenderer(GraphicsContext gc, FxCamera camera, GameStateManager gameState) {
        this.gc = gc;
        this.canvas = gc.getCanvas();
        this.camera = camera;
        this.gameState = gameState;
        this.spriteManager = SpriteManager.getInstance();
    }

    private static ColorAdjust createFlashEffect() {
        ColorAdjust effect = new ColorAdjust();
        effect.setBrightness(0.7);
        return effect;
    }

    public void setBuildingPreview(int tileX, int tileY, BuildingType buildingType, Rgba color) {
        this.previewTileX = tileX;
        this.previewTileY = tileY;
        this.previewBuildingType = buildingType;
        if (previewRgba == null || !previewRgba.equals(color)) {
            this.previewRgba = color;
            this.previewColor = FxColors.toFx(color);
        }
    }

    /** Forgets the building preview, so the normal tile highlight is drawn again. */
    public void clearBuildingPreview() {
        previewTileX = -1;
        previewTileY = -1;
        previewBuildingType = null;
        previewRgba = null;
        previewColor = null;
    }

    /**
     * Draws a black and yellow border around the canvas to indicate building mode.
     *
     * @param canvasWidth  The width of the main canvas.
     * @param canvasHeight The height of the main canvas.
     */
    public void drawBuildingModeBorder(double canvasWidth, double canvasHeight) {
        FxBuildingDrawingHelper.drawBuildingModeBorder(gc, canvasWidth, canvasHeight);
    }

    /**
     * Renders the tile map with isometric projection using camera position and zoom.
     * Performs frustum culling and depth sorting for efficient rendering.
     */
    public void render(double alpha, double deltaTime) {
        clearCanvas();
        renderQueue.clear();
        frameStats.record(deltaTime);
        this.currentFrameGameTime = gameState.getGameTime();

        TileMap map = gameState.getCurrentMap();
        final double zoom = camera.getZoom();
        final double offsetX = camera.getX();
        final double offsetY = camera.getY();
        final AppConfig config = AppConfig.getInstance();
        final MapType currentMapType = config.world().getCurrentMapType();
        final boolean isGrayscaleEnabled = config.debug().isGrayscaleEnabled();

        // Tile dimensions calculations
        final double tileWidth = config.graphics().getTileWidth() * zoom;
        final double tileHeight = config.graphics().getTileHeight() * zoom;
        final double halfTileWidth = config.graphics().getTileHalfWidth() * zoom;
        final double halfTileHeight = config.graphics().getTileHalfHeight() * zoom;
        final double waterDepthOffset = halfTileHeight * 0.5;
        final int mapWidth = map.getWidth();
        final int mapHeight = map.getHeight();

        // Get visible tile bounds
        int[] bounds = camera.getVisibleTileBounds(canvas.getWidth(), canvas.getHeight(), mapWidth, mapHeight);
        final int minTileX = bounds[0];
        final int maxTileX = bounds[1];
        final int minTileY = bounds[2];
        final int maxTileY = bounds[3];

        // Render tiles and resources
        for (int y = minTileY; y < maxTileY; y++) {
            for (int x = minTileX; x < maxTileX; x++) {
                final Tile tile = map.getTile(x, y);
                if (tile == null) continue;

                // Calculate screen coordinates
                double screenX = (x - y) * halfTileWidth + offsetX;
                double screenY = (x + y) * halfTileHeight + offsetY;

                // Frustum culling
                if (!isTileVisible(screenX, screenY, tileWidth, tileHeight)) {
                    continue;
                }

                // Adjust water tiles
                if (!isGrayscaleEnabled && tile.getType().isWater()) {
                    screenY += waterDepthOffset;
                }

                // Add tile to render queue
                renderQueue.add(screenX, screenY, screenY + halfTileHeight, tile, null, false, tile);

                // Add resource if present
                final Resource resource = tile.getWorldResource();
                if (tile.hasResource() && !isGrayscaleEnabled && resource != null) {
                    boolean isFlashing = resource.isFlashing();
                    SpriteManager.SpriteMetadata spriteMetadata = spriteManager.getResourceSprite(
                            currentMapType, resource.getType(), resource.getVisualSeed()
                    );
                    // A resource blocks movement whether or not its image exists, so it is always queued;
                    // without an image the painter draws a stand-in shape.
                    Image resourceImage = spriteMetadata != null ? spriteMetadata.getImage() : null;
                    renderQueue.add(screenX, screenY, screenY + tileHeight * 0.5,
                            resource.getType(), resourceImage, isFlashing, tile);
                }
                // Add building if present
                final Building building = tile.getBuilding();
                if (tile.hasBuilding() && !isGrayscaleEnabled && building != null) {
                    renderQueue.add(screenX, screenY, screenY + halfTileHeight, building, null, false, tile);
                }
            }
        }

        int mouseOverTileX = gameState.getMouseOverTileX();
        int mouseOverTileY = gameState.getMouseOverTileY();

        if (mouseOverTileX != -1 && mouseOverTileY != -1) {
            Tile highlightTile = map.getTile(mouseOverTileX, mouseOverTileY);
            if (highlightTile != null) {
                double screenX = (mouseOverTileX - mouseOverTileY) * halfTileWidth + offsetX;
                double screenY = (mouseOverTileX + mouseOverTileY) * halfTileHeight + offsetY;
                double centerY = screenY + config.graphics().getTileHalfHeight() * zoom;

                // Show the construction preview while building, the normal highlight otherwise
                if (previewBuildingType != null && previewColor != null) {
                    renderQueue.add(screenX, centerY, centerY + 1, previewFor(), null, false, null);
                } else {
                    renderQueue.add(screenX, centerY, centerY + 1,
                            highlightFor(screenX, centerY, tileWidth, tileHeight), null, false, null);
                }
            }
        }

        // Add player to render queue
        Player player = gameState.getPlayer();
        if (player != null) {
            final double playerX = (player.getInterpolatedX(alpha) - player.getInterpolatedY(alpha)) * halfTileWidth + offsetX;
            final double playerY = (player.getInterpolatedX(alpha) + player.getInterpolatedY(alpha)) * halfTileHeight + offsetY;
            final Image playerSprite = spriteManager.getEntitySprite(
                    "player", player.getAnimationFrame(), player.getDirection()
            );
            if (playerSprite != null) {
                final double playerDepth = playerY + (config.graphics().getTileHalfHeight() * zoom * 1.5);
                renderQueue.add(playerX, playerY, playerDepth, player, playerSprite, false, null);
            }
        }

        // Add enemies to render queue
        List<Enemy> enemies = gameState.getEnemies();
        if (enemies != null) {
            for (int i = 0, count = enemies.size(); i < count; i++) {
                Enemy enemy = enemies.get(i);
                if (enemy.isDead()) continue;

                double enemyInterpX = enemy.getInterpolatedX(alpha);
                double enemyInterpY = enemy.getInterpolatedY(alpha);

                double enemyScreenX = (enemyInterpX - enemyInterpY) * halfTileWidth + offsetX;
                double enemyScreenY = (enemyInterpX + enemyInterpY) * halfTileHeight + offsetY;
                double enemyDrawDepth = enemyScreenY + (halfTileHeight * 1.5);

                renderQueue.add(enemyScreenX, enemyScreenY, enemyDrawDepth, enemy, null, false, null);
            }
        }

        // Add dropped items to render queue
        List<DroppedItem> droppedItems = gameState.getDroppedItems();
        if (droppedItems != null) {
            for (int i = 0, count = droppedItems.size(); i < count; i++) {
                DroppedItem droppedItem = droppedItems.get(i);
                double itemScreenX = (droppedItem.getTileX() - droppedItem.getTileY()) * halfTileWidth + offsetX;
                double itemScreenY = (droppedItem.getTileX() + droppedItem.getTileY()) * halfTileHeight + offsetY;
                double itemDrawDepth = itemScreenY + (halfTileHeight * 2);

                renderQueue.add(itemScreenX, itemScreenY, itemDrawDepth, droppedItem, null, false, null);
            }
        }

        // Depth sort and render
        renderQueue.sortByDepth();
        gc.save();
        for (int i = 0, count = renderQueue.size(); i < count; i++) {
            drawRenderable(renderQueue.get(i));
        }
        if (player != null) {
            renderPlayerAttack(gc, player, halfTileWidth, halfTileHeight, offsetX, offsetY);
        }
        gc.restore();

        // Draw debug info
        FxDebugDrawingHelper.drawDebugInfo(gc, gameState, canvas, deltaTime);
        if (config.debug().isFpsOverlayEnabled()) {
            FxDebugDrawingHelper.drawFpsOverlay(gc, frameStats, renderQueue.size());
        }
    }

    // Helper methods --------------------------------------------------------

    /** The preview of the building being placed, recreated only when the tile or the colour changes. */
    private BuildingPreview previewFor() {
        BuildingPreview preview = cachedPreview;
        if (preview == null || preview.tileX() != previewTileX || preview.tileY() != previewTileY
                || preview.color() != previewColor) {
            preview = new BuildingPreview(previewTileX, previewTileY, previewColor);
            cachedPreview = preview;
        }
        return preview;
    }

    /** The tile highlight, recreated only when the hovered tile or the zoom changes it. */
    private TileHighlight highlightFor(double centerX, double centerY, double tileWidth, double tileHeight) {
        TileHighlight highlight = cachedHighlight;
        if (highlight == null || highlight.centerX() != centerX || highlight.centerY() != centerY
                || highlight.tileWidth() != tileWidth || highlight.tileHeight() != tileHeight) {
            highlight = new TileHighlight(centerX, centerY, tileWidth, tileHeight);
            cachedHighlight = highlight;
        }
        return highlight;
    }

    /**
     * Clears the entire canvas.
     */
    private void clearCanvas() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    /**
     * Checks if a tile's bounding box is within the visible canvas viewport.
     * This is a basic frustum culling check.
     */
    private boolean isTileVisible(double screenX, double screenY, double tileWidth, double tileHeight) {
        return screenX + tileWidth > 0 &&
                screenX - tileWidth < canvas.getWidth() &&
                screenY + tileHeight * 2 > 0 &&
                screenY - tileHeight * 3 < canvas.getHeight();
    }

    /**
     * Dispatches the drawing call based on the type of the renderable object.
     *
     * @param r The Renderable object to draw.
     */
    private void drawRenderable(Renderable r) {
        final Object object = r.object();
        if (object instanceof Tile tile) {
            drawTile(tile, r);
        } else if (object instanceof ResourceType) {
            drawResource(r);
        } else if (object instanceof Building building) {
            drawBuilding(building);
        } else if (object instanceof Player) {
            drawPlayer(r);
        } else if (object instanceof Enemy) {
            drawEnemy(r);
        } else if (object instanceof DroppedItem) {
            drawDroppedItem(r);
        } else if (object instanceof TileHighlight h) {
            drawHighlightTile(h);
        } else if (object instanceof BuildingPreview bp) {
            drawBuildingPlacementPreview(bp);
        }
    }

    private void drawBuildingPlacementPreview(BuildingPreview preview) {
        Tile tile = gameState.getCurrentMap().getTile(preview.tileX(), preview.tileY());
        if (tile != null) {
            FxBuildingDrawingHelper.drawBuildingPlacementPreview(
                    gc, tile, preview.color(), camera.getX(), camera.getY(), camera.getZoom()
            );
        }
    }

    /**
     * Draws a generic building using its abstract draw method.
     * @param building The building to draw.
     */
    private void drawBuilding(Building building) {
        FxBuildingDrawingHelper.drawBuilding(gc, building, camera.getX(), camera.getY(), camera.getZoom());
    }

    /**
     * Draws highlight lines for the tile currently under the mouse.
     *
     * @param highlight The TileHighlight object containing the highlight information.
     */
    private void drawHighlightTile(TileHighlight highlight) {
        FxTileDrawingHelper.drawTileHighlight(gc, highlight.centerX(), highlight.centerY(),
                highlight.tileWidth(), highlight.tileHeight(), camera.getZoom());
    }

    /**
     * Draws the player entity on the canvas.
     *
     * @param r The Renderable object containing player information.
     */
    private void drawPlayer(Renderable r) {
        Player player = (Player) r.object();
        FxPlayerDrawingHelper.drawPlayer(
                gc, r.screenX(), r.screenY(),
                camera.getZoom(), player, r.sprite()
        );
    }

    /**
     * Draws an enemy entity on the canvas.
     *
     * @param r The Renderable object containing enemy information.
     */
    private void drawEnemy(Renderable r) {
        final Enemy enemy = (Enemy) r.object();
        final double zoom = camera.getZoom();

        if (enemy instanceof Slime) {
            FxEnemyDrawingHelper.drawSlime(gc, r.screenX(), r.screenY(), zoom, (Slime) enemy);
        } else {
            FxEnemyDrawingHelper.drawGenericEnemy(gc, r.screenX(), r.screenY(), zoom, enemy);
        }
    }

    /**
     * Draws a single tile on the canvas. Applies debug effects if enabled.
     *
     * @param tile The Tile object to draw.
     * @param r    The Renderable object containing screen coordinates and flashing status.
     */
    private void drawTile(Tile tile, Renderable r) {
        if (AppConfig.getInstance().debug().isEnabled() &&
                AppConfig.getInstance().debug().isGrayscaleEnabled()) {
            FxTileDrawingHelper.drawDebugTile(gc, r.screenX(), r.screenY(), camera.getZoom(), tile);
            return;
        }

        int saveCount = 0;
        if (r.isFlashing()) {
            gc.save();
            saveCount++;
            gc.setEffect(flashEffect);
        }

        FxTileDrawingHelper.drawTileShape(gc, r.screenX(), r.screenY(), camera.getZoom(), tile);
        if (saveCount > 0) {
            gc.restore();
        }
    }

    /**
     * Draws a resource sprite on the canvas. Applies a flash effect if the resource is flashing.
     *
     * @param r The Renderable object containing resource information and screen coordinates.
     */
    private void drawResource(Renderable r) {
        int saveCount = 0;
        if (r.isFlashing()) {
            gc.save();
            saveCount++;
            gc.setEffect(flashEffect);
        }

        if (r.sprite() != null) {
            FxTileDrawingHelper.drawResourceSprite(
                    gc, r.screenX(), r.screenY(),
                    camera.getZoom(), r.sprite()
            );
        } else {
            FxResourceFallbackPainter.draw(
                    gc, r.screenX(), r.screenY(), camera.getZoom(),
                    (ResourceType) r.object(),
                    AppConfig.getInstance().graphics().getTileHalfHeight() * camera.getZoom()
            );
        }
        if (saveCount > 0) {
            gc.restore();
        }
    }

    /**
     * Draws a dropped item on the canvas with a floating animation.
     *
     * @param r The Renderable object containing dropped item information and screen coordinates.
     */
    private void drawDroppedItem(Renderable r) {
        DroppedItem droppedItem = (DroppedItem) r.object();
        FxItemDrawingHelper.drawDroppedItem(
                gc, r.screenX(), r.screenY(), camera.getZoom(), droppedItem, currentFrameGameTime
        );
    }

    /**
     * Renders the visual effect of a player attack.
     *
     * @param gc The GraphicsContext for drawing.
     * @param player The player entity.
     * @param halfTileWidth Half the width of a zoomed tile.
     * @param halfTileHeight Half the height of a zoomed tile.
     * @param offsetX Camera X offset.
     * @param offsetY Camera Y offset.
     */
    private void renderPlayerAttack(GraphicsContext gc, Player player,
                                    double halfTileWidth, double halfTileHeight,
                                    double offsetX, double offsetY) {
        if (!player.isAttacking()) return;
        double progress = player.getAttackAnimationProgress();
        double[] attackPos = player.getAttackPosition();
        double x = attackPos[0];
        double y = attackPos[1];
        // Convert tile coordinates to screen coordinates
        double screenX = (x - y) * halfTileWidth + offsetX;
        double screenY = (x + y) * halfTileHeight + offsetY;
        // Draw the attack effect (a pulsing circle)
        gc.setFill(Color.rgb(255, 0, 0, 0.5 * (1 - progress)));
        gc.fillOval(
                screenX - 10 * progress,
                screenY - 10 * progress,
                20 * progress,
                20 * progress
        );
    }

}
