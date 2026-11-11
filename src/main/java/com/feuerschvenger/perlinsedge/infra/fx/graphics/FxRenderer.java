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
import com.feuerschvenger.perlinsedge.infra.fx.utils.AssetPaths;
import com.feuerschvenger.perlinsedge.infra.fx.utils.DebugTileSprites;
import com.feuerschvenger.perlinsedge.infra.fx.utils.FlatTileSprites;
import com.feuerschvenger.perlinsedge.infra.fx.utils.GrayscaleTiles;
import com.feuerschvenger.perlinsedge.infra.fx.utils.FxColors;
import com.feuerschvenger.perlinsedge.infra.fx.utils.GroundDecals;
import com.feuerschvenger.perlinsedge.infra.fx.utils.GroundShadow;
import com.feuerschvenger.perlinsedge.infra.fx.utils.HitShake;
import com.feuerschvenger.perlinsedge.infra.fx.utils.HurtReaction;
import com.feuerschvenger.perlinsedge.infra.fx.utils.TintedSprites;
import com.feuerschvenger.perlinsedge.infra.fx.utils.ResourceKindCache;
import com.feuerschvenger.perlinsedge.infra.fx.utils.SpriteLayout;
import com.feuerschvenger.perlinsedge.infra.fx.utils.SpriteManager;
import com.feuerschvenger.perlinsedge.infra.fx.utils.WaterShimmer;
import com.feuerschvenger.perlinsedge.infra.fx.utils.TileDepths;
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

    /** Shimmer lines fainter than this are skipped, since they are not visible. */
    private static final double MIN_SHIMMER_ALPHA = 0.02;

    /** Width in art pixels the shadow is sized from for a resource drawn without art. */
    private static final double FALLBACK_RESOURCE_ART_WIDTH = 12.0;

    /** Width in art pixels the shadow is sized from for a player drawn without art. */
    private static final double FALLBACK_PLAYER_ART_WIDTH = 12.0;

    private final GraphicsContext gc;
    private final Canvas canvas;
    private final FxCamera camera;
    private final SpriteManager spriteManager;
    private final GameStateManager gameState;
    /** The tiles of a frame, drawn first and in the order they were queued. */
    private final RenderQueue tileQueue = new RenderQueue();
    /** The depth sorted objects of a frame: resources, buildings, entities, items and the tile cursor. */
    private final RenderQueue renderQueue = new RenderQueue();
    private final ShadowList shadows = new ShadowList();
    private final ColumnPainter columns;
    /** The palm or cactus choice of the desert trees, worked out once per tree instead of once per frame. */
    private final ResourceKindCache resourceKinds = new ResourceKindCache();
    private final FrameStats frameStats = new FrameStats();
    private final ColorAdjust flashEffect = createFlashEffect();
    private double currentFrameGameTime;
    /** The map type of the frame, which picks the biome art of the ground and its decals. */
    private MapType activeMapType;
    /** The shimmer animation clock of the frame, computed once from the game time. */
    private double waterCycles;
    /** Whether the frame draws the grey height view, read once per frame from the debug settings. */
    private boolean grayscaleView;
    private boolean debugTextEnabled;
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
        this.columns = new ColumnPainter(gc, spriteManager);
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

    /** Drops what the renderer remembers about the current world, so a discarded map is not kept alive by it. */
    public void invalidateWorldCaches() {
        resourceKinds.invalidate();
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
        gc.setImageSmoothing(false);
        renderQueue.clear();
        tileQueue.clear();
        shadows.clear();
        frameStats.record(deltaTime);
        this.currentFrameGameTime = gameState.getGameTime();
        this.waterCycles = WaterShimmer.cycles(currentFrameGameTime);
        final boolean paused = gameState.isGamePaused();

        TileMap map = gameState.getCurrentMap();
        final double zoom = camera.getZoom();
        final double offsetX = camera.getX();
        final double offsetY = camera.getY();
        final AppConfig config = AppConfig.getInstance();
        final MapType currentMapType = config.world().getCurrentMapType();
        this.activeMapType = currentMapType;
        final boolean isGrayscaleEnabled = config.debug().isGrayscaleEnabled();
        // The height view is a debug tool: with debug off, grayscale only hides the art and the procedural tiles show
        this.grayscaleView = isGrayscaleEnabled && config.debug().isEnabled();
        this.debugTextEnabled = grayscaleView && config.debug().isTextEnabled();

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

        final long worldSeed = gameState.getWorldSeed();

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

                // Tiles with art are drawn from their sprite, the others from the procedural shape
                final Image tileSprite = isGrayscaleEnabled
                        ? (grayscaleView ? DebugTileSprites.get(GrayscaleTiles.levelFor(tile.getHeight())) : null)
                        : spriteManager.getTileSprite(currentMapType, tile.getType(), x, y);

                // A water tile with art is drawn as its sunk bed and its sheet from the land top, the one without
                // art is a flat stand-in lowered by hand
                if (!isGrayscaleEnabled && tile.getType().isWater() && tileSprite == null) {
                    screenY += waterDepthOffset;
                }
                tileQueue.add(screenX, screenY, screenY + halfTileHeight, tile, tileSprite, false, tile);

                // Add resource if present
                final Resource resource = tile.getWorldResource();
                if (tile.hasResource() && !isGrayscaleEnabled && resource != null) {
                    // The hit effects run on the game clock, which stands still while paused: the frame drawn for the
                    // pause menu shows the resource at rest instead of a frozen white flash or a frozen sideways shake
                    boolean isFlashing = !paused && resource.isFlashing(currentFrameGameTime);
                    // A desert tree is a palm near water and a cactus in the dry sand; the look-around only runs for it
                    final String preferredKind = resourceKinds.preferredKind(map, currentMapType, resource.getType(),
                            x, y, worldSeed);
                    SpriteManager.SpriteMetadata spriteMetadata = spriteManager.getResourceSprite(
                            currentMapType, resource.getType(), x, y, worldSeed, preferredKind
                    );
                    // A resource blocks movement whether or not its image exists, so it is always queued;
                    // without an image the painter draws a stand-in shape.
                    Image resourceImage = spriteMetadata != null ? spriteMetadata.getImage() : null;
                    // A struck resource shakes sideways for a moment, its shadow stays on the ground
                    final double artScale = ZoomSteps.artScale(zoom);
                    final double secondsSinceHit = paused
                            ? Double.POSITIVE_INFINITY : resource.secondsSinceHit(currentFrameGameTime);
                    final double shakeX = HitShake.screenOffset(secondsSinceHit, artScale);
                    // A low sprite is drawn lower so its foot touches the ground, and its shadow follows that contact point
                    final double drop = spriteMetadata != null ? spriteMetadata.getDropArtPixels() * artScale : 0.0;
                    final double contact = spriteMetadata != null ? spriteMetadata.getContactArtPixels() * artScale : 0.0;
                    renderQueue.add(screenX + shakeX, screenY + drop, screenY + tileHeight * 0.5,
                            resource.getType(), resourceImage, isFlashing, tile);
                    shadows.add(screenX, screenY + halfTileHeight + contact,
                            GroundShadow.widthFor(resource.getType(),
                                    resourceImage != null ? resourceImage.getWidth() : FALLBACK_RESOURCE_ART_WIDTH),
                            1.0);
                }
                // Add building if present
                final Building building = tile.getBuilding();
                if (tile.hasBuilding() && !isGrayscaleEnabled && building != null) {
                    renderQueue.add(screenX, screenY, screenY + halfTileHeight, building, null, false, tile);
                    final double buildingShadow = GroundShadow.widthFor(building.getType());
                    if (buildingShadow > 0) {
                        shadows.add(screenX, screenY + halfTileHeight, buildingShadow, 1.0);
                    }
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
            // A missing sheet leaves the sprite null and the player is drawn as the stand-in circle
            final Image playerSprite = spriteManager.getEntitySprite(
                    "player", player.getAnimationFrame(), SpriteLayout.playerColumn(player.getDirection())
            );
            final double playerDepth = playerY + (config.graphics().getTileHalfHeight() * zoom * 1.5);
            renderQueue.add(playerX, playerY, playerDepth, player, playerSprite, false, null);
            // The sprite stands on the tile centre
            final double playerFeetY = playerY + halfTileHeight;
            if (FxShadowHelper.isOnScreen(playerX, playerFeetY, canvas.getWidth(), canvas.getHeight())) {
                final double playerWidth = playerSprite != null ? playerSprite.getWidth() : FALLBACK_PLAYER_ART_WIDTH;
                shadows.add(playerX, playerFeetY, playerWidth * GroundShadow.PLAYER_FRACTION, 1.0);
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
                if (FxShadowHelper.isOnScreen(enemyScreenX, enemyScreenY, canvas.getWidth(), canvas.getHeight())) {
                    if (enemy instanceof Slime slime) {
                        FxEnemyDrawingHelper.addSlimeShadow(shadows, enemyScreenX, enemyScreenY, zoom, slime);
                    } else {
                        FxEnemyDrawingHelper.addGenericShadow(shadows, enemyScreenX, enemyScreenY, zoom);
                    }
                }
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

        // Ground first: every tile and the water, then the shadows lying on them, then the depth sorted objects.
        // Tiles are queued row by row, which already is back to front for blocks of one size.
        gc.save();
        for (int i = 0, count = tileQueue.size(); i < count; i++) {
            drawRenderable(tileQueue.get(i));
        }
        FxShadowHelper.draw(gc, shadows, zoom);
        renderQueue.sortByDepth();
        for (int i = 0, count = renderQueue.size(); i < count; i++) {
            drawRenderable(renderQueue.get(i));
        }
        if (player != null) {
            renderPlayerAttack(gc, player, halfTileWidth, halfTileHeight, offsetX, offsetY);
        }
        gc.restore();

        if (player != null) {
            FxHurtGlow.draw(gc, canvas.getWidth(), canvas.getHeight(), HurtReaction.glowOpacity(player.getSecondsSinceHurt()));
        }

        // Draw debug info
        FxDebugDrawingHelper.drawDebugInfo(gc, gameState, canvas, deltaTime);
        if (config.debug().isFpsOverlayEnabled()) {
            FxDebugDrawingHelper.drawFpsOverlay(gc, frameStats, tileQueue.size() + renderQueue.size());
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
        return ScreenCulling.isTileVisible(screenX, screenY, tileWidth, tileHeight,
                canvas.getWidth(), canvas.getHeight(), ZoomSteps.artScale(camera.getZoom()));
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
        // A hit flashes the sprite red and white and shakes it sideways for a moment
        final double sinceHurt = player.getSecondsSinceHurt();
        final int flash = HurtReaction.flashPhase(sinceHurt);
        final Image sprite = r.sprite() != null && flash != HurtReaction.NO_FLASH
                ? TintedSprites.of(r.sprite(), flash) : r.sprite();
        final double shakeX = HurtReaction.shakeScreenOffset(sinceHurt, ZoomSteps.artScale(camera.getZoom()));
        FxPlayerDrawingHelper.drawPlayer(
                gc, r.screenX() + shakeX, r.screenY(),
                camera.getZoom(), player, sprite
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
        if (grayscaleView) {
            FxTileDrawingHelper.drawTileSprite(gc, r.sprite(), r.screenX(), r.screenY(), camera.getZoom());
            if (debugTextEnabled) {
                FxTileDrawingHelper.drawDebugTileText(gc, r.screenX(), r.screenY(), camera.getZoom(), tile);
            }
            return;
        }

        int saveCount = 0;
        if (r.isFlashing()) {
            gc.save();
            saveCount++;
            gc.setEffect(flashEffect);
        }

        if (r.sprite() != null) {
            if (tile.getType().isWater()) {
                drawWater(tile, r);
            } else {
                FxTileDrawingHelper.drawTileSprite(gc, r.sprite(), r.screenX(), r.screenY(), camera.getZoom());
                columns.drawExposedFaces(gameState.getCurrentMap(), tile, AssetPaths.tileArt(activeMapType, tile.getType()),
                        r.screenX(), r.screenY(), camera.getZoom());
                drawGroundDecal(tile, r);
            }
        } else {
            FxTileDrawingHelper.drawTileSprite(gc, FlatTileSprites.get(tile.getType()), r.screenX(), r.screenY(), camera.getZoom());
        }
        if (saveCount > 0) {
            gc.restore();
        }
    }

    /**
     * Draws a water tile as a column: its sunk sand bed with the faces its deeper neighbours expose and the decal on
     * it, then the one translucent water sheet half a step below the land tops, the shimmer lines and the lily pad.
     * The sheet is drawn right after its own bed, so a land tile drawn later in front of the water covers it, and the
     * sprite of the sheet is narrower than a land diamond so neighbouring sheets never overlap. Every value comes from
     * pure functions of the tile, the world seed and the animation clock computed once per frame, and the sprites are
     * shared, so nothing is allocated.
     */
    private void drawWater(Tile tile, Renderable r) {
        final int x = tile.getX();
        final int y = tile.getY();
        final long seed = gameState.getWorldSeed();
        final double zoom = camera.getZoom();
        final TileMap map = gameState.getCurrentMap();

        columns.drawBed(map, tile, r.sprite(), AssetPaths.tileArt(tile.getType()), r.screenX(), r.screenY(), zoom, seed);

        final double surfaceY = r.screenY() + TileDepths.SURFACE_PIXELS * ZoomSteps.artScale(zoom);
        final Image sheet = spriteManager.getWaterSprite(AssetPaths.waterArt(tile.getType()));
        if (sheet != null) {
            FxTileDrawingHelper.drawTileSprite(gc, sheet, r.screenX(), surfaceY, zoom);
        }

        final int lines = WaterShimmer.lineCount(x, y);
        for (int line = 0; line < lines; line++) {
            final double alpha = WaterShimmer.alpha(waterCycles, x, y, line);
            final Image shimmer = spriteManager.getShimmerSprite(line);
            if (alpha < MIN_SHIMMER_ALPHA || shimmer == null) {
                continue;
            }
            gc.setGlobalAlpha(alpha);
            FxTileDrawingHelper.drawDecal(gc, shimmer, r.screenX(), surfaceY, zoom,
                    WaterShimmer.offsetU(waterCycles, x, y, line), WaterShimmer.offsetV(waterCycles, x, y, line));
        }
        gc.setGlobalAlpha(1.0);

        final Image lilyPad = spriteManager.getDecalSprite(GroundDecals.lilyPadFor(map, tile.getType(), x, y, seed));
        if (lilyPad != null) {
            FxTileDrawingHelper.drawDecal(gc, lilyPad, r.screenX(), surfaceY, zoom,
                    GroundDecals.offsetU(x, y, seed), GroundDecals.offsetV(x, y, seed));
        }
    }

    /**
     * Draws the decal of a tile on its top face, when the tile has one and holds no resource or building.
     * The choice and the offset are pure functions of the tile and the world seed, so nothing is allocated.
     */
    private void drawGroundDecal(Tile tile, Renderable r) {
        if (!tile.isBuildable()) {
            return;
        }
        final long seed = gameState.getWorldSeed();
        final Image decal = spriteManager.getDecalSprite(GroundDecals.decalFor(activeMapType, tile.getType(), tile.getX(), tile.getY(), seed));
        if (decal == null) {
            return;
        }
        FxTileDrawingHelper.drawDecal(gc, decal, r.screenX(), r.screenY(), camera.getZoom(),
                GroundDecals.offsetU(tile.getX(), tile.getY(), seed), GroundDecals.offsetV(tile.getX(), tile.getY(), seed));
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
