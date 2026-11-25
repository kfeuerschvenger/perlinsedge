package com.feuerschvenger.perlinsedge.infra.fx.graphics;

import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Building;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Fence;
import com.feuerschvenger.perlinsedge.domain.entities.creatures.Creature;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Slime;
import com.feuerschvenger.perlinsedge.domain.entities.items.DroppedItem;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.BuildingPreview;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.CameraView;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.FxBuildingDrawingHelper;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.FxBuildingHealthBar;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.FxEnemyDrawingHelper;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.FxItemDrawingHelper;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.FxPlayerDrawingHelper;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.FxResourceFallbackPainter;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.FxTileDrawingHelper;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.HealthBarMark;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.TileHighlight;
import com.feuerschvenger.perlinsedge.infra.fx.utils.AssetPaths;
import com.feuerschvenger.perlinsedge.infra.fx.utils.BuildingSprites;
import com.feuerschvenger.perlinsedge.infra.fx.utils.CursorStyle;
import com.feuerschvenger.perlinsedge.infra.fx.utils.FlatTileSprites;
import com.feuerschvenger.perlinsedge.infra.fx.utils.GroundDecals;
import com.feuerschvenger.perlinsedge.infra.fx.utils.HurtReaction;
import com.feuerschvenger.perlinsedge.infra.fx.utils.SpriteManager;
import com.feuerschvenger.perlinsedge.infra.fx.utils.TileDepths;
import com.feuerschvenger.perlinsedge.infra.fx.utils.TintedSprites;
import com.feuerschvenger.perlinsedge.infra.fx.utils.WaterShimmer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 * Draws the queued items of a frame: it looks at what a {@link Renderable} holds and calls the painter of that kind of
 * thing. The frame values it needs, the map type, the grey view, the animation clocks and the camera as the building
 * painter sees it, are given once at the start of every frame with {@link #beginFrame}.
 */
final class FxRenderableDrawer {

    /** Shimmer lines fainter than this are skipped, since they are not visible. */
    private static final double MIN_SHIMMER_ALPHA = 0.02;

    /** Brightness the flash effect adds to a struck resource or tile. */
    private static final double FLASH_BRIGHTNESS = 0.7;

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

    /** Red channel of the pulsing circle of an attack. */
    private static final int ATTACK_RED = 255;
    /** Opacity of the pulsing circle of an attack when it starts. */
    private static final double ATTACK_OPACITY = 0.5;
    /** How far the circle of an attack is moved left and up per unit of progress, which centres it. */
    private static final int ATTACK_CENTRE_OFFSET = 10;
    /** How wide and tall the circle of an attack grows per unit of progress. */
    private static final int ATTACK_SIZE = 20;

    private final GraphicsContext gc;
    private final FxCamera camera;
    private final GameStateManager gameState;
    private final SpriteManager spriteManager;
    private final BuildingSprites buildingSprites = BuildingSprites.getInstance();
    private final ColumnPainter columns;
    private final ColorAdjust flashEffect = createFlashEffect();

    /** The map type of the frame, which picks the biome art of the ground and its decals. */
    private MapType mapType;
    /** Whether the frame draws the grey height view. */
    private boolean grayscaleView;
    /** Whether the grey height view writes the height and resource text on the tiles. */
    private boolean debugTextEnabled;
    /** The shimmer animation clock of the frame. */
    private double waterCycles;
    /** The game clock of the frame in seconds. */
    private double gameTime;
    /** The camera as the building painter sees it. */
    private CameraView cameraView;

    /**
     * Creates the drawer.
     *
     * @param gc            the graphics context to draw on
     * @param camera        the camera controlling the viewport
     * @param gameState     the game state the world is read from
     * @param spriteManager where the sprites come from
     */
    FxRenderableDrawer(GraphicsContext gc, FxCamera camera, GameStateManager gameState, SpriteManager spriteManager) {
        this.gc = gc;
        this.camera = camera;
        this.gameState = gameState;
        this.spriteManager = spriteManager;
        this.columns = new ColumnPainter(gc, spriteManager);
    }

    private static ColorAdjust createFlashEffect() {
        final var effect = new ColorAdjust();
        effect.setBrightness(FLASH_BRIGHTNESS);
        return effect;
    }

    /**
     * Gives the values of the frame about to be drawn.
     *
     * @param frameMapType     the map type, which picks the biome art
     * @param grayscale        whether the grey height view is drawn
     * @param debugText        whether the grey height view writes text on the tiles
     * @param shimmerCycles    the shimmer animation clock
     * @param frameGameTime    the game clock in seconds
     * @param frameCameraView  the camera as the building painter sees it
     */
    void beginFrame(MapType frameMapType, boolean grayscale, boolean debugText, double shimmerCycles,
                    double frameGameTime, CameraView frameCameraView) {
        this.mapType = frameMapType;
        this.grayscaleView = grayscale;
        this.debugTextEnabled = debugText;
        this.waterCycles = shimmerCycles;
        this.gameTime = frameGameTime;
        this.cameraView = frameCameraView;
    }

    /**
     * Dispatches the drawing call based on the type of the renderable object.
     *
     * @param r The Renderable object to draw.
     */
    void draw(Renderable r) {
        final var object = r.object();
        if (object instanceof Tile tile) {
            drawTile(tile, r);
        } else if (object instanceof ResourceType) {
            drawResource(r);
        } else if (object instanceof Building building) {
            drawBuilding(building, r);
        } else if (object instanceof Player) {
            drawPlayer(r);
        } else if (object instanceof Creature) {
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
        FxBuildingDrawingHelper.drawBuilding(gc, building, r.sprite(), r.screenX(), centerY, cameraView);
    }

    /**
     * Draws highlight lines for the tile currently under the mouse.
     *
     * @param highlight The TileHighlight object containing the highlight information.
     */
    private void drawHighlightTile(TileHighlight highlight) {
        FxTileDrawingHelper.drawTileHighlight(gc, highlight, camera.getZoom(), cursorLine(highlight),
                cursorOutline(highlight), cursorLineWidth(highlight), cursorAlpha(highlight));
    }

    /**
     * The crosshair is dark over light ground and light over dark ground, and the answer to a click shows in green or
     * red.
     */
    private static Color cursorLine(TileHighlight highlight) {
        return switch (highlight.tone()) {
            case ACCEPTED -> CURSOR_ACCEPTED;
            case REJECTED -> CURSOR_REJECTED;
            default -> highlight.lightGround() ? CURSOR_DARK : CURSOR_LIGHT;
        };
    }

    /** The outline has the opposite brightness of the crosshair, and is dark under the answer to a click. */
    private static Color cursorOutline(TileHighlight highlight) {
        return switch (highlight.tone()) {
            case ACCEPTED, REJECTED -> CURSOR_DARK_OUTLINE;
            default -> highlight.lightGround() ? CURSOR_LIGHT_OUTLINE : CURSOR_DARK_OUTLINE;
        };
    }

    /** The answer to a click is drawn with a thicker line than the resting crosshair. */
    private static double cursorLineWidth(TileHighlight highlight) {
        return switch (highlight.tone()) {
            case ACCEPTED, REJECTED -> FLASH_LINE_WIDTH;
            default -> CURSOR_LINE_WIDTH;
        };
    }

    /** The answer to a click fades with its own clock, the resting crosshair breathes slowly on the game clock. */
    private double cursorAlpha(TileHighlight highlight) {
        return switch (highlight.tone()) {
            case ACCEPTED, REJECTED ->
                    CursorStyle.flashAlpha(gameState.getClickFeedback().strength(gameTime));
            default -> CursorStyle.breathAlpha(gameTime);
        };
    }

    /**
     * Draws the player entity on the canvas.
     *
     * @param r The Renderable object containing player information.
     */
    private void drawPlayer(Renderable r) {
        final var player = (Player) r.object();
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
     * Draws a creature entity on the canvas.
     *
     * @param r The Renderable object containing creature information.
     */
    private void drawEnemy(Renderable r) {
        final var enemy = (Creature) r.object();
        final double zoom = camera.getZoom();

        if (enemy instanceof Slime slime) {
            FxEnemyDrawingHelper.drawSlime(gc, r.screenX(), r.screenY(), zoom, slime, gameTime);
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

        var saveCount = 0;
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
                columns.drawExposedFaces(gameState.getCurrentMap(), tile, AssetPaths.tileArt(mapType, tile.getType()),
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
        final var map = gameState.getCurrentMap();

        columns.drawBed(map, tile, r.sprite(), r.screenX(), r.screenY(), zoom, seed);

        final double surfaceY = r.screenY() + TileDepths.SURFACE_PIXELS * ZoomSteps.integerArtScale(zoom);
        final Image sheet = spriteManager.getWaterSprite(AssetPaths.waterArt(tile.getType()));
        if (sheet != null) {
            FxTileDrawingHelper.drawTileSprite(gc, sheet, r.screenX(), surfaceY, zoom);
        }

        final int lines = WaterShimmer.lineCount(x, y);
        for (var line = 0; line < lines; line++) {
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
        final Image decal = spriteManager.getDecalSprite(GroundDecals.decalFor(mapType, tile.getType(), tile.getX(), tile.getY(), seed));
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
        var saveCount = 0;
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
        final var droppedItem = (DroppedItem) r.object();
        FxItemDrawingHelper.drawDroppedItem(
                gc, r.screenX(), r.screenY(), camera.getZoom(), droppedItem, gameTime
        );
    }

    /**
     * Renders the visual effect of a player attack.
     *
     * @param player The player entity.
     * @param halfTileWidth Half the width of a zoomed tile.
     * @param halfTileHeight Half the height of a zoomed tile.
     * @param offsetX Camera X offset.
     * @param offsetY Camera Y offset.
     */
    void drawPlayerAttack(Player player, double halfTileWidth, double halfTileHeight, double offsetX, double offsetY) {
        if (!player.isAttacking()) {
            return;
        }
        double progress = player.getAttackAnimationProgress();
        double[] attackPos = player.getAttackPosition();
        double x = attackPos[0];
        double y = attackPos[1];
        // Convert tile coordinates to screen coordinates
        double screenX = (x - y) * halfTileWidth + offsetX;
        double screenY = (x + y) * halfTileHeight + offsetY;
        // Draw the attack effect (a pulsing circle)
        gc.setFill(Color.rgb(ATTACK_RED, 0, 0, ATTACK_OPACITY * (1 - progress)));
        gc.fillOval(
                screenX - ATTACK_CENTRE_OFFSET * progress,
                screenY - ATTACK_CENTRE_OFFSET * progress,
                ATTACK_SIZE * progress,
                ATTACK_SIZE * progress
        );
    }
}
