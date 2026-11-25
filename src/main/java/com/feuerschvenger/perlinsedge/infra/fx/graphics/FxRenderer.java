package com.feuerschvenger.perlinsedge.infra.fx.graphics;

import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Building;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.FenceConnections;
import com.feuerschvenger.perlinsedge.domain.entities.creatures.Creature;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Slime;
import com.feuerschvenger.perlinsedge.domain.utils.ClickFeedback;
import com.feuerschvenger.perlinsedge.domain.utils.Doubles;
import com.feuerschvenger.perlinsedge.domain.utils.Rgba;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.Resource;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.time.DayCycle;
import com.feuerschvenger.perlinsedge.domain.world.time.SunScreenMapping;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.BuildingPreview;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.CameraView;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.CanvasView;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.FxBuildGridPainter;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.FxBuildingHealthBar;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.FxDamageNumbersPainter;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.FxDebugDrawingHelper;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.FxEnemyDrawingHelper;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.FxHurtGlow;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.FxObjectShadows;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.FxShadowHelper;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.FxShadowPass;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.FxSkyTint;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.HealthBarMark;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.SunShadow;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.TileHighlight;
import com.feuerschvenger.perlinsedge.infra.fx.utils.AssetPaths;
import com.feuerschvenger.perlinsedge.infra.fx.utils.BuildingSprites;
import com.feuerschvenger.perlinsedge.infra.fx.utils.CursorStyle;
import com.feuerschvenger.perlinsedge.infra.fx.utils.DebugTileSprites;
import com.feuerschvenger.perlinsedge.infra.fx.utils.FxColors;
import com.feuerschvenger.perlinsedge.infra.fx.utils.GrayscaleTiles;
import com.feuerschvenger.perlinsedge.infra.fx.utils.GroundShadow;
import com.feuerschvenger.perlinsedge.infra.fx.utils.HitShake;
import com.feuerschvenger.perlinsedge.infra.fx.utils.HurtReaction;
import com.feuerschvenger.perlinsedge.infra.fx.utils.ResourceKindCache;
import com.feuerschvenger.perlinsedge.infra.fx.utils.SilhouetteSprites;
import com.feuerschvenger.perlinsedge.infra.fx.utils.SpriteLayout;
import com.feuerschvenger.perlinsedge.infra.fx.utils.SpriteManager;
import com.feuerschvenger.perlinsedge.infra.fx.utils.TileDepths;
import com.feuerschvenger.perlinsedge.infra.fx.utils.WaterShimmer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.Objects;

/**
 * Renders an isometric tile map with frustum culling optimization.
 * Handles tiles, resources, entities, and buildings with depth sorting.
 * The drawables of a frame live in a {@link RenderQueue} whose instances are recycled, and the flash effect and the
 * tile highlight are kept between frames, so a steady frame allocates almost nothing.
 */
public class FxRenderer {

    /** Height in art pixels of the tallest sprite that casts a shadow, which bounds how far a shadow can reach. */
    public static final double MAX_CASTER_ART_HEIGHT = 48.0;

    /** Half the width in art pixels of the widest sprite that casts a shadow. */
    public static final double MAX_CASTER_ART_HALF_WIDTH = 16.0;

    /** Width in art pixels the shadow is sized from for a player drawn without art. */
    private static final double FALLBACK_PLAYER_ART_WIDTH = 12.0;

    /** Depth of the health bar of a building, far past every object so nothing is drawn over it. */
    private static final double HEALTH_BAR_DEPTH = 1.0e9;
    /** Height in art pixels the bar floats above a procedural building, which has no sprite to measure. */
    private static final double PROCEDURAL_BUILDING_HEIGHT_ART = 24.0;

