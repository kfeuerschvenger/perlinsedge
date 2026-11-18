package com.feuerschvenger.perlinsedge.infra.fx.graphics;

import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.domain.utils.ClickFeedback;
import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Building;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Fence;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.FenceConnections;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Slime;
import com.feuerschvenger.perlinsedge.domain.entities.items.DroppedItem;
import com.feuerschvenger.perlinsedge.domain.world.model.*;
import com.feuerschvenger.perlinsedge.domain.world.time.DayCycle;
import com.feuerschvenger.perlinsedge.domain.world.time.SunScreenMapping;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.*;
import com.feuerschvenger.perlinsedge.infra.fx.utils.Silhouette;
import com.feuerschvenger.perlinsedge.infra.fx.utils.SilhouetteSprites;
import com.feuerschvenger.perlinsedge.domain.utils.Rgba;
import com.feuerschvenger.perlinsedge.infra.fx.utils.AssetPaths;
import com.feuerschvenger.perlinsedge.infra.fx.utils.BuildingSprites;
import com.feuerschvenger.perlinsedge.infra.fx.utils.CursorStyle;
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

    /** Height in art pixels of the tallest sprite that casts a shadow, which bounds how far a shadow can reach. */
    static final double MAX_CASTER_ART_HEIGHT = 48.0;

    /** Half the width in art pixels of the widest sprite that casts a shadow. */
    static final double MAX_CASTER_ART_HALF_WIDTH = 16.0;

    /** Width in art pixels the shadow is sized from for a player drawn without art. */
    private static final double FALLBACK_PLAYER_ART_WIDTH = 12.0;

    /** Depth of the health bar of a building, far past every object so nothing is drawn over it. */
    private static final double HEALTH_BAR_DEPTH = 1.0e9;
    /** Height in art pixels the bar floats above a procedural building, which has no sprite to measure. */
    private static final double PROCEDURAL_BUILDING_HEIGHT_ART = 24.0;

    /** The resting crosshair, dark over light ground and light over dark ground, each over an outline of the other. */
    private static final Color CURSOR_DARK = Color.rgb(40, 40, 40);
    private static final Color CURSOR_LIGHT = Color.rgb(235, 235, 235);
    private static final Color CURSOR_DARK_OUTLINE = Color.rgb(0, 0, 0, 0.6);
    private static final Color CURSOR_LIGHT_OUTLINE = Color.rgb(255, 255, 255, 0.6);
    private static final double CURSOR_LINE_WIDTH = 2.0;

    /** The answer to a click order: green when it was accepted, red when it was refused. */
    private static final Color CURSOR_ACCEPTED = Color.rgb(60, 220, 90);
    private static final Color CURSOR_REJECTED = Color.rgb(235, 60, 60);
    private static final double FLASH_LINE_WIDTH = 3.0;

    private final GraphicsContext gc;
    private final Canvas canvas;
    private final FxCamera camera;
    private final SpriteManager spriteManager;
    private final BuildingSprites buildingSprites = BuildingSprites.getInstance();
    private final GameStateManager gameState;
    /** The tiles of a frame, drawn first and in the order they were queued. */
    private final RenderQueue tileQueue = new RenderQueue();
    /** The depth sorted objects of a frame: resources, buildings, entities, items and the tile cursor. */
    private final RenderQueue renderQueue = new RenderQueue();
    /**
     * The shadows of the resources and buildings of the frame, collected while the tiles are walked. They stand still,
     * so they are kept in world coordinates, the screen position with the camera offset taken off, and a list stays the
     * same while the camera pans. The list is rebuilt, with a walk of the wide ring of tiles that reaches the whole
     * static window, only when something it was built from changed, see {@link #staticCastersStale}; every other frame
     * keeps it and walks the visible tiles alone.
     */
    private final ShadowList staticShadows = new ShadowList();
    /** Whether {@link #staticShadows} holds a list that was built from the state remembered in the fields below. */
    private boolean staticCastersValid;
    /** Changes whenever {@link #staticShadows} does, so the shadow pass tells a changed list without comparing it. */
    private long staticShadowVersion;
    /** The map the static list was built from. */
    private TileMap staticCastersMap;
    /** The revision of that map when the list was built. */
    private long staticCastersRevision;
    /** The zoom the static list was built at, which scales the world coordinates of its shadows. */
    private double staticCastersZoom;
    /** The integer art scale the static list was built at. */
    private double staticCastersArtScale;
    /** The reach above and beside the window the static list was built with. */
    private double staticCastersReach;
    /** The reach below the window the static list was built with. */
    private double staticCastersBottomReach;
    /** The window of the world the static list covers as left, top, width and height, in world coordinates. */
    private final double[] staticCastersWindow = new double[4];
    /** Whether the grey height view was on when the static list was built, which casts nothing. */
    private boolean staticCastersGrayscale;
    /** The map type the static list was built for, which picks the art the shadows follow. */
    private MapType staticCastersType;
    /** How many times the static list was rebuilt, which a frame that kept it does not count. */
    private int staticListRebuilds;
    /** The shadows of the player and the enemies of the frame, which move and are kept apart from the static ones. */
    private final ShadowList dynamicShadows = new ShadowList();
    /** Casts the shadows of a frame into two layers, only what changed, and lays them over the canvas once. */
    private final FxShadowPass shadowPass = new FxShadowPass();
    /** Lays the tint of the time of day over the world. */
    private final FxSkyTint skyTint = new FxSkyTint();
    private final ColumnPainter columns;
    /** The values of the frame being drawn, written at the start of every frame. */
    private final FrameState frame = new FrameState();
    /** The palm or cactus choice of the desert trees, worked out once per tree instead of once per frame. */
    private final ResourceKindCache resourceKinds = new ResourceKindCache();
    private final FrameStats frameStats = new FrameStats();
    private final ColorAdjust flashEffect = createFlashEffect();
    private double currentFrameGameTime;
    /** How far above or beside the canvas, in pixels, a caster can stand and still cast a shadow into it this frame. */
    private double shadowReach;
    /** How far below the canvas, in pixels, a caster can stand and still cast a shadow into it this frame. */
    private double shadowBottomReach;
    /** Screen X of the left edge of the window of the world the static shadows are cast into this frame. */
    private double shadowWindowLeft;
    /** Screen Y of the top edge of the window of the world the static shadows are cast into this frame. */
    private double shadowWindowTop;
    /** Width in pixels of the window of the world the static shadows are cast into this frame. */
    private double shadowWindowWidth;
    /** Height in pixels of the window of the world the static shadows are cast into this frame. */
    private double shadowWindowHeight;
    /** The camera offset along X of the frame, which turns the screen position of a static shadow into a world one. */
    private double shadowCameraX;
    /** The camera offset along Y of the frame. */
    private double shadowCameraY;
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
    private int previewRotation;
    /** Whether the hammer is in hand, which is when the grid, the ghost and the health bar of a hovered building show. */
    private boolean buildModeActive;
    private Rgba previewRgba;
    private Color previewColor;
    private BuildingPreview cachedPreview;
    private TileHighlight cachedHighlight;
    private TileHighlight cachedFlashHighlight;

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

    /**
     * Turns on what is drawn while the hammer is in hand: the faint grid around the hovered tile, and the ghost of the
     * selected piece on it. Call it every frame of the build mode and {@link #clearBuildingPreview()} when it ends.
     *
     * @param tileX        the hovered tile X, -1 for none
     * @param tileY        the hovered tile Y, -1 for none
     * @param buildingType the selected piece, or null for none, which leaves the grid without a ghost
     * @param color        the tint of the ghost: green when the piece can be placed, red when the tile refuses it,
     *                     amber when the resources are missing
     */
    public void setBuildingPreview(int tileX, int tileY, BuildingType buildingType, Rgba color) {
        setBuildingPreview(tileX, tileY, buildingType, 0, color);
    }

    /**
     * Turns on what is drawn while the hammer is in hand, with the ghost facing a given way.
     *
     * @param tileX        the hovered tile X, -1 for none
     * @param tileY        the hovered tile Y, -1 for none
     * @param buildingType the selected piece, or null for none, which leaves the grid without a ghost
     * @param rotation     the way the ghost faces, see {@link BuildingType#rotationCount()}
     * @param color        the tint of the ghost, see {@link #setBuildingPreview(int, int, BuildingType, Rgba)}
     */
    public void setBuildingPreview(int tileX, int tileY, BuildingType buildingType, int rotation, Rgba color) {
        this.previewRotation = rotation;
        this.buildModeActive = true;
        this.previewTileX = tileX;
        this.previewTileY = tileY;
        this.previewBuildingType = buildingType;
        if (color == null) {
            this.previewRgba = null;
            this.previewColor = null;
        } else if (previewRgba == null || !previewRgba.equals(color)) {
            this.previewRgba = color;
            this.previewColor = FxColors.toFx(color);
        }
    }

    /** Drops what the renderer remembers about the current world, so a discarded map is not kept alive by it. */
    public void invalidateWorldCaches() {
        resourceKinds.invalidate();
        staticCastersValid = false;
        staticCastersMap = null;
    }

    /** Ends the build mode drawing: no grid, no ghost, no health bar, and the normal tile highlight is drawn again. */
    public void clearBuildingPreview() {
        buildModeActive = false;
        previewTileX = -1;
        previewTileY = -1;
        previewBuildingType = null;
        previewRgba = null;
        previewColor = null;
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
        dynamicShadows.clear();
        frameStats.recordFrame(deltaTime);
        this.currentFrameGameTime = gameState.getGameTime();
        // The light of the held torch is placed where the sprite of the player is drawn this frame, and is in the
        // registry only while a torch is the selected item, which can change between two updates
        gameState.syncLights(alpha);
        this.waterCycles = WaterShimmer.cycles(currentFrameGameTime);
        final DayCycle sky = gameState.getDayCycle();
        final FrameState f = frame;
        prepareFrame(f, sky);
        prepareShadows(f);

        walkTiles(f);
        if (f.rebuildStatic) {
            rememberStaticCasters(f.map, f.zoom, f.artScale, f.grayscaleEnabled, f.mapType);
        }

        queueHover(f);
        queuePlayer(f, alpha);
        queueEnemies(f, alpha);
        queueDroppedItems(f);
        drawFrame(f, sky);

        if (f.player != null) {
            FxHurtGlow.draw(gc, canvas.getWidth(), canvas.getHeight(), HurtReaction.glowOpacity(f.player.getSecondsSinceHurt()));
        }

        // Draw debug info
        FxDebugDrawingHelper.drawDebugInfo(gc, gameState, canvas, frameStats, tileQueue.size() + renderQueue.size());
    }

    /**
     * The values of one frame that the steps of {@link #render} share. One instance is kept and its fields are
     * written at the start of every frame, so the steps do not take them one by one and the frame allocates no
     * holder for them.
     */
    private static final class FrameState {
        /** The map drawn this frame. */
        TileMap map;
        /** The camera zoom. */
        double zoom;
        /** The camera offset along X. */
        double offsetX;
        /** The camera offset along Y. */
        double offsetY;
        /** The zoomed width of a tile. */
        double tileWidth;
        /** The zoomed height of a tile. */
        double tileHeight;
        /** Half the zoomed width of a tile. */
        double halfTileWidth;
        /** Half the zoomed height of a tile. */
        double halfTileHeight;
        /** The integer art scale, which the shadow and light passes and the sprites are drawn at. */
        double artScale;
        /** The seed of the world. */
        long worldSeed;
        /** The map type, which picks the biome art. */
        MapType mapType;
        /** Whether the grey height view was asked for. */
        boolean grayscaleEnabled;
        /** Whether the game is paused, which stops the hit effects of the resources. */
        boolean paused;
        /** Whether the shadows are strong enough to be cast. */
        boolean castShadows;
        /** Whether the static shadow list is rebuilt this frame. */
        boolean rebuildStatic;
        /** The first visible tile column. */
        int minTileX;
        /** The column after the last visible tile column. */
        int maxTileX;
        /** The first visible tile row. */
        int minTileY;
        /** The row after the last visible tile row. */
        int maxTileY;
        /** How far the near ring of the static walk reaches, in screen pixels. */
        double overhang;
        /** The horizontal overhang of the static window, in screen pixels. */
        double overhangX;
        /** The vertical overhang of the static window, in screen pixels. */
        double overhangY;
        /** The tile column under the mouse, -1 for none. */
        int mouseTileX;
        /** The tile row under the mouse, -1 for none. */
        int mouseTileY;
        /** The player, or null when there is none. */
        Player player;
        /** The canvas as the shadow and light passes see it. */
        CanvasView view;
        /** The camera as the building painter sees it. */
        CameraView cameraView;
        /** The tile projection the damage numbers are placed with. */
        FxDamageNumbersPainter.TileProjection projection;
        /** The direction, length and strength of the shadows. */
        SunShadow sun;
    }

    /** Reads what the frame needs from the camera, the configuration and the game state into the frame values. */
    private void prepareFrame(FrameState f, DayCycle sky) {
        f.paused = gameState.isGamePaused();
        // Nothing is cast, and nothing is read from the sprites, while the shadows are too faint to see
        final double shadowStrength = sky.shadowStrength();
        f.castShadows = shadowStrength >= FxShadowHelper.MIN_VISIBLE_STRENGTH;
        // The sun of the frame, read once and shared by the reach of the casters and the shadow pass
        final double shadowLengthFactor = sky.shadowLengthFactor();
        f.sun = new SunShadow(SunScreenMapping.shadowAngle(sky.shadowSunProgress()), shadowLengthFactor,
                shadowStrength);
        f.map = gameState.getCurrentMap();
        f.zoom = camera.getZoom();
        f.offsetX = camera.getX();
        f.offsetY = camera.getY();
        final AppConfig config = AppConfig.getInstance();
        f.mapType = config.world().getCurrentMapType();
        this.activeMapType = f.mapType;
        f.grayscaleEnabled = config.debug().isGrayscaleEnabled();
        // The height view is a debug tool: with debug off, grayscale only hides the art and the procedural tiles show
        this.grayscaleView = f.grayscaleEnabled && config.debug().isEnabled();
        this.debugTextEnabled = grayscaleView && config.debug().isTextEnabled();

        // Tile dimensions calculations
        f.tileWidth = config.graphics().getTileWidth() * f.zoom;
        f.tileHeight = config.graphics().getTileHeight() * f.zoom;
        f.halfTileWidth = config.graphics().getTileHalfWidth() * f.zoom;
        f.halfTileHeight = config.graphics().getTileHalfHeight() * f.zoom;

        // Get visible tile bounds
        final int[] bounds = camera.getVisibleTileBounds(canvas.getWidth(), canvas.getHeight(), f.map.getWidth(),
                f.map.getHeight());
        f.minTileX = bounds[0];
        f.maxTileX = bounds[1];
        f.minTileY = bounds[2];
        f.maxTileY = bounds[3];

        f.worldSeed = gameState.getWorldSeed();
        // The art scale is snapped to a whole number once here, and the shadow and light passes take this value, so the
        // grids they cast on and the integer scale they draw at can never disagree
        f.artScale = ZoomSteps.integerArtScale(f.zoom);
        f.view = new CanvasView(f.artScale, f.offsetX, f.offsetY, canvas.getWidth(), canvas.getHeight());
        f.cameraView = new CameraView(f.offsetX, f.offsetY, f.zoom);
        f.projection = new FxDamageNumbersPainter.TileProjection(f.halfTileWidth, f.halfTileHeight, f.offsetX,
                f.offsetY, f.zoom);
        f.mouseTileX = gameState.getMouseOverTileX();
        f.mouseTileY = gameState.getMouseOverTileY();
        f.player = gameState.getPlayer();
    }

    /**
     * Works out how far the shadows reach this frame and where the window of the static shadows lies, and decides
     * whether the static list is rebuilt.
     */
    private void prepareShadows(FrameState f) {
        // An object outside the view still casts a shadow into it when the shadow is long enough to reach, so the
        // walk covers a wider ring of tiles, and only the casting is done for the tiles outside the view
        shadowReach = f.castShadows ? FxShadowHelper.reachMargin(MAX_CASTER_ART_HEIGHT, MAX_CASTER_ART_HALF_WIDTH,
                f.sun.length(), f.artScale) : 0.0;
        // Shadows fall down the screen, so a caster below the view reaches into it far less than one above or beside it,
        // and the ring is thinner on the sides of the tile bounds that face the bottom of the screen
        shadowBottomReach = f.castShadows ? FxShadowHelper.bottomMargin(MAX_CASTER_ART_HALF_WIDTH, f.artScale) : 0.0;
        // The static shadows are cast into a window of the world that reaches past the canvas, and the walk covers the
        // casters that reach into the whole window, so panning the camera inside the window changes none of them
        f.overhang = 0.0;
        f.overhangX = 0.0;
        f.overhangY = 0.0;
        if (f.castShadows) {
            shadowPass.beginFrame(f.view);
            shadowWindowLeft = shadowPass.windowLeft();
            shadowWindowTop = shadowPass.windowTop();
            shadowWindowWidth = shadowPass.windowWidth();
            shadowWindowHeight = shadowPass.windowHeight();
            // The near ring is as wide as the larger overhang takes tiles, the far ring adds the two overhangs up. A ring
            // of tiles n wide reaches n half tile widths to the sides and n half tile heights up, so the
            // sideways overhang counts for the tiles it takes to span it on the screen
            f.overhangX = shadowPass.windowOverhangXPixels();
            f.overhangY = shadowPass.windowOverhangYPixels();
            f.overhang = Math.max(f.overhangY, f.overhangX * f.halfTileHeight / f.halfTileWidth);
        }
        // The static shadows are given relative to the whole pixel camera offset the pass snaps to, which the camera
        // already is, and the pass adds it back to round them on the screen the way the sprites are
        shadowCameraX = f.castShadows ? shadowPass.snappedOffsetX() : f.offsetX;
        shadowCameraY = f.castShadows ? shadowPass.snappedOffsetY() : f.offsetY;
        // The static list is rebuilt, with the wide walk, only when it no longer matches what it was built from
        f.rebuildStatic = f.castShadows && staticCastersStale(f.map, f.zoom, f.artScale, f.grayscaleEnabled,
                f.mapType);
        if (f.rebuildStatic || !f.castShadows) {
            staticShadowVersion++;
            staticShadows.clear();
        }
        if (!f.castShadows) {
            staticCastersValid = false;
        }
    }

    /** Walks the tiles of the view, and the wider ring that casts shadows into it, and queues what each one draws. */
    private void walkTiles(FrameState f) {
        final int nearTiles = f.rebuildStatic
                ? FxShadowHelper.nearRingTiles(shadowReach + f.overhang, f.halfTileHeight) : 0;
        final int farTiles = f.rebuildStatic
                ? FxShadowHelper.farRingTiles(shadowReach, shadowBottomReach, f.overhangX, f.overhangY,
                f.halfTileWidth, f.halfTileHeight) : 0;
        final int walkMinX = Math.max(0, f.minTileX - nearTiles);
        final int walkMaxX = Math.min(f.map.getWidth(), f.maxTileX + farTiles);
        final int walkMinY = Math.max(0, f.minTileY - nearTiles);
        final int walkMaxY = Math.min(f.map.getHeight(), f.maxTileY + farTiles);
        for (int y = walkMinY; y < walkMaxY; y++) {
            for (int x = walkMinX; x < walkMaxX; x++) {
                final Tile tile = f.map.getTile(x, y);
                if (tile != null) {
                    visitTile(f, tile, x, y);
                }
            }
        }
    }

    /** Queues a tile of the walk when it is in view, and casts its shadows when it is out of view but reaches in. */
    private void visitTile(FrameState f, Tile tile, int x, int y) {
        final boolean insideView = x >= f.minTileX && x < f.maxTileX && y >= f.minTileY && y < f.maxTileY;
        final boolean castsStatic = f.rebuildStatic && !f.grayscaleEnabled;
        // A tile outside the view only matters for the shadow of what stands on it, so one with nothing on it
        // is skipped before any screen maths is done for it
        if (!insideView && !(castsStatic && castsShadow(tile))) {
            return;
        }
        // Calculate screen coordinates
        final double screenX = (x - y) * f.halfTileWidth + f.offsetX;
        final double screenY = (x + y) * f.halfTileHeight + f.offsetY;

        // Frustum culling: a tile that is not drawn can still cast a shadow into the view
        if (!insideView || !isTileVisible(screenX, screenY, f.tileWidth, f.tileHeight)) {
            if (castsStatic) {
                castOffscreenShadows(f, tile, x, y, screenX, screenY);
            }
            return;
        }
        queueVisibleTile(f, tile, x, y, screenX, screenY);
    }

    /** Queues a tile in view, the floor on it, and the resource and the building that stand on it. */
    private void queueVisibleTile(FrameState f, Tile tile, int x, int y, double screenX, double screenY) {
        // Tiles with art are drawn from their sprite, the others from the procedural shape
        final Image tileSprite = tileSpriteFor(f, tile, x, y);

        // A water tile with art is drawn as its sunk bed and its sheet from the land top, the one without
        // art is a flat stand-in lowered by hand
        final double topY = screenY + FxObjectShadows.waterDrop(!f.grayscaleEnabled && tile.getType().isWater(),
                tileSprite != null, f.halfTileHeight);
        tileQueue.add(screenX, topY, topY + f.halfTileHeight, tile, tileSprite, false, tile);
        if (f.grayscaleEnabled) {
            return;
        }
        // The floor, a bridge, lies on the ground right after its tile, under the shadows and the objects
        final Building floor = tile.getFloor();
        if (floor != null) {
            tileQueue.add(screenX, topY, topY + f.halfTileHeight, floor, buildingSprites.imageFor(floor, 0), false,
                    tile);
        }
        queueResource(f, tile, x, y, screenX, topY);
        queueBuilding(f, tile, screenX, topY);
    }

    /** Returns the sprite a tile is drawn from, null when it is drawn from the procedural shape or hidden. */
    private Image tileSpriteFor(FrameState f, Tile tile, int x, int y) {
        if (!f.grayscaleEnabled) {
            return spriteManager.getTileSprite(f.mapType, tile.getType(), x, y);
        }
        return grayscaleView ? DebugTileSprites.get(GrayscaleTiles.levelFor(tile.getHeight())) : null;
    }

    /** Queues the resource of a tile, if it has one, and adds its shadow to the static list when that is rebuilt. */
    private void queueResource(FrameState f, Tile tile, int x, int y, double screenX, double screenY) {
        final Resource resource = tile.getWorldResource();
        if (!tile.hasResource() || resource == null) {
            return;
        }
        // The hit effects run on the game clock, which stands still while paused: the frame drawn for the
        // pause menu shows the resource at rest instead of a frozen white flash or a frozen sideways shake
        final boolean isFlashing = !f.paused && resource.isFlashing(currentFrameGameTime);
        // A desert tree is a palm near water and a cactus in the dry sand; the look-around only runs for it
        final String preferredKind = resourceKinds.preferredKind(f.map, f.mapType, resource.getType(), x, y,
                f.worldSeed);
        final SpriteManager.SpriteMetadata spriteMetadata = spriteManager.getResourceSprite(
                f.mapType, resource.getType(), x, y, f.worldSeed, preferredKind
        );
        // A resource blocks movement whether or not its image exists, so it is always queued;
        // without an image the painter draws a stand-in shape.
        final Image resourceImage = spriteMetadata != null ? spriteMetadata.getImage() : null;
        // A struck resource shakes sideways for a moment, its shadow stays on the ground
        final double secondsSinceHit = f.paused
                ? Double.POSITIVE_INFINITY : resource.secondsSinceHit(currentFrameGameTime);
        final double shakeX = HitShake.screenOffset(secondsSinceHit, f.artScale);
        // A low sprite is drawn lower so its foot touches the ground, and its shadow follows that contact point
        final double drop = spriteMetadata != null ? spriteMetadata.getDropArtPixels() * f.artScale : 0.0;
        renderQueue.add(screenX + shakeX, screenY + drop, screenY + f.tileHeight * 0.5,
                resource.getType(), resourceImage, isFlashing, tile);
        if (f.rebuildStatic) {
            FxObjectShadows.addForTile(staticShadows, resource.getType(), resourceImage,
                    spriteMetadata != null ? spriteMetadata.getContactArtPixels() : 0, null,
                    staticAnchor(f, screenX, screenY));
        }
    }

    /** Queues the building of a tile, if it has one, and adds its shadow to the static list when that is rebuilt. */
    private void queueBuilding(FrameState f, Tile tile, double screenX, double screenY) {
        final Building building = tile.getBuilding();
        if (!tile.hasBuilding() || building == null) {
            return;
        }
        // A piece with art is drawn from its sprite, in its present state and frame, the others from
        // their procedural shape
        final Image buildingArt = buildingSprites.imageFor(building,
                BuildingSprites.flameFrame(currentFrameGameTime));
        renderQueue.add(screenX, screenY, screenY + f.halfTileHeight, building, buildingArt, false, tile);
        if (f.rebuildStatic) {
            FxObjectShadows.addForTile(staticShadows, null, null, 0, building.getType(),
                    buildingSprites.shadowImages(building), staticAnchor(f, screenX, screenY));
        }
    }

    /** Returns where a tile stands in the world coordinates the static shadows are kept in. */
    private FxObjectShadows.TileAnchor staticAnchor(FrameState f, double screenX, double screenY) {
        return new FxObjectShadows.TileAnchor(screenX - shadowCameraX, screenY - shadowCameraY, f.halfTileHeight,
                f.artScale);
    }

    /**
     * Queues what the hovered tile shows, the ghost or the crosshair and the health bar, and the answer to a click
     * where it was given.
     */
    private void queueHover(FrameState f) {
        // The tile of the last click order flashes green or red, on the hovered tile or wherever the click was
        final ClickFeedback feedback = gameState.getClickFeedback();
        final ClickFeedback.Kind flashKind = feedback.kind(currentFrameGameTime);
        final boolean flashOnHover = flashKind != ClickFeedback.Kind.NONE
                && feedback.getTileX() == f.mouseTileX && feedback.getTileY() == f.mouseTileY;

        if (f.mouseTileX != -1 && f.mouseTileY != -1) {
            final Tile highlightTile = f.map.getTile(f.mouseTileX, f.mouseTileY);
            if (highlightTile != null) {
                queueHoveredTile(f, highlightTile, flashKind, flashOnHover);
            }
        }
        if (flashKind != ClickFeedback.Kind.NONE && !flashOnHover) {
            final Tile flashTile = f.map.getTile(feedback.getTileX(), feedback.getTileY());
            if (flashTile != null) {
                queueCursor(f, flashTile, feedback.getTileX(), feedback.getTileY(), flashKind, true);
            }
        }
    }

    /** Queues the ghost or the crosshair of the hovered tile, and the health bar of the building on it. */
    private void queueHoveredTile(FrameState f, Tile tile, ClickFeedback.Kind flashKind, boolean flashOnHover) {
        // The ghost of the selected piece stands on the hovered tile while building, the crosshair otherwise.
        // The ghost is a piece, so it is depth sorted where a building of that tile would be
        if (buildModeActive && previewBuildingType != null && previewColor != null) {
            queueGhost(f, tile);
            // The answer to a click still shows on the tile, over the ghost
            if (flashOnHover) {
                queueCursor(f, tile, f.mouseTileX, f.mouseTileY, flashKind, false);
            }
        } else {
            queueCursor(f, tile, f.mouseTileX, f.mouseTileY, flashOnHover ? flashKind : ClickFeedback.Kind.NONE,
                    false);
        }
        queueHealthBar(tile, f.artScale, f.zoom);
    }

    /** Queues the ghost of the selected piece on the hovered tile. */
    private void queueGhost(FrameState f, Tile tile) {
        final double screenX = (f.mouseTileX - f.mouseTileY) * f.halfTileWidth + f.offsetX;
        final double topY = (f.mouseTileX + f.mouseTileY) * f.halfTileHeight + f.offsetY
                + FxObjectShadows.waterDrop(tile.getType().isWater(),
                spriteManager.getTileSprite(f.mapType, tile.getType(), f.mouseTileX, f.mouseTileY) != null,
                f.halfTileHeight);
        renderQueue.add(screenX, topY, topY + f.halfTileHeight + 0.5, previewFor(), null, false, null);
    }

    /** Adds the player to the render queue, and the shadow of the player to the dynamic list. */
    private void queuePlayer(FrameState f, double alpha) {
        final Player player = f.player;
        if (player == null) {
            return;
        }
        final double interpolatedX = player.getInterpolatedX(alpha);
        final double interpolatedY = player.getInterpolatedY(alpha);
        final double playerX = (interpolatedX - interpolatedY) * f.halfTileWidth + f.offsetX;
        final double playerY = (interpolatedX + interpolatedY) * f.halfTileHeight + f.offsetY;
        // A missing sheet leaves the sprite null and the player is drawn as the stand-in circle
        final Image playerSprite = spriteManager.getEntitySprite(
                "player", player.getAnimationFrame(), SpriteLayout.playerColumn(player.getDirection())
        );
        final double playerDepth = playerY + (f.halfTileHeight * 1.5);
        renderQueue.add(playerX, playerY, playerDepth, player, playerSprite, false, null);
        // The sprite stands on the tile centre
        final double playerFeetY = playerY + f.halfTileHeight;
        if (f.castShadows && FxShadowHelper.canReach(playerX, playerFeetY, canvas.getWidth(), canvas.getHeight(),
                shadowReach, shadowBottomReach)) {
            addPlayerShadow(playerX, playerFeetY, playerSprite);
        }
    }

    /** Adds the shadow of the player, cast from its sprite or an ellipse when it is drawn without art. */
    private void addPlayerShadow(double playerX, double playerFeetY, Image playerSprite) {
        if (playerSprite != null) {
            final Silhouette silhouette = SilhouetteSprites.of(playerSprite);
            dynamicShadows.addSilhouette(playerX, playerFeetY, silhouette, silhouette.height(), 1.0, 1.0);
        } else {
            dynamicShadows.add(playerX, playerFeetY, FALLBACK_PLAYER_ART_WIDTH * GroundShadow.PLAYER_FRACTION, 1.0);
        }
    }

    /** Adds the enemies that are alive to the render queue, and their shadows to the dynamic list. */
    private void queueEnemies(FrameState f, double alpha) {
        final List<Enemy> enemies = gameState.getEnemies();
        if (enemies == null) {
            return;
        }
        for (int i = 0, count = enemies.size(); i < count; i++) {
            final Enemy enemy = enemies.get(i);
            if (!enemy.isDead()) {
                queueEnemy(f, enemy, alpha);
            }
        }
    }

    /** Adds one enemy to the render queue, and its shadow to the dynamic list when it reaches into the view. */
    private void queueEnemy(FrameState f, Enemy enemy, double alpha) {
        final double enemyInterpX = enemy.getInterpolatedX(alpha);
        final double enemyInterpY = enemy.getInterpolatedY(alpha);

        final double enemyScreenX = (enemyInterpX - enemyInterpY) * f.halfTileWidth + f.offsetX;
        final double enemyScreenY = (enemyInterpX + enemyInterpY) * f.halfTileHeight + f.offsetY;
        final double enemyDrawDepth = enemyScreenY + (f.halfTileHeight * 1.5);

        renderQueue.add(enemyScreenX, enemyScreenY, enemyDrawDepth, enemy, null, false, null);
        if (!f.castShadows || !FxShadowHelper.canReach(enemyScreenX, enemyScreenY, canvas.getWidth(),
                canvas.getHeight(), shadowReach, shadowBottomReach)) {
            return;
        }
        if (enemy instanceof Slime slime) {
            FxEnemyDrawingHelper.addSlimeShadow(dynamicShadows, enemyScreenX, enemyScreenY, f.zoom, slime,
                    currentFrameGameTime);
        } else {
            FxEnemyDrawingHelper.addGenericShadow(dynamicShadows, enemyScreenX, enemyScreenY, f.zoom);
        }
    }

    /** Adds the dropped items to the render queue. */
    private void queueDroppedItems(FrameState f) {
        final List<DroppedItem> droppedItems = gameState.getDroppedItems();
        if (droppedItems == null) {
            return;
        }
        for (int i = 0, count = droppedItems.size(); i < count; i++) {
            final DroppedItem droppedItem = droppedItems.get(i);
            // A thrown item slides from the thrower to its landing tile, a landed one rests on its tile
            final double itemTileX = droppedItem.flightX(currentFrameGameTime);
            final double itemTileY = droppedItem.flightY(currentFrameGameTime);
            final double itemScreenX = (itemTileX - itemTileY) * f.halfTileWidth + f.offsetX;
            final double itemScreenY = (itemTileX + itemTileY) * f.halfTileHeight + f.offsetY;
            final double itemDrawDepth = itemScreenY + (f.halfTileHeight * 2);

            renderQueue.add(itemScreenX, itemScreenY, itemDrawDepth, droppedItem, null, false, null);
        }
    }

    /** Draws the queued frame: the ground, the shadows on it, the depth sorted objects and the sky tint over all. */
    private void drawFrame(FrameState f, DayCycle sky) {
        // Ground first: every tile and the water, then the shadows lying on them, then the depth sorted objects.
        // Tiles are queued row by row, which already is back to front for blocks of one size.
        gc.save();
        for (int i = 0, count = tileQueue.size(); i < count; i++) {
            drawRenderable(tileQueue.get(i));
        }
        // The grid of the build mode lies on the ground, under the shadows and the objects
        if (buildModeActive && f.mouseTileX != -1 && f.mouseTileY != -1) {
            FxBuildGridPainter.draw(gc, f.map, f.mouseTileX, f.mouseTileY, f.offsetX, f.offsetY, f.zoom);
        }
        shadowPass.draw(gc, staticShadows, staticShadowVersion, dynamicShadows, f.view, f.sun);
        renderQueue.sortByDepth();
        for (int i = 0, count = renderQueue.size(); i < count; i++) {
            drawRenderable(renderQueue.get(i));
        }
        if (f.player != null) {
            renderPlayerAttack(gc, f.player, f.halfTileWidth, f.halfTileHeight, f.offsetX, f.offsetY);
        }
        // The sky tint covers the whole world, tiles, shadows and objects alike, and is pushed back around the lights
        // by the same pass, in the slot the plain tint always had. It is laid on before the damage
        // numbers, which are readable text and stay untinted, and the hurt glow, the debug text and the interface
        // panels come after it or are not part of the canvas at all.
        skyTint.draw(gc, sky, gameState.getLights(), currentFrameGameTime, f.view);
        FxDamageNumbersPainter.draw(gc, gameState.getDamageNumbers(), currentFrameGameTime, f.projection,
                canvas.getWidth(), canvas.getHeight());
        gc.restore();
    }

    // Helper methods --------------------------------------------------------

    /**
     * Returns how many times the static shadow list was rebuilt with the wide walk so far. A frame that kept the list
     * does not count.
     *
     * @return the number of rebuilds
     */
    public int staticListRebuilds() {
        return staticListRebuilds;
    }

    /**
     * Gives the window of the world the static shadows of the last frame were cast into.
     *
     * @return left, top, width and height in screen pixels of that frame
     */
    double[] staticWindow() {
        return new double[]{shadowWindowLeft, shadowWindowTop, shadowWindowWidth, shadowWindowHeight};
    }

    /** @return how many static shadows the list of the last frame holds */
    int staticShadowCount() {
        return staticShadows.size();
    }

    /**
     * Tells whether the static shadow list has to be built again. It holds the shadows of the casters that reach into
     * the static window, in world coordinates, so it stays right while the camera pans inside the window and changes
     * with the window itself, with the reach of the shadows, which follows the sun and the art scale, with the zoom
     * that scales the world coordinates, with the map type and the grey view, which decide what is cast, and with the
     * map: another map, or one whose tiles changed since the list was built.
     */
    private boolean staticCastersStale(TileMap map, double zoom, double artScale, boolean grayscale, MapType type) {
        return !staticCastersValid || staticCastersMap != map || staticCastersRevision != map.getRevision()
                || staticCastersZoom != zoom || staticCastersArtScale != artScale
                || staticCastersReach != shadowReach || staticCastersBottomReach != shadowBottomReach
                || staticCastersWindow[0] != shadowWindowLeft - shadowCameraX
                || staticCastersWindow[1] != shadowWindowTop - shadowCameraY
                || staticCastersWindow[2] != shadowWindowWidth || staticCastersWindow[3] != shadowWindowHeight
                || staticCastersGrayscale != grayscale || staticCastersType != type;
    }

    /** Remembers what the static shadow list was just built from, see {@link #staticCastersStale}. */
    private void rememberStaticCasters(TileMap map, double zoom, double artScale, boolean grayscale, MapType type) {
        staticCastersValid = true;
        staticCastersMap = map;
        staticCastersRevision = map.getRevision();
        staticCastersZoom = zoom;
        staticCastersArtScale = artScale;
        staticCastersReach = shadowReach;
        staticCastersBottomReach = shadowBottomReach;
        staticCastersWindow[0] = shadowWindowLeft - shadowCameraX;
        staticCastersWindow[1] = shadowWindowTop - shadowCameraY;
        staticCastersWindow[2] = shadowWindowWidth;
        staticCastersWindow[3] = shadowWindowHeight;
        staticCastersGrayscale = grayscale;
        staticCastersType = type;
        staticListRebuilds++;
    }

    /**
     * Casts the shadows of a tile that is not drawn, when they are long enough to reach into the view. Only the
     * shadow is added, so nothing outside the view is queued for drawing. It goes through the same
     * {@link FxObjectShadows} calls as a tile in the view, including the drop of a water tile with no art, so an
     * object throws its shadow from the same point either way.
     */
    private void castOffscreenShadows(FrameState f, Tile tile, int x, int y, double screenX, double screenY) {
        final Resource resource = tile.getWorldResource();
        final boolean hasResource = tile.hasResource() && resource != null;
        final Building building = tile.getBuilding();
        final boolean hasBuilding = tile.hasBuilding() && building != null;
        if (!hasResource && !hasBuilding) {
            return;
        }
        // A static shadow is wanted when it reaches into the window the static layer covers, not only the canvas
        if (!FxShadowHelper.canReach(screenX - shadowWindowLeft, screenY + f.halfTileHeight - shadowWindowTop,
                shadowWindowWidth, shadowWindowHeight, shadowReach, shadowBottomReach)) {
            return;
        }
        final boolean water = tile.getType().isWater();
        final double drop = FxObjectShadows.waterDrop(water,
                water && spriteManager.getTileSprite(f.mapType, tile.getType(), x, y) != null, f.halfTileHeight);
        Image image = null;
        int contactArt = 0;
        if (hasResource) {
            final String preferredKind = resourceKinds.preferredKind(f.map, f.mapType, resource.getType(), x, y,
                    f.worldSeed);
            final SpriteManager.SpriteMetadata metadata = spriteManager.getResourceSprite(
                    f.mapType, resource.getType(), x, y, f.worldSeed, preferredKind);
            image = metadata != null ? metadata.getImage() : null;
            contactArt = metadata != null ? metadata.getContactArtPixels() : 0;
        }
        FxObjectShadows.addForTile(staticShadows, hasResource ? resource.getType() : null, image, contactArt,
                hasBuilding ? building.getType() : null,
                hasBuilding ? buildingSprites.shadowImages(building) : null,
                staticAnchor(f, screenX, screenY + drop));
    }

    /** Tells whether something stands on a tile that casts a shadow: a resource or a building. */
    private static boolean castsShadow(Tile tile) {
        return (tile.hasResource() && tile.getWorldResource() != null)
                || (tile.hasBuilding() && tile.getBuilding() != null);
    }

    /** The ghost of the building being placed, recreated only when the tile, the piece or the tint changes. */
    private BuildingPreview previewFor() {
        final TileMap map = gameState.getCurrentMap();
        final int connections = previewBuildingType == BuildingType.FENCE && map != null
                ? FenceConnections.maskAt(map, previewTileX, previewTileY) : 0;
        BuildingPreview preview = cachedPreview;
        if (preview == null || preview.tileX() != previewTileX || preview.tileY() != previewTileY
                || preview.type() != previewBuildingType || preview.rotation() != previewRotation
                || preview.connections() != connections || preview.color() != previewColor) {
            preview = new BuildingPreview(previewTileX, previewTileY, previewBuildingType, previewRotation,
                    connections, previewRgba, previewColor);
            cachedPreview = preview;
        }
        return preview;
    }

    /**
     * Queues the health bar over the building of the hovered tile, drawn after everything else so nothing covers it,
     * when the hammer is in hand and the building lost some health.
     */
    private void queueHealthBar(Tile tile, double artScale, double zoom) {
        final Building building = tile.hasBuilding() ? tile.getBuilding() : tile.getFloor();
        if (!FxBuildingHealthBar.isShown(buildModeActive, building)) {
            return;
        }
        final AppConfig.GraphicsConfig graphics = AppConfig.getInstance().graphics();
        final double halfTileWidth = graphics.getTileHalfWidth() * zoom;
        final double halfTileHeight = graphics.getTileHalfHeight() * zoom;
        final int x = building.getTileX();
        final int y = building.getTileY();
        final double centerX = (x - y) * halfTileWidth + camera.getX();
        final double topVertexY = (x + y) * halfTileHeight + camera.getY()
                + FxObjectShadows.waterDrop(tile.getType().isWater(),
                spriteManager.getTileSprite(activeMapType, tile.getType(), x, y) != null, halfTileHeight);
        final double centerY = topVertexY + halfTileHeight;
        final Image art = buildingSprites.imageFor(building, 0);
        final double buildingTopY = art != null
                ? BuildingSprites.drawY(centerY, (int) art.getHeight(), artScale)
                : centerY - PROCEDURAL_BUILDING_HEIGHT_ART * zoom;
        renderQueue.add(centerX, topVertexY, HEALTH_BAR_DEPTH,
                new HealthBarMark(centerX, buildingTopY, building.getHealthFraction()), null, false, null);
    }

    /**
     * Y of the centre of the top of a tile as the player sees it.
     * A water tile shows its sheet below the land tops, so the cursor sits on the sheet.
     */
    private double cursorCenterY(Tile tile, int tileX, int tileY, double zoom, double offsetY) {
        final double halfTileHeight = AppConfig.getInstance().graphics().getTileHalfHeight() * zoom;
        final double sheetDrop = tile.getType().isWater()
                ? TileDepths.SURFACE_PIXELS * ZoomSteps.integerArtScale(zoom) : 0;
        return (tileX + tileY) * halfTileHeight + offsetY + halfTileHeight + sheetDrop;
    }

    /** Queues the crosshair of a tile, dark or light by the ground under it. */
    private void queueCursor(FrameState f, Tile tile, int tileX, int tileY, ClickFeedback.Kind tone,
                             boolean flashSlot) {
        final AppConfig.GraphicsConfig graphics = AppConfig.getInstance().graphics();
        final double screenX = (tileX - tileY) * graphics.getTileHalfWidth() * f.zoom + f.offsetX;
        final double centerY = cursorCenterY(tile, tileX, tileY, f.zoom, f.offsetY);
        final boolean light = CursorStyle.isLightGround(tile.getType(),
                AssetPaths.tileArt(activeMapType, tile.getType()));
        renderQueue.add(screenX, centerY, centerY + 1,
                highlightFor(flashSlot, screenX, centerY, graphics.getTileWidth() * f.zoom,
                        graphics.getTileHeight() * f.zoom, light, tone), null, false, null);
    }

    /** The tile highlight, recreated only when the tile, the zoom, the ground or the answer to a click changes it. */
    private TileHighlight highlightFor(boolean flashSlot, double centerX, double centerY, double tileWidth,
                                       double tileHeight, boolean lightGround, ClickFeedback.Kind tone) {
        TileHighlight highlight = flashSlot ? cachedFlashHighlight : cachedHighlight;
        if (highlight == null || highlight.centerX() != centerX || highlight.centerY() != centerY
                || highlight.tileWidth() != tileWidth || highlight.tileHeight() != tileHeight
                || highlight.lightGround() != lightGround || highlight.tone() != tone) {
            highlight = new TileHighlight(centerX, centerY, tileWidth, tileHeight, lightGround, tone);
            if (flashSlot) {
                cachedFlashHighlight = highlight;
            } else {
                cachedHighlight = highlight;
            }
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
                canvas.getWidth(), canvas.getHeight(), ZoomSteps.integerArtScale(camera.getZoom()));
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
            drawBuilding(building, r);
        } else if (object instanceof Player) {
            drawPlayer(r);
        } else if (object instanceof Enemy) {
            drawEnemy(r);
        } else if (object instanceof DroppedItem) {
            drawDroppedItem(r);
        } else if (object instanceof TileHighlight h) {
            drawHighlightTile(h);
        } else if (object instanceof BuildingPreview bp) {
            drawBuildingGhost(bp, r);
        } else if (object instanceof HealthBarMark mark) {
            FxBuildingHealthBar.draw(gc, mark, ZoomSteps.integerArtScale(camera.getZoom()));
        }
    }

    private static Image[] singleton(Image image) {
        return image != null ? new Image[]{image} : null;
    }

    /** Draws the ghost of the selected piece on the hovered tile, from its sprite when it has one. */
    private void drawBuildingGhost(BuildingPreview preview, Renderable r) {
        final double zoom = camera.getZoom();
        final Image[] art = preview.type() == BuildingType.FENCE
                ? buildingSprites.fenceLayers(preview.connections())
                : singleton(buildingSprites.restingImage(preview.type(), preview.rotation()));
        FxBuildingDrawingHelper.drawGhost(gc, preview, art, r.screenX(),
                r.screenY() + AppConfig.getInstance().graphics().getTileHalfHeight() * zoom, zoom);
    }

    /**
     * Draws a building from its sprite when it has one, from its procedural shape otherwise.
     *
     * @param building The building to draw.
     * @param r        The queued entry, which holds the tile position on screen and the sprite
     */
    private void drawBuilding(Building building, Renderable r) {
        final double zoom = camera.getZoom();
        final double centerY = r.screenY() + AppConfig.getInstance().graphics().getTileHalfHeight() * zoom;
        if (building instanceof Fence fence && r.sprite() != null) {
            final Image[] layers = buildingSprites.fenceLayers(fence.getConnections());
            if (layers.length > 0) {
                FxBuildingDrawingHelper.drawLayers(gc, layers, r.screenX(), centerY, zoom);
                return;
            }
        }
        FxBuildingDrawingHelper.drawBuilding(gc, building, r.sprite(), r.screenX(), centerY, frame.cameraView);
    }

    /**
     * Draws highlight lines for the tile currently under the mouse.
     *
     * @param highlight The TileHighlight object containing the highlight information.
     */
    private void drawHighlightTile(TileHighlight highlight) {
        final Color line;
        final Color outline;
        final double lineWidth;
        final double alpha;
        switch (highlight.tone()) {
            case ACCEPTED, REJECTED -> {
                line = highlight.tone() == ClickFeedback.Kind.ACCEPTED ? CURSOR_ACCEPTED : CURSOR_REJECTED;
                outline = CURSOR_DARK_OUTLINE;
                lineWidth = FLASH_LINE_WIDTH;
                alpha = CursorStyle.flashAlpha(gameState.getClickFeedback().strength(currentFrameGameTime));
            }
            default -> {
                // The crosshair is dark over light ground and light over dark ground, each with an outline of the
                // opposite brightness, and it breathes slowly on the game clock
                line = highlight.lightGround() ? CURSOR_DARK : CURSOR_LIGHT;
                outline = highlight.lightGround() ? CURSOR_LIGHT_OUTLINE : CURSOR_DARK_OUTLINE;
                lineWidth = CURSOR_LINE_WIDTH;
                alpha = CursorStyle.breathAlpha(currentFrameGameTime);
            }
        }
        FxTileDrawingHelper.drawTileHighlight(gc, highlight, camera.getZoom(), line, outline, lineWidth, alpha);
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
        final double shakeX = HurtReaction.shakeScreenOffset(sinceHurt, ZoomSteps.integerArtScale(camera.getZoom()));
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

        if (enemy instanceof Slime slime) {
            FxEnemyDrawingHelper.drawSlime(gc, r.screenX(), r.screenY(), zoom, slime, currentFrameGameTime);
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

        columns.drawBed(map, tile, r.sprite(), r.screenX(), r.screenY(), zoom, seed);

        final double surfaceY = r.screenY() + TileDepths.SURFACE_PIXELS * ZoomSteps.integerArtScale(zoom);
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
