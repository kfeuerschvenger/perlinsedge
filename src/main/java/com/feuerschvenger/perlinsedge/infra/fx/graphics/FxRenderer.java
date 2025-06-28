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
import com.feuerschvenger.perlinsedge.infra.fx.utils.SpriteManager;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Renders an isometric tile map with frustum culling optimization.
 * Handles tiles, resources, entities, and buildings with depth sorting.
 */
public class FxRenderer {

    private final GraphicsContext gc;
    private final Canvas canvas;
    private final FxCamera camera;
    private final SpriteManager spriteManager;
    private final GameStateManager gameState;
    private final List<Renderable> renderQueue = new ArrayList<>();
    private double currentFrameGameTime;
    private int previewTileX = -1;
    private int previewTileY = -1;
    private BuildingType previewBuildingType;
    private Color previewColor;

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

    public void setBuildingPreview(int tileX, int tileY, BuildingType buildingType, Color color) {
        this.previewTileX = tileX;
        this.previewTileY = tileY;
        this.previewBuildingType = buildingType;
        this.previewColor = color;
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
                renderQueue.add(new Renderable(
                        screenX, screenY, screenY + halfTileHeight,
                        tile, null, false, false, tile
                ));

                // Add resource if present
                final Resource resource = tile.getWorldResource();
                if (tile.hasResource() && !isGrayscaleEnabled && resource != null) {
                    boolean isFlashing = resource.isFlashing();
                    SpriteManager.SpriteMetadata spriteMetadata = spriteManager.getResourceSprite(
                            currentMapType, resource.getType(), resource.getVisualSeed()
                    );
                    if (spriteMetadata != null) {
                        renderQueue.add(new Renderable(
                                screenX, screenY, screenY + tileHeight * 0.5,
                                resource.getType(), spriteMetadata.getImage(),
                                false, isFlashing, tile
                        ));
                    }
                }
                // Add building if present
                final Building building = tile.getBuilding();
                if (tile.hasBuilding() && !isGrayscaleEnabled && building != null) {
                    renderQueue.add(new Renderable(
                            screenX, screenY, screenY + halfTileHeight,
                            building, null, false, false, tile
                    ));
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

                // Mostrar previsualización de construcción si estamos en modo construcción
                if (previewBuildingType != null && previewColor != null) {
                    renderQueue.add(new Renderable(
                            screenX,
                            centerY,
                            centerY + 1,
                            new BuildingPreview(previewTileX, previewTileY, previewColor),
                            null,
                            false,
                            false,
                            null
                    ));
                }
                // Mostrar highlight normal si no estamos construyendo
                else {
                    renderQueue.add(new Renderable(
                            screenX,
                            centerY,
                            centerY + 1,
                            new TileHighlight(screenX, centerY, tileWidth, tileHeight),
                            null,
                            false,
                            false,
                            null
                    ));
                }
            }
        }

        // Add player to render queue
        Player player = gameState.getPlayer();
        if (player != null) {
            double[] playerCords = calculatePlayerPosition(player, halfTileWidth, halfTileHeight, offsetX, offsetY, alpha);
            final Image playerSprite = spriteManager.getEntitySprite(
                    "player", player.getAnimationFrame(), player.getDirection()
            );
            if (playerSprite != null) {
                final double playerDepth = playerCords[1] + (config.graphics().getTileHalfHeight() * zoom * 1.5);
                renderQueue.add(new Renderable(
                        playerCords[0], playerCords[1], playerDepth,
                        player, playerSprite, false, false, null
                ));
            }
        }

        // Add enemies to render queue
        List<Enemy> enemies = gameState.getEnemies();
        if (enemies != null) {
            for (Enemy enemy : enemies) {
                if (enemy.isDead()) continue;

                double[] enemyPos = enemy.getInterpolatedPosition(alpha);
                double enemyInterpX = enemyPos[0];
                double enemyInterpY = enemyPos[1];

                double enemyScreenX = (enemyInterpX - enemyInterpY) * halfTileWidth + offsetX;
                double enemyScreenY = (enemyInterpX + enemyInterpY) * halfTileHeight + offsetY;
                double enemyDrawDepth = enemyScreenY + (halfTileHeight * 1.5);

                renderQueue.add(new Renderable(
                        enemyScreenX, enemyScreenY, enemyDrawDepth,
                        enemy, null, false, false, null
                ));
            }
        }