    /** Number of values that describe the window of the static casters: left, top, width and height. */
    private static final int WINDOW_VALUES = 4;
    /** Position of the left edge in the window of the static casters. */
    private static final int WINDOW_LEFT = 0;
    /** Position of the top edge in the window of the static casters. */
    private static final int WINDOW_TOP = 1;
    /** Position of the width in the window of the static casters. */
    private static final int WINDOW_WIDTH = 2;
    /** Position of the height in the window of the static casters. */
    private static final int WINDOW_HEIGHT = 3;

    /** Position of the first visible column in the visible tile bounds the camera gives. */
    private static final int BOUNDS_MIN_X = 0;
    /** Position of the last visible column in the visible tile bounds. */
    private static final int BOUNDS_MAX_X = 1;
    /** Position of the first visible row in the visible tile bounds. */
    private static final int BOUNDS_MIN_Y = 2;
    /** Position of the last visible row in the visible tile bounds. */
    private static final int BOUNDS_MAX_Y = 3;

    /** Share of the height of a tile below its top vertex at which a resource is depth sorted. */
    private static final double RESOURCE_DEPTH_SHARE = 0.5;
    /** How far past the tile the ghost is depth sorted, so it stands over the ground of its tile. */
    private static final double GHOST_DEPTH_NUDGE = 0.5;

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
    private final double[] staticCastersWindow = new double[WINDOW_VALUES];
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
    /** Draws what the queues hold. */
    private final FxRenderableDrawer drawer;
    /** The values of the frame being drawn, written at the start of every frame. */
    private final FrameState frame = new FrameState();
    /** The palm or cactus choice of the desert trees, worked out once per tree instead of once per frame. */
    private final ResourceKindCache resourceKinds = new ResourceKindCache();
    private final FrameStats frameStats = new FrameStats();
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
    /** Whether the frame draws the grey height view, read once per frame from the debug settings. */
    private boolean grayscaleView;
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
        this.drawer = new FxRenderableDrawer(gc, camera, gameState, spriteManager);
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
        final double waterCycles = WaterShimmer.cycles(currentFrameGameTime);
        final var sky = gameState.getDayCycle();
        final FrameState f = frame;
        prepareFrame(f, sky, waterCycles);
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
        private TileMap map;
        /** The camera zoom. */
        private double zoom;
        /** The camera offset along X. */
        private double offsetX;
        /** The camera offset along Y. */
        private double offsetY;
        /** The zoomed width of a tile. */
        private double tileWidth;
        /** The zoomed height of a tile. */
        private double tileHeight;
        /** Half the zoomed width of a tile. */
        private double halfTileWidth;
        /** Half the zoomed height of a tile. */
        private double halfTileHeight;
        /** The integer art scale, which the shadow and light passes and the sprites are drawn at. */
        private double artScale;
        /** The seed of the world. */
        private long worldSeed;
        /** The map type, which picks the biome art. */
        private MapType mapType;
        /** Whether the grey height view was asked for. */
        private boolean grayscaleEnabled;
        /** Whether the game is paused, which stops the hit effects of the resources. */
        private boolean paused;
        /** Whether the shadows are strong enough to be cast. */
        private boolean castShadows;
        /** Whether the static shadow list is rebuilt this frame. */
        private boolean rebuildStatic;
        /** The first visible tile column. */
        private int minTileX;
        /** The column after the last visible tile column. */
        private int maxTileX;
        /** The first visible tile row. */
        private int minTileY;
        /** The row after the last visible tile row. */
        private int maxTileY;
        /** How far the near ring of the static walk reaches, in screen pixels. */
        private double overhang;
        /** The horizontal overhang of the static window, in screen pixels. */
        private double overhangX;
        /** The vertical overhang of the static window, in screen pixels. */
        private double overhangY;
        /** The tile column under the mouse, -1 for none. */
        private int mouseTileX;
        /** The tile row under the mouse, -1 for none. */
        private int mouseTileY;
        /** The player, or null when there is none. */
        private Player player;
        /** The canvas as the shadow and light passes see it. */
        private CanvasView view;
        /** The camera as the building painter sees it. */
        private CameraView cameraView;
        /** The tile projection the damage numbers are placed with. */
        private FxDamageNumbersPainter.TileProjection projection;
        /** The direction, length and strength of the shadows. */
        private SunShadow sun;
    }

    /** Reads what the frame needs from the camera, the configuration and the game state into the frame values. */
    private void prepareFrame(FrameState f, DayCycle sky, double waterCycles) {
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
        final var config = AppConfig.getInstance();
        f.mapType = config.world().getCurrentMapType();
        this.activeMapType = f.mapType;
        f.grayscaleEnabled = config.debug().isGrayscaleEnabled();
        // The height view is a debug tool: with debug off, grayscale only hides the art and the procedural tiles show
        this.grayscaleView = f.grayscaleEnabled && config.debug().isEnabled();
        final boolean debugTextEnabled = grayscaleView && config.debug().isTextEnabled();

        // Tile dimensions calculations
        f.tileWidth = config.graphics().getTileWidth() * f.zoom;
        f.tileHeight = config.graphics().getTileHeight() * f.zoom;
        f.halfTileWidth = config.graphics().getTileHalfWidth() * f.zoom;
        f.halfTileHeight = config.graphics().getTileHalfHeight() * f.zoom;

        // Get visible tile bounds
        final int[] bounds = camera.getVisibleTileBounds(canvas.getWidth(), canvas.getHeight(), f.map.getWidth(),
                f.map.getHeight());
        f.minTileX = bounds[BOUNDS_MIN_X];
        f.maxTileX = bounds[BOUNDS_MAX_X];
        f.minTileY = bounds[BOUNDS_MIN_Y];
        f.maxTileY = bounds[BOUNDS_MAX_Y];

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
        drawer.beginFrame(f.mapType, grayscaleView, debugTextEnabled, waterCycles, currentFrameGameTime, f.cameraView);
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
        for (var y = walkMinY; y < walkMaxY; y++) {
            for (var x = walkMinX; x < walkMaxX; x++) {
                final var tile = f.map.getTile(x, y);
                if (tile != null) {
                    visitTile(f, tile, x, y);
                }
            }
        }
    }

    /** Queues a tile of the walk when it is in view, and casts its shadows when it is out of view but reaches in. */
    private void visitTile(FrameState f, Tile tile, int x, int y) {
        final boolean insideView = isInsideView(f, x, y);
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

    /** Tells whether a tile lies inside the tile window of the frame. */
    private static boolean isInsideView(FrameState f, int x, int y) {
        return x >= f.minTileX && x < f.maxTileX && y >= f.minTileY && y < f.maxTileY;
    }

    /** Queues a tile in view, the floor on it, and the resource and the building that stand on it. */
    private void queueVisibleTile(FrameState f, Tile tile, int x, int y, double screenX, double screenY) {
        // Tiles with art are drawn from their sprite, the others from the procedural shape
        final Image tileSprite = tileSpriteFor(f, tile);

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
    private Image tileSpriteFor(FrameState f, Tile tile) {
        if (!f.grayscaleEnabled) {
            return spriteManager.getTileSprite(f.mapType, tile);
        }
        return grayscaleView ? DebugTileSprites.get(GrayscaleTiles.levelFor(tile.getHeight())) : null;
    }

    /** Queues the resource of a tile, if it has one, and adds its shadow to the static list when that is rebuilt. */
    private void queueResource(FrameState f, Tile tile, int x, int y, double screenX, double screenY) {
        final var resource = tile.getWorldResource();
        if (!tile.hasResource() || resource == null) {
            return;
        }
        // The hit effects run on the game clock, which stands still while paused: the frame drawn for the
        // pause menu shows the resource at rest instead of a frozen white flash or a frozen sideways shake
        final boolean isFlashing = !f.paused && resource.isFlashing(currentFrameGameTime);
        // A desert tree is a palm near water and a cactus in the dry sand; the look-around only runs for it
        final String preferredKind = resourceKinds.preferredKind(f.map, f.mapType, resource.getType(), x, y,
                f.worldSeed);
        final var spriteMetadata = spriteManager.getResourceSprite(
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
        renderQueue.add(screenX + shakeX, screenY + drop, screenY + f.tileHeight * RESOURCE_DEPTH_SHARE,
                resource.getType(), resourceImage, isFlashing, tile);
        if (f.rebuildStatic) {
            FxObjectShadows.addForTile(staticShadows, resource.getType(), resourceImage,
                    spriteMetadata != null ? spriteMetadata.getContactArtPixels() : 0, null,
                    staticAnchor(f, screenX, screenY));
        }
    }

    /** Queues the building of a tile, if it has one, and adds its shadow to the static list when that is rebuilt. */
    private void queueBuilding(FrameState f, Tile tile, double screenX, double screenY) {
        final var building = tile.getBuilding();
        if (!tile.hasBuilding() || building == null) {
            return;
        }
        // A piece with art is drawn from its sprite, in its present state and frame, the others from
        // their procedural shape
        final var buildingArt = buildingSprites.imageFor(building,
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
        final var feedback = gameState.getClickFeedback();
        final var flashKind = feedback.kind(currentFrameGameTime);
        final boolean flashOnHover = flashKind != ClickFeedback.Kind.NONE
                && feedback.getTileX() == f.mouseTileX && feedback.getTileY() == f.mouseTileY;

        if (f.mouseTileX != -1 && f.mouseTileY != -1) {
            final var highlightTile = f.map.getTile(f.mouseTileX, f.mouseTileY);
            if (highlightTile != null) {
                queueHoveredTile(f, highlightTile, flashKind, flashOnHover);
            }
        }
        if (flashKind != ClickFeedback.Kind.NONE && !flashOnHover) {
            final var flashTile = f.map.getTile(feedback.getTileX(), feedback.getTileY());
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
                spriteManager.getTileSprite(f.mapType, tile) != null,
                f.halfTileHeight);
        renderQueue.add(screenX, topY, topY + f.halfTileHeight + GHOST_DEPTH_NUDGE, previewFor(), null, false, null);
    }

    /** Adds the player to the render queue, and the shadow of the player to the dynamic list. */
    private void queuePlayer(FrameState f, double alpha) {
        final var player = f.player;
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
            final var silhouette = SilhouetteSprites.of(playerSprite);
            dynamicShadows.addSilhouette(playerX, playerFeetY, silhouette, silhouette.height(), 1.0, 1.0);
        } else {
            dynamicShadows.add(playerX, playerFeetY, FALLBACK_PLAYER_ART_WIDTH * GroundShadow.PLAYER_FRACTION, 1.0);
        }
    }

    /** Adds the creatures that are alive to the render queue, and their shadows to the dynamic list. */
    private void queueEnemies(FrameState f, double alpha) {
        final var creatures = gameState.getCreatures();
        if (creatures == null) {
            return;
        }
        for (int i = 0, count = creatures.size(); i < count; i++) {
            final var creature = creatures.get(i);
            if (!creature.isDead()) {
                queueEnemy(f, creature, alpha);
            }
        }
    }

    /** Adds one creature to the render queue, and its shadow to the dynamic list when it reaches into the view. */
    private void queueEnemy(FrameState f, Creature enemy, double alpha) {
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
        final var droppedItems = gameState.getDroppedItems();
        if (droppedItems == null) {
            return;
        }
        for (int i = 0, count = droppedItems.size(); i < count; i++) {
            final var droppedItem = droppedItems.get(i);
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
            drawer.draw(tileQueue.get(i));
        }
        // The grid of the build mode lies on the ground, under the shadows and the objects
        if (buildModeActive && f.mouseTileX != -1 && f.mouseTileY != -1) {
            FxBuildGridPainter.draw(gc, f.map, f.mouseTileX, f.mouseTileY, f.offsetX, f.offsetY, f.zoom);
        }
        shadowPass.draw(gc, staticShadows, staticShadowVersion, dynamicShadows, f.view, f.sun);
        renderQueue.sortByDepth();
        for (int i = 0, count = renderQueue.size(); i < count; i++) {
            drawer.draw(renderQueue.get(i));
        }
        if (f.player != null) {
            drawer.drawPlayerAttack(f.player, f.halfTileWidth, f.halfTileHeight, f.offsetX, f.offsetY);
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
        if (!staticCastersValid || sourceChanged(map, grayscale, type) || scaleChanged(zoom, artScale)) {
            return true;
        }
        return reachChanged() || windowChanged();
    }

    /** Tells whether the list was built from another map, or from the same one as it was, or for another look. */
    private boolean sourceChanged(TileMap map, boolean grayscale, MapType type) {
        return staticCastersMap != map || staticCastersRevision != map.getRevision()
                || staticCastersGrayscale != grayscale || staticCastersType != type;
    }

    /** Tells whether the list was built at another zoom or art scale. */
    private boolean scaleChanged(double zoom, double artScale) {
        return Doubles.differs(staticCastersZoom, zoom) || Doubles.differs(staticCastersArtScale, artScale);
    }

    /** Tells whether the list was built for another reach of the shadows. */
    private boolean reachChanged() {
        return Doubles.differs(staticCastersReach, shadowReach)
                || Doubles.differs(staticCastersBottomReach, shadowBottomReach);
    }

    /** Tells whether the list was built for another window of the world. */
    private boolean windowChanged() {
        return Doubles.differs(staticCastersWindow[WINDOW_LEFT], shadowWindowLeft - shadowCameraX)
                || Doubles.differs(staticCastersWindow[WINDOW_TOP], shadowWindowTop - shadowCameraY)
                || Doubles.differs(staticCastersWindow[WINDOW_WIDTH], shadowWindowWidth)
                || Doubles.differs(staticCastersWindow[WINDOW_HEIGHT], shadowWindowHeight);
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
        staticCastersWindow[WINDOW_LEFT] = shadowWindowLeft - shadowCameraX;
        staticCastersWindow[WINDOW_TOP] = shadowWindowTop - shadowCameraY;
        staticCastersWindow[WINDOW_WIDTH] = shadowWindowWidth;
        staticCastersWindow[WINDOW_HEIGHT] = shadowWindowHeight;
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
        if (shadowingResource(tile) == null && shadowingBuilding(tile) == null) {
            return;
        }
        // A static shadow is wanted when it reaches into the window the static layer covers, not only the canvas
        if (!FxShadowHelper.canReach(screenX - shadowWindowLeft, screenY + f.halfTileHeight - shadowWindowTop,
                shadowWindowWidth, shadowWindowHeight, shadowReach, shadowBottomReach)) {
            return;
        }
        addOffscreenShadow(f, tile, x, y, screenX, screenY);
    }

    /** Adds the shadow of the resource and of the building of a tile that is not drawn to the static list. */
    private void addOffscreenShadow(FrameState f, Tile tile, int x, int y, double screenX, double screenY) {
        final var resource = shadowingResource(tile);
        final var building = shadowingBuilding(tile);
        final boolean water = tile.getType().isWater();
        final double drop = FxObjectShadows.waterDrop(water,
                water && spriteManager.getTileSprite(f.mapType, tile) != null, f.halfTileHeight);
        Image image = null;
        var contactArt = 0;
        if (resource != null) {
            final String preferredKind = resourceKinds.preferredKind(f.map, f.mapType, resource.getType(), x, y,
                    f.worldSeed);
            final var metadata = spriteManager.getResourceSprite(
                    f.mapType, resource.getType(), x, y, f.worldSeed, preferredKind);
            image = metadata != null ? metadata.getImage() : null;
            contactArt = metadata != null ? metadata.getContactArtPixels() : 0;
        }
        FxObjectShadows.addForTile(staticShadows, resource != null ? resource.getType() : null, image, contactArt,
                building != null ? building.getType() : null,
                building != null ? buildingSprites.shadowImages(building) : null,
                staticAnchor(f, screenX, screenY + drop));
    }

    /** Gives the resource of a tile when it casts a shadow, null when the tile has none. */
    private static Resource shadowingResource(Tile tile) {
        return tile.hasResource() ? tile.getWorldResource() : null;
    }

    /** Gives the building of a tile when it casts a shadow, null when the tile has none. */
    private static Building shadowingBuilding(Tile tile) {
        return tile.hasBuilding() ? tile.getBuilding() : null;
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
        var preview = cachedPreview;
        if (preview == null || !isCurrent(preview, connections)) {
            preview = new BuildingPreview(previewTileX, previewTileY, previewBuildingType, previewRotation,
                    connections, previewRgba, previewColor);
            cachedPreview = preview;
        }
        return preview;
    }

    /** Tells whether the cached ghost stands on the hovered tile, is of the selected piece and has its tint. */
    private boolean isCurrent(BuildingPreview preview, int connections) {
        final boolean sameTile = preview.tileX() == previewTileX && preview.tileY() == previewTileY;
        final boolean samePiece = preview.type() == previewBuildingType && preview.rotation() == previewRotation
                && preview.connections() == connections;
        return sameTile && samePiece && Objects.equals(preview.color(), previewColor);
    }

    /**
     * Queues the health bar over the building of the hovered tile, drawn after everything else so nothing covers it,
     * when the hammer is in hand and the building lost some health.
     */
    private void queueHealthBar(Tile tile, double artScale, double zoom) {
        final var building = tile.hasBuilding() ? tile.getBuilding() : tile.getFloor();
        if (!FxBuildingHealthBar.isShown(buildModeActive, building)) {
            return;
        }
        final var graphics = AppConfig.getInstance().graphics();
        final double halfTileWidth = graphics.getTileHalfWidth() * zoom;
        final double halfTileHeight = graphics.getTileHalfHeight() * zoom;
        final int x = building.getTileX();
        final int y = building.getTileY();
        final double centerX = (x - y) * halfTileWidth + camera.getX();
        final double topVertexY = (x + y) * halfTileHeight + camera.getY()
                + FxObjectShadows.waterDrop(tile.getType().isWater(),
                spriteManager.getTileSprite(activeMapType, tile) != null, halfTileHeight);
        final double centerY = topVertexY + halfTileHeight;
        final var art = buildingSprites.imageFor(building, 0);
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
    private static double cursorCenterY(Tile tile, int tileX, int tileY, double zoom, double offsetY) {
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
        var highlight = flashSlot ? cachedFlashHighlight : cachedHighlight;
        if (highlight == null
                || !showsSameCursor(highlight, centerX, centerY, tileWidth, tileHeight, lightGround, tone)) {
            highlight = new TileHighlight(centerX, centerY, tileWidth, tileHeight, lightGround, tone);
            if (flashSlot) {
                cachedFlashHighlight = highlight;
            } else {
                cachedHighlight = highlight;
            }
        }
        return highlight;
    }

    /** Tells whether a highlight is the crosshair that the given values would make. */
    private static boolean showsSameCursor(TileHighlight highlight, double centerX, double centerY, double tileWidth,
                                           double tileHeight, boolean lightGround, ClickFeedback.Kind tone) {
        final boolean sameSpot = Doubles.areEqual(highlight.centerX(), centerX)
                && Doubles.areEqual(highlight.centerY(), centerY);
        final boolean sameSize = Doubles.areEqual(highlight.tileWidth(), tileWidth)
                && Doubles.areEqual(highlight.tileHeight(), tileHeight);
        return sameSpot && sameSize && highlight.lightGround() == lightGround && highlight.tone() == tone;
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

}
