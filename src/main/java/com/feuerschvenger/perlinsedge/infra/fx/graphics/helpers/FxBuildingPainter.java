package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Building;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Fence;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.FenceConnections;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.FenceGate;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Draws every building type procedurally with isometric projection.
 * The domain model stays free of JavaFX, so the shapes live here and are selected by {@link Building#getType()}.
 */
public final class FxBuildingPainter {

    // Divisors that take a share of a size
    /** Divisor that halves a size, to centre a shape on its anchor or to take half of a dimension. */
    private static final int HALF_DIVISOR = 2;
    /** Divisor that takes a quarter of a size. */
    private static final int QUARTER_DIVISOR = 4;
    /** Divisor that takes an eighth of a size. */
    private static final int EIGHTH_DIVISOR = 8;

    // Outline and rail widths, in pixels at zoom 1
    /** Width of the thin outline of the furniture and the torch. */
    private static final double THIN_OUTLINE = 0.5;
    /** Width of the rails of a fence or a gate, which also sets the stroke after a post. */
    private static final int RAIL_WIDTH = 3;

    // Polygon sizes
    /** Corners of a flame drawn as a diamond. */
    private static final int DIAMOND_POINTS = 4;
    /** Corners of the outer flame of the campfire. */
    private static final int FIRE_POINTS = 5;
    /** Corners of the inner flame of the campfire. */
    private static final int TRIANGLE_POINTS = 3;

    // Standing torch
    private static final int TORCH_POST_WIDTH = 6;
    private static final int TORCH_POST_HEIGHT = 34;
    private static final int TORCH_FLAME_HEIGHT = 16;
    private static final int TORCH_FLAME_WIDTH = 12;
    /** Share of the flame height at which the outer flame is widest. */
    private static final double TORCH_FLAME_SHOULDER = 0.3;
    /** How far the outer flame reaches below the top of the post. */
    private static final int TORCH_FLAME_DIP = 2;
    /** Share of the flame height of the tip of the inner flame. */
    private static final double TORCH_CORE_TIP = 0.65;
    /** Share of the flame height at which the inner flame is widest. */
    private static final double TORCH_CORE_SHOULDER = 0.2;

    // Bridge
    private static final int BRIDGE_LENGTH = 80;
    private static final int BRIDGE_WIDTH = 30;
    private static final int BRIDGE_THICKNESS = 10;
    private static final int BRIDGE_PLANK_SPACING = 15;

    // Fence and gate
    private static final int FENCE_POST_WIDTH = 8;
    private static final int GATE_POST_WIDTH = 6;
    private static final int FENCE_POST_HEIGHT = 30;
    /** Divisor that gives how far a rail reaches along an axis from the post, a half of the half tile. */
    private static final double RAIL_REACH_DIVISOR = 2.0;
    /** The side of a fence from which the rails stand in front of the post, so the post is drawn before it. */
    private static final int FIRST_FRONT_SIDE = 2;
    /** Heights of the two rails, as shares of the height of the post. */
    private static final double[] RAIL_HEIGHT_SHARES = {0.45, 0.8};

    // Furnace
    private static final int FURNACE_SIZE = 50;
    private static final int FURNACE_OPENING_WIDTH = 20;
    private static final int FURNACE_OPENING_HEIGHT = 15;
    /** Share of the furnace height at which the opening starts. */
    private static final double FURNACE_OPENING_TOP = 0.6;
    /** Opacity of the glow in the opening. */
    private static final double FURNACE_GLOW_OPACITY = 0.7;
    /** Share of the opening height the glow starts below the top of the opening. */
    private static final double FURNACE_GLOW_DROP = 0.2;

    // Anvil
    private static final int ANVIL_BASE_WIDTH = 40;
    private static final int ANVIL_BASE_HEIGHT = 15;
    private static final int ANVIL_BODY_WIDTH = 25;
    private static final int ANVIL_BODY_HEIGHT = 20;
    private static final int ANVIL_TOP_WIDTH = 35;
    private static final int ANVIL_TOP_HEIGHT = 10;
    private static final int ANVIL_CORNER_RADIUS = 5;

    // Workbench
    private static final int TABLE_WIDTH = 60;
    private static final int TABLE_HEIGHT = 10;
    private static final int LEG_WIDTH = 8;
    private static final int LEG_HEIGHT = 30;
    /** Multiple of the leg width the right legs stand in from the end of the table. */
    private static final double LEG_END_INSET = 1.5;
    /** Share of the leg width the back legs are moved down the screen, which puts them behind. */
    private static final double LEG_BACK_DROP = 0.7;

    // Campfire
    private static final int CAMPFIRE_BASE_WIDTH = 30;
    private static final int CAMPFIRE_BASE_HEIGHT = 10;
    private static final int CAMPFIRE_FIRE_HEIGHT = 25;
    private static final int CAMPFIRE_WOOD_HEIGHT = 10;
    /** Share of the fire height of the tip of the inner flame. */
    private static final double CAMPFIRE_CORE_TIP = 0.7;

    // Bed
    private static final int BED_WIDTH = 50;
    private static final int BED_LENGTH = 70;
    private static final int BED_HEADBOARD_HEIGHT = 25;
    /** Share of the bed width the headboard spans. */
    private static final double BED_HEADBOARD_SHARE = 0.8;
    /** Opacity of the blanket. */
    private static final double BED_BLANKET_OPACITY = 0.8;
    /** How far the blanket is inset from the edge of the bed on each side. */
    private static final int BED_BLANKET_INSET = 2;
    /** How much narrower and shorter than the bed the blanket is, the inset of both sides. */
    private static final int BED_BLANKET_SHRINK = 4;

    // Sleeping bag
    private static final int SLEEPING_BAG_WIDTH = 20;
    private static final int SLEEPING_BAG_LENGTH = 40;
    /** Share of the bag length the pillow end spans. */
    private static final double SLEEPING_BAG_PILLOW_SHARE = 0.3;

    // Chest
    private static final double CHEST_SIZE_FACTOR = 0.6;
    private static final double CHEST_VISUAL_HEIGHT_FACTOR = 1.2;
    private static final double CHEST_ELEVATION_OFFSET_FACTOR = 0.3;
    private static final double CHEST_LOCK_SIZE_FACTOR = 0.4;
    /** Half heights of the chest footprint from the back vertex of the lid to its front vertex. */
    private static final int LID_DEPTH_IN_HALF_HEIGHTS = 2;
    private static final Color CHEST_BROWN = Color.rgb(139, 69, 19);
    private static final Color CHEST_LID_COLOR = CHEST_BROWN.deriveColor(0, 1, 1.2, 1);
    private static final Color CHEST_RIGHT_COLOR = CHEST_BROWN.deriveColor(0, 1, 0.8, 1);
    private static final Color CHEST_LEFT_COLOR = CHEST_BROWN.deriveColor(0, 1, 0.9, 1);

    /** The four sides of a fence in drawing order: the back rails, then the front rails, with the screen direction of each. */
    private static final int[] FENCE_SIDES = {
            FenceConnections.UPPER_LEFT, FenceConnections.UPPER_RIGHT,
            FenceConnections.LOWER_LEFT, FenceConnections.LOWER_RIGHT};
    private static final int[][] FENCE_SIDE_DIRECTION = {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}};

    private FxBuildingPainter() {
    }

    /**
     * Draws a building at the given screen position.
     *
     * @param gc      graphics context
     * @param building the building to draw
     * @param screenX  screen X of the tile origin
     * @param screenY  screen Y of the tile origin
     * @param zoom     current zoom level
     */
    public static void draw(GraphicsContext gc, Building building, double screenX, double screenY, double zoom) {
        switch (building.getType()) {
            case BRIDGE -> drawBridge(gc, screenX, screenY, zoom);
            case FENCE -> drawFence(gc, building instanceof Fence fence ? fence.getConnections() : 0, screenX, screenY, zoom);
            case FENCE_GATE -> drawFenceGate(gc, (FenceGate) building, screenX, screenY, zoom);
            case CHEST, FURNACE, ANVIL, WORKBENCH, CAMPFIRE, STANDING_TORCH, BED, SLEEPING_BAG ->
                    drawFurnishing(gc, building.getType(), screenX, screenY, zoom);
        }
    }

    /** Draws the pieces that furnish a camp: the containers, the crafting stations, the lights and the bed. */
    private static void drawFurnishing(GraphicsContext gc, BuildingType type, double screenX, double screenY,
                                       double zoom) {
        switch (type) {
            case CHEST -> drawChest(gc, screenX, screenY, zoom);
            case FURNACE -> drawFurnace(gc, screenX, screenY, zoom);
            case ANVIL -> drawAnvil(gc, screenX, screenY, zoom);
            case WORKBENCH -> drawWorkbench(gc, screenX, screenY, zoom);
            case CAMPFIRE -> drawCampfire(gc, screenX, screenY, zoom);
            case STANDING_TORCH -> drawStandingTorch(gc, screenX, screenY, zoom);
            case BED -> drawBed(gc, screenX, screenY, zoom);
            case SLEEPING_BAG -> drawSleepingBag(gc, screenX, screenY, zoom);
            case BRIDGE, FENCE, FENCE_GATE -> throw new IllegalArgumentException("Not a furnishing: " + type);
        }
    }

    private static void drawStandingTorch(GraphicsContext gc, double screenX, double screenY, double zoom) {
        double postWidth = TORCH_POST_WIDTH * zoom;
        double postHeight = TORCH_POST_HEIGHT * zoom;
        double flameHeight = TORCH_FLAME_HEIGHT * zoom;
        double flameWidth = TORCH_FLAME_WIDTH * zoom;
        double postTopY = screenY - postHeight;

        gc.setFill(Color.SADDLEBROWN);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(THIN_OUTLINE * zoom);
        gc.fillRect(screenX - postWidth / HALF_DIVISOR, postTopY, postWidth, postHeight);
        gc.strokeRect(screenX - postWidth / HALF_DIVISOR, postTopY, postWidth, postHeight);

        gc.setFill(Color.ORANGE);
        gc.fillPolygon(new double[]{
                screenX, screenX + flameWidth / HALF_DIVISOR, screenX, screenX - flameWidth / HALF_DIVISOR
        }, new double[]{
                postTopY - flameHeight, postTopY - flameHeight * TORCH_FLAME_SHOULDER,
                postTopY + TORCH_FLAME_DIP * zoom, postTopY - flameHeight * TORCH_FLAME_SHOULDER
        }, DIAMOND_POINTS);
        gc.setFill(Color.YELLOW);
        gc.fillPolygon(new double[]{
                screenX, screenX + flameWidth / QUARTER_DIVISOR, screenX, screenX - flameWidth / QUARTER_DIVISOR
        }, new double[]{
                postTopY - flameHeight * TORCH_CORE_TIP, postTopY - flameHeight * TORCH_CORE_SHOULDER, postTopY,
                postTopY - flameHeight * TORCH_CORE_SHOULDER
        }, DIAMOND_POINTS);
    }

    private static void drawBridge(GraphicsContext gc, double screenX, double screenY, double zoom) {
        double length = BRIDGE_LENGTH * zoom;
        double width = BRIDGE_WIDTH * zoom;
        double thickness = BRIDGE_THICKNESS * zoom;

        double drawY = screenY - thickness / HALF_DIVISOR;

        gc.setFill(Color.SADDLEBROWN);
        gc.setStroke(Color.DARKGOLDENROD);
        gc.setLineWidth(1 * zoom);

        gc.fillRect(screenX - length / HALF_DIVISOR, drawY - width / HALF_DIVISOR, length, width);
        gc.strokeRect(screenX - length / HALF_DIVISOR, drawY - width / HALF_DIVISOR, length, width);

        double plankSpacing = BRIDGE_PLANK_SPACING * zoom;
        for (double i = 0; i < length; i += plankSpacing) {
            gc.strokeLine(screenX - length / HALF_DIVISOR + i, drawY - width / HALF_DIVISOR,
                    screenX - length / HALF_DIVISOR + i, drawY + width / HALF_DIVISOR);
        }
    }

    /**
     * Draws a fence as a post with a half rail toward every joined side. The rails toward the upper edges stand behind
     * the post and are drawn first, the ones toward the lower edges stand in front and are drawn last.
     */
    private static void drawFence(GraphicsContext gc, int connections, double screenX, double screenY, double zoom) {
        final AppConfig.GraphicsConfig graphics = AppConfig.getInstance().graphics();
        final double centerY = screenY + graphics.getTileHalfHeight() * zoom;
        final double reachX = graphics.getTileHalfWidth() * zoom / RAIL_REACH_DIVISOR;
        final double reachY = graphics.getTileHalfHeight() * zoom / RAIL_REACH_DIVISOR;
        final double postWidth = FENCE_POST_WIDTH * zoom;
        final double postHeight = FENCE_POST_HEIGHT * zoom;

        gc.setStroke(Color.BROWN);
        gc.setLineWidth(RAIL_WIDTH * zoom);
        for (var side = 0; side < FENCE_SIDES.length; side++) {
            if (side == FIRST_FRONT_SIDE) {
                drawPost(gc, screenX, centerY, postWidth, postHeight, zoom);
            }
            if ((connections & FENCE_SIDES[side]) == 0) {
                continue;
            }
            for (double share : RAIL_HEIGHT_SHARES) {
                final double height = postHeight * share;
                gc.strokeLine(screenX, centerY - height,
                        screenX + FENCE_SIDE_DIRECTION[side][0] * reachX,
                        centerY - height + FENCE_SIDE_DIRECTION[side][1] * reachY);
            }
        }
    }

    private static void drawPost(GraphicsContext gc, double screenX, double centerY, double width, double height,
                                 double zoom) {
        gc.setFill(Color.DARKGOLDENROD);
        gc.setStroke(Color.SADDLEBROWN);
        gc.setLineWidth(1 * zoom);
        gc.fillRect(screenX - width / HALF_DIVISOR, centerY - height, width, height);
        gc.strokeRect(screenX - width / HALF_DIVISOR, centerY - height, width, height);
        gc.setStroke(Color.BROWN);
        gc.setLineWidth(RAIL_WIDTH * zoom);
    }

    /**
     * Draws a gate as two posts at the ends of the axis it runs along, with two rails between them while it is closed.
     * An open gate has only its posts.
     */
    private static void drawFenceGate(GraphicsContext gc, FenceGate gate, double screenX, double screenY, double zoom) {
        final AppConfig.GraphicsConfig graphics = AppConfig.getInstance().graphics();
        final double centerY = screenY + graphics.getTileHalfHeight() * zoom;
        final double reachX = graphics.getTileHalfWidth() * zoom / RAIL_REACH_DIVISOR;
        final double reachY = graphics.getTileHalfHeight() * zoom / RAIL_REACH_DIVISOR;
        // Rotation 0 runs from the upper left edge to the lower right edge, rotation 1 from the upper right to the lower left
        final double dx = gate.getRotation() == 0 ? 1 : -1;
        final double postWidth = GATE_POST_WIDTH * zoom;
        final double postHeight = FENCE_POST_HEIGHT * zoom;
        final double[] ends = {-1, 1};

        for (double end : ends) {
            drawPost(gc, screenX + end * dx * reachX, centerY + end * reachY, postWidth, postHeight, zoom);
        }
        if (!gate.isOpen()) {
            gc.setStroke(Color.BROWN);
            gc.setLineWidth(RAIL_WIDTH * zoom);
            for (double share : RAIL_HEIGHT_SHARES) {
                final double height = postHeight * share;
                gc.strokeLine(screenX - dx * reachX, centerY - reachY - height, screenX + dx * reachX,
                        centerY + reachY - height);
            }
        }
    }

    private static void drawFurnace(GraphicsContext gc, double screenX, double screenY, double zoom) {
        double size = FURNACE_SIZE * zoom;
        double openingWidth = FURNACE_OPENING_WIDTH * zoom;
        double openingHeight = FURNACE_OPENING_HEIGHT * zoom;

        double drawY = screenY - size / HALF_DIVISOR;

        gc.setFill(Color.DARKGRAY);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1 * zoom);
        gc.fillRect(screenX - size / HALF_DIVISOR, drawY, size, size);
        gc.strokeRect(screenX - size / HALF_DIVISOR, drawY, size, size);

        gc.setFill(Color.ORANGERED);
        gc.fillRect(screenX - openingWidth / HALF_DIVISOR, drawY + (size * FURNACE_OPENING_TOP), openingWidth,
                openingHeight);

        gc.setFill(Color.YELLOW.deriveColor(0, 1, 1, FURNACE_GLOW_OPACITY));
        gc.fillOval(screenX - openingWidth / QUARTER_DIVISOR,
                drawY + (size * FURNACE_OPENING_TOP) + (openingHeight * FURNACE_GLOW_DROP),
                openingWidth / HALF_DIVISOR, openingHeight / HALF_DIVISOR);
    }

    private static void drawAnvil(GraphicsContext gc, double screenX, double screenY, double zoom) {
        double baseWidth = ANVIL_BASE_WIDTH * zoom;
        double baseHeight = ANVIL_BASE_HEIGHT * zoom;
        double bodyWidth = ANVIL_BODY_WIDTH * zoom;
        double bodyHeight = ANVIL_BODY_HEIGHT * zoom;
        double topWidth = ANVIL_TOP_WIDTH * zoom;
        double topHeight = ANVIL_TOP_HEIGHT * zoom;
        double cornerRadius = ANVIL_CORNER_RADIUS * zoom;

        gc.setFill(Color.DARKGRAY.darker());
        gc.fillRect(screenX - baseWidth / HALF_DIVISOR, screenY - baseHeight, baseWidth, baseHeight);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(THIN_OUTLINE * zoom);
        gc.strokeRect(screenX - baseWidth / HALF_DIVISOR, screenY - baseHeight, baseWidth, baseHeight);

        gc.setFill(Color.DARKGRAY);
        gc.fillRect(screenX - bodyWidth / HALF_DIVISOR, screenY - baseHeight - bodyHeight, bodyWidth, bodyHeight);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(THIN_OUTLINE * zoom);
        gc.strokeRect(screenX - bodyWidth / HALF_DIVISOR, screenY - baseHeight - bodyHeight, bodyWidth, bodyHeight);

        gc.setFill(Color.GRAY);
        gc.fillRoundRect(screenX - topWidth / HALF_DIVISOR, screenY - baseHeight - bodyHeight - topHeight, topWidth,
                topHeight, cornerRadius, cornerRadius);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(THIN_OUTLINE * zoom);
        gc.strokeRoundRect(screenX - topWidth / HALF_DIVISOR, screenY - baseHeight - bodyHeight - topHeight, topWidth,
                topHeight, cornerRadius, cornerRadius);
    }

    private static void drawWorkbench(GraphicsContext gc, double screenX, double screenY, double zoom) {
        double tableWidth = TABLE_WIDTH * zoom;
        double tableHeight = TABLE_HEIGHT * zoom;
        double legWidth = LEG_WIDTH * zoom;
        double legHeight = LEG_HEIGHT * zoom;

        gc.setFill(Color.SADDLEBROWN);
        gc.fillRect(screenX - tableWidth / HALF_DIVISOR, screenY - tableHeight - legHeight, tableWidth, tableHeight);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(THIN_OUTLINE * zoom);
        gc.strokeRect(screenX - tableWidth / HALF_DIVISOR, screenY - tableHeight - legHeight, tableWidth, tableHeight);

        gc.setFill(Color.BROWN);
        gc.fillRect(screenX - tableWidth / HALF_DIVISOR + (legWidth / HALF_DIVISOR), screenY - legHeight, legWidth,
                legHeight);
        gc.fillRect(screenX + tableWidth / HALF_DIVISOR - (legWidth * LEG_END_INSET), screenY - legHeight, legWidth,
                legHeight);
        gc.fillRect(screenX - (legWidth / HALF_DIVISOR), screenY - legHeight + (legWidth * LEG_BACK_DROP), legWidth,
                legHeight);
        gc.fillRect(screenX + tableWidth / HALF_DIVISOR - (legWidth / HALF_DIVISOR) - (legWidth * LEG_BACK_DROP),
                screenY - legHeight + (legWidth * LEG_BACK_DROP), legWidth, legHeight);

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(THIN_OUTLINE * zoom);
        gc.strokeRect(screenX - tableWidth / HALF_DIVISOR + (legWidth / HALF_DIVISOR), screenY - legHeight, legWidth,
                legHeight);
        gc.strokeRect(screenX + tableWidth / HALF_DIVISOR - (legWidth * LEG_END_INSET), screenY - legHeight, legWidth,
                legHeight);
    }

    private static void drawCampfire(GraphicsContext gc, double screenX, double screenY, double zoom) {
        double baseWidth = CAMPFIRE_BASE_WIDTH * zoom;
        double baseHeight = CAMPFIRE_BASE_HEIGHT * zoom;
        double fireHeight = CAMPFIRE_FIRE_HEIGHT * zoom;
        double woodHeight = CAMPFIRE_WOOD_HEIGHT * zoom;

        gc.setFill(Color.BROWN.darker().darker());
        gc.fillOval(screenX - baseWidth / HALF_DIVISOR, screenY - baseHeight / HALF_DIVISOR + woodHeight, baseWidth,
                baseHeight);

        gc.setFill(Color.ORANGE);
        gc.fillPolygon(new double[]{
                screenX, screenX + baseWidth / QUARTER_DIVISOR, screenX + baseWidth / EIGHTH_DIVISOR,
                screenX - baseWidth / EIGHTH_DIVISOR, screenX - baseWidth / QUARTER_DIVISOR
        }, new double[]{
                screenY - fireHeight, screenY - baseHeight / HALF_DIVISOR, screenY - baseHeight / QUARTER_DIVISOR,
                screenY - baseHeight / QUARTER_DIVISOR, screenY - baseHeight / HALF_DIVISOR
        }, FIRE_POINTS);
        gc.setFill(Color.YELLOW);
        gc.fillPolygon(new double[]{
                screenX, screenX + baseWidth / EIGHTH_DIVISOR, screenX - baseWidth / EIGHTH_DIVISOR
        }, new double[]{
                screenY - fireHeight * CAMPFIRE_CORE_TIP, screenY - baseHeight / HALF_DIVISOR,
                screenY - baseHeight / HALF_DIVISOR
        }, TRIANGLE_POINTS);

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(THIN_OUTLINE * zoom);
        gc.strokeOval(screenX - baseWidth / HALF_DIVISOR, screenY - baseHeight / HALF_DIVISOR + woodHeight, baseWidth,
                baseHeight);
    }

    /** The stand-in of the sleeping bag: a flat straw roll with a pale pillow end, drawn when its art is missing. */
    private static void drawSleepingBag(GraphicsContext gc, double screenX, double screenY, double zoom) {
        double width = SLEEPING_BAG_WIDTH * zoom;
        double length = SLEEPING_BAG_LENGTH * zoom;
        double left = screenX - width / HALF_DIVISOR;
        double top = screenY - length / HALF_DIVISOR;

        gc.setFill(Color.BURLYWOOD);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(THIN_OUTLINE * zoom);
        gc.fillRect(left, top, width, length);
        gc.strokeRect(left, top, width, length);

        gc.setFill(Color.IVORY);
        gc.fillRect(left, top, width, length * SLEEPING_BAG_PILLOW_SHARE);
        gc.strokeRect(left, top, width, length * SLEEPING_BAG_PILLOW_SHARE);
    }

    private static void drawBed(GraphicsContext gc, double screenX, double screenY, double zoom) {
        double bedWidth = BED_WIDTH * zoom;
        double bedLength = BED_LENGTH * zoom;
        double headboardHeight = BED_HEADBOARD_HEIGHT * zoom;
        double headboardWidth = bedWidth * BED_HEADBOARD_SHARE;

        double drawX = screenX;
        double drawY = screenY + (AppConfig.getInstance().graphics().getTileHalfHeight() * zoom);

        gc.setFill(Color.PERU);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1 * zoom);

        gc.fillRect(drawX - bedWidth / HALF_DIVISOR, drawY - bedLength, bedWidth, bedLength);
        gc.strokeRect(drawX - bedWidth / HALF_DIVISOR, drawY - bedLength, bedWidth, bedLength);

        gc.setFill(Color.LIGHTGRAY.deriveColor(0, 1, 1, BED_BLANKET_OPACITY));
        gc.fillRect(drawX - bedWidth / HALF_DIVISOR + (BED_BLANKET_INSET * zoom),
                drawY - bedLength + (BED_BLANKET_INSET * zoom), bedWidth - (BED_BLANKET_SHRINK * zoom),
                bedLength - (BED_BLANKET_SHRINK * zoom));
        gc.strokeRect(drawX - bedWidth / HALF_DIVISOR + (BED_BLANKET_INSET * zoom),
                drawY - bedLength + (BED_BLANKET_INSET * zoom), bedWidth - (BED_BLANKET_SHRINK * zoom),
                bedLength - (BED_BLANKET_SHRINK * zoom));

        gc.setFill(Color.SADDLEBROWN);
        gc.fillRect(drawX - headboardWidth / HALF_DIVISOR, drawY - bedLength - headboardHeight, headboardWidth,
                headboardHeight);
        gc.strokeRect(drawX - headboardWidth / HALF_DIVISOR, drawY - bedLength - headboardHeight, headboardWidth,
                headboardHeight);
    }

    // ------------------------------------------------------------------
    //  Chest
    // ------------------------------------------------------------------

    /**
     * Vertical positions of the diamond that forms the lid of a chest.
     *
     * @param topY    Y of the back vertex
     * @param sideY   Y of the left and right vertices
     * @param bottomY Y of the front vertex
     */
    record ChestLid(double topY, double sideY, double bottomY) {
    }

    /**
     * Computes the lid diamond: the back vertex sits at the top of the chest, the front vertex one chest tile
     * height lower, and the side vertices halfway, so the lid has a real surface.
     *
     * @param chestTopY      Y of the top of the chest
     * @param chestHalfHeight half the height of the chest footprint
     * @return the lid vertices
     */
    static ChestLid chestLid(double chestTopY, double chestHalfHeight) {
        return new ChestLid(chestTopY, chestTopY + chestHalfHeight,
                chestTopY + LID_DEPTH_IN_HALF_HEIGHTS * chestHalfHeight);
    }

    private static void drawChest(GraphicsContext gc, double screenX, double screenY, double zoom) {
        final var config = AppConfig.getInstance();

        final double halfWidth = config.graphics().getTileHalfWidth() * zoom;
        final double halfHeight = config.graphics().getTileHalfHeight() * zoom;

        final double chestHalfWidth = halfWidth * CHEST_SIZE_FACTOR;
        final double chestHalfHeight = halfHeight * CHEST_SIZE_FACTOR;

        final double elevationOffset = halfHeight * CHEST_ELEVATION_OFFSET_FACTOR;
        final double chestTopY = screenY - elevationOffset;
        final double chestBodyHeight = chestHalfHeight * CHEST_VISUAL_HEIGHT_FACTOR;

        final var lid = chestLid(chestTopY, chestHalfHeight);
        final double lidRightX = screenX + chestHalfWidth;
        final double lidRightY = lid.sideY();
        final double lidBottomY = lid.bottomY();
        final double lidLeftX = screenX - chestHalfWidth;
        final double lidLeftY = lid.sideY();
        final double lidTopY = lid.topY();

        final double bodyBaseRightY = lidRightY + chestBodyHeight;
        final double bodyBaseBottomY = lidBottomY + chestBodyHeight;
        final double bodyBaseLeftY = lidLeftY + chestBodyHeight;

        final double lockWidth = chestHalfWidth * CHEST_LOCK_SIZE_FACTOR;
        final double lockHeight = chestHalfHeight * CHEST_LOCK_SIZE_FACTOR;
        final double lockX = screenX - lockWidth / HALF_DIVISOR;
        final double lockY = chestTopY + chestHalfHeight - lockHeight / HALF_DIVISOR;

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.0 * zoom);

        gc.setFill(CHEST_LEFT_COLOR);
        gc.beginPath();
        gc.moveTo(lidLeftX, lidLeftY);
        gc.lineTo(lidLeftX, bodyBaseLeftY);
        gc.lineTo(screenX, bodyBaseBottomY);
        gc.lineTo(screenX, lidBottomY);
        gc.closePath();
        gc.fill();
        gc.stroke();

        gc.setFill(CHEST_RIGHT_COLOR);
        gc.beginPath();
        gc.moveTo(lidRightX, lidRightY);
        gc.lineTo(lidRightX, bodyBaseRightY);
        gc.lineTo(screenX, bodyBaseBottomY);
        gc.lineTo(screenX, lidBottomY);
        gc.closePath();
        gc.fill();
        gc.stroke();

        gc.setFill(CHEST_LID_COLOR);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.0 * zoom);
        gc.beginPath();
        gc.moveTo(screenX, lidTopY);
        gc.lineTo(lidRightX, lidRightY);
        gc.lineTo(screenX, lidBottomY);
        gc.lineTo(lidLeftX, lidLeftY);
        gc.closePath();
        gc.fill();
        gc.stroke();

        gc.setFill(Color.DARKGRAY);
        gc.fillOval(lockX, lockY, lockWidth, lockHeight);
    }
}