        // Add dropped items to render queue
        List<DroppedItem> droppedItems = gameState.getDroppedItems();
        if (droppedItems != null) {
            for (DroppedItem droppedItem : droppedItems) {
                double itemScreenX = (droppedItem.getTileX() - droppedItem.getTileY()) * halfTileWidth + offsetX;
                double itemScreenY = (droppedItem.getTileX() + droppedItem.getTileY()) * halfTileHeight + offsetY;
                double itemDrawDepth = itemScreenY + (halfTileHeight * 2);

                renderQueue.add(new Renderable(
                        itemScreenX, itemScreenY, itemDrawDepth,
                        droppedItem, null, false, false, null
                ));
            }
        }

        // Depth sort and render
        renderQueue.sort(Comparator.comparingDouble(Renderable::drawDepth));
        gc.save();
        renderQueue.forEach(this::drawRenderable);
        if (player != null) {
            renderPlayerAttack(gc, player, halfTileWidth, halfTileHeight, offsetX, offsetY);
        }
        gc.restore();

        // Draw debug info
        FxDebugDrawingHelper.drawDebugInfo(gc, gameState, canvas, deltaTime);
    }

    // Helper methods --------------------------------------------------------

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
     * Calculates interpolated player position during movement for smooth rendering.
     *
     * @param player        The player entity.
     * @param halfTileWidth Half the width of a zoomed tile.
     * @param halfTileHeight Half the height of a zoomed tile.
     * @param offsetX       Camera X offset.
     * @param offsetY       Camera Y offset.
     * @param alpha         Interpolation factor
     * @return An array containing [screenX, screenY] of the player's position.
     */
    private double[] calculatePlayerPosition(Player player, double halfTileWidth, double halfTileHeight,
                                             double offsetX, double offsetY, double alpha) {
        double[] pos = player.getInterpolatedPosition(alpha); // Get interpolated tile coordinates
        double x = pos[0]; // Interpolated tile X
        double y = pos[1]; // Interpolated tile Y

        // Convert interpolated tile coordinates to screen coordinates
        return new double[] {
                (x - y) * halfTileWidth + offsetX,
                (x + y) * halfTileHeight + offsetY
        };
    }

    /**
     * Dispatches the drawing call based on the type of the renderable object.
     *
     * @param r The Renderable object to draw.
     */
    private void drawRenderable(Renderable r) {
        if (r.object() instanceof Tile tile) {
            drawTile(tile, r);
        } else if (r.object() instanceof ResourceType) {
            drawResource(r);
        } else if (r.object() instanceof Building building) {
            drawBuilding(building);
        } else if (r.object() instanceof Player) {
            drawPlayer(r);
        } else if (r.object() instanceof Enemy) {
            drawEnemy(r);
        } else if (r.object() instanceof DroppedItem) {
            drawDroppedItem(r);
        } else if (r.object() instanceof TileHighlight h) {
            drawHighlightTile(h);
        } else if (r.object() instanceof BuildingPreview bp) {
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
        // TODO: Add other enemy types here
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
            ColorAdjust flashEffect = new ColorAdjust();
            flashEffect.setBrightness(0.7);
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
            ColorAdjust flashEffect = new ColorAdjust();
            flashEffect.setBrightness(0.7);
            gc.setEffect(flashEffect);
        }

        FxTileDrawingHelper.drawResourceSprite(
                gc, r.screenX(), r.screenY(),
                camera.getZoom(), r.sprite()
        );
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
        // Convertir coordenadas de tile a pantalla
        double screenX = (x - y) * halfTileWidth + offsetX;
        double screenY = (x + y) * halfTileHeight + offsetY;
        // Dibujar efecto de ataque (círculo pulsante)
        gc.setFill(Color.rgb(255, 0, 0, 0.5 * (1 - progress)));
        gc.fillOval(
                screenX - 10 * progress,
                screenY - 10 * progress,
                20 * progress,
                20 * progress
        );
    }

}
