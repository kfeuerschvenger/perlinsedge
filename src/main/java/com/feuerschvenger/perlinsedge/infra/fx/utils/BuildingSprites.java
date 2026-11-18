package com.feuerschvenger.perlinsedge.infra.fx.utils;

import com.feuerschvenger.perlinsedge.domain.entities.buildings.Building;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Fence;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.FenceConnections;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.FenceGate;
import javafx.scene.image.Image;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Loads the building art once and hands it out by building. The files live in {@code assets/buildings} and are named
 * after the piece and the way it faces: {@code chest_0} to {@code chest_3}, the same for {@code furnace_off},
 * {@code anvil}, {@code workbench}, and {@code bed}, and {@code bridge_0} and {@code bridge_1}. The
 * campfire and the standing torch do not turn and have four flame frames, {@code campfire_0} to {@code campfire_3} and
 * {@code standing_torch_0} to {@code standing_torch_3}. A piece with no file answers null, and the caller draws the
 * procedural shape instead.
 * <p>
 * Building art is 32 px wide and its bottom 16 rows are the footprint of the tile, a 32 by 16 diamond, so the bottom
 * centre of the image is the bottom vertex of the tile and the centre of the tile is {@value #TILE_CENTER_ART_PIXELS}
 * art pixels above it. The image is placed with {@link #drawX} and {@link #drawY}.
 */
@Slf4j
public final class BuildingSprites {
    /** Frames of the animated pieces, the campfire and the standing torch. */
    public static final int FLAME_FRAMES = 4;
    /** Flame frames per second of the game clock. */
    public static final double FLAME_FPS = 8.0;
    /** Rows of the art between the centre of the tile footprint and the bottom of the image. */
    public static final int TILE_CENTER_ART_PIXELS = 8;

    private static final String FOLDER = "/assets/buildings/";

    /** Number of combinations of the four fence sides, one cached layer set for each. */
    private static final int FENCE_MASK_COUNT = 16;
    /** Ways a fence gate faces. */
    private static final int GATE_ROTATIONS = 2;
    /** Ways a furnace faces. */
    private static final int FURNACE_ROTATIONS = 4;
    /** Positions of the half rails in {@code FENCE_RAILS}. */
    private static final int RAIL_UPPER_LEFT = 0;
    private static final int RAIL_UPPER_RIGHT = 1;
    private static final int RAIL_LOWER_LEFT = 2;
    private static final int RAIL_LOWER_RIGHT = 3;
    /** The divisor that puts the centre of an image on a point. */
    private static final double CENTERING_DIVISOR = 2.0;

    private final Function<String, Image> loader;
    private final Map<String, Image> cache = new HashMap<>();
    private final Image[][] fenceCache = new Image[FENCE_MASK_COUNT][];

    /** The lazily created instance that reads the classpath. */
    private static final class InstanceHolder {
        private static final BuildingSprites INSTANCE = new BuildingSprites(BuildingSprites::loadFromClasspath);
    }

    /**
     * Creates a set of sprites that reads its images through a loader.
     *
     * @param loader gives the image of a file name without the extension, or null when there is none
     */
    public BuildingSprites(Function<String, Image> loader) {
        this.loader = loader;
    }

    /** @return the sprites of the game, read from the classpath */
    public static BuildingSprites getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Gives the file name, without extension, of the art of a building in its present state and the way it faces. A
     * rotatable piece has one file for each way it faces, numbered as {@link BuildingType#rotationCount()} describes.
     *
     * @param building the building
     * @param frame    the animation frame, used by the pieces that animate
     * @return the name of the art
     */
    public static String spriteName(Building building, int frame) {
        return switch (building.getType()) {
            case CAMPFIRE, STANDING_TORCH -> FLAME_NAMES.get(building.getType())[Math.floorMod(frame, FLAME_FRAMES)];
            case FENCE_GATE -> (building instanceof FenceGate gate && gate.isOpen() ? GATE_OPEN_NAMES : GATE_CLOSED_NAMES)
                    [Math.floorMod(building.getRotation(), GATE_ROTATIONS)];
            default -> restingName(building.getType(), building.getRotation());
        };
    }

    /**
     * Gives the file name of the art of a type at rest, which is what the palette and the placement ghost show. The
     * furnace has no lit state yet, so it is always shown cold.
     *
     * @param type     the building type
     * @param rotation the way the piece faces
     * @return the name of the art
     */
    public static String restingName(BuildingType type, int rotation) {
        return switch (type) {
            case CAMPFIRE, STANDING_TORCH -> FLAME_NAMES.get(type)[0];
            case FENCE -> FENCE_POST;
            case FENCE_GATE -> GATE_CLOSED_NAMES[Math.floorMod(rotation, GATE_ROTATIONS)];
            case FURNACE -> FURNACE_NAMES[Math.floorMod(rotation, FURNACE_NAMES.length)];
            default -> {
                final String[] names = NAMES.get(type);
                yield names[Math.floorMod(rotation, names.length)];
            }
        };
    }

    /** The names are built once, so naming a building every frame allocates nothing. */
    private static final Map<BuildingType, String[]> NAMES = new EnumMap<>(BuildingType.class);
    private static final Map<BuildingType, String[]> FLAME_NAMES = new EnumMap<>(BuildingType.class);
    private static final String[] FURNACE_NAMES = new String[FURNACE_ROTATIONS];
    private static final String[] GATE_CLOSED_NAMES = {"fence_gate_closed_0", "fence_gate_closed_1"};
    private static final String[] GATE_OPEN_NAMES = {"fence_gate_open_0", "fence_gate_open_1"};
    /** The post of a fence and its four half rails, in the order they are drawn: the back ones, the post, the front ones. */
    private static final String FENCE_POST = "fence_post";
    private static final String[] FENCE_RAILS = {"fence_rail_ul", "fence_rail_ur", "fence_rail_ll", "fence_rail_lr"};

    static {
        for (BuildingType type : BuildingType.values()) {
            final var base = type.name().toLowerCase(Locale.ROOT);
            final var names = new String[type.rotationCount()];
            for (var i = 0; i < names.length; i++) {
                names[i] = type.isRotatable() ? base + "_" + i : base;
            }
            NAMES.put(type, names);
            final var frames = new String[FLAME_FRAMES];
            for (var i = 0; i < FLAME_FRAMES; i++) {
                frames[i] = base + "_" + i;
            }
            FLAME_NAMES.put(type, frames);
        }
        for (var i = 0; i < FURNACE_NAMES.length; i++) {
            FURNACE_NAMES[i] = "furnace_off_" + i;
        }
    }

    /**
     * Gives the animation frame of the flame pieces at a time.
     *
     * @param gameTime seconds on the game clock
     * @return the frame from 0 to {@value #FLAME_FRAMES} minus one
     */
    public static int flameFrame(double gameTime) {
        return Math.floorMod((int) Math.floor(gameTime * FLAME_FPS), FLAME_FRAMES);
    }

    /**
     * Gives the art of a building.
     *
     * @param building the building
     * @param frame    the animation frame
     * @return the image, or null when the piece has no art
     */
    public Image imageFor(Building building, int frame) {
        return image(spriteName(building, frame));
    }

    /**
     * Gives the art of a type at rest facing a given way, which is what the palette and the placement ghost show.
     *
     * @param type     the building type
     * @param rotation the way the piece faces
     * @return the image, or null when the piece has no art
     */
    public Image restingImage(BuildingType type, int rotation) {
        return image(restingName(type, rotation));
    }

    /**
     * Gives the art of a type at rest facing the first way.
     *
     * @param type the building type
     * @return the image, or null when the piece has no art
     */
    public Image restingImage(BuildingType type) {
        return restingImage(type, 0);
    }
    /**
     * Gives the images a fence is drawn from, in the order they are drawn: the half rails toward the upper left and the
     * upper right, which stand behind the post, then the post, then the half rails toward the lower left and the lower
     * right, which stand in front of it. Each image is a full canvas with the anchor of every building sprite, so they
     * are laid on top of each other as they are.
     *
     * @param connections the joined sides of the fence, a mask of {@link FenceConnections}
     * @return the images to draw, empty when there is no art for the post, which means the procedural shape is drawn
     */
    public Image[] fenceLayers(int connections) {
        final int mask = connections & (FENCE_MASK_COUNT - 1);
        Image[] layers = fenceCache[mask];
        if (layers == null) {
            layers = composeFence(mask);
            fenceCache[mask] = layers;
        }
        return layers;
    }

    private Image[] composeFence(int mask) {
        final var post = image(FENCE_POST);
        if (post == null) {
            return NO_IMAGES;
        }
        final java.util.List<Image> layers = new java.util.ArrayList<>(5);
        addRail(layers, mask, FenceConnections.UPPER_LEFT, RAIL_UPPER_LEFT);
        addRail(layers, mask, FenceConnections.UPPER_RIGHT, RAIL_UPPER_RIGHT);
        layers.add(post);
        addRail(layers, mask, FenceConnections.LOWER_LEFT, RAIL_LOWER_LEFT);
        addRail(layers, mask, FenceConnections.LOWER_RIGHT, RAIL_LOWER_RIGHT);
        return layers.toArray(new Image[0]);
    }

    private void addRail(java.util.List<Image> layers, int mask, int bit, int rail) {
        if ((mask & bit) != 0) {
            final var image = image(FENCE_RAILS[rail]);
            if (image != null) {
                layers.add(image);
            }
        }
    }

    /**
     * Gives the images a building casts its shadow from, in its present state: a fence casts its post and its rails as
     * separate silhouettes, any other piece one silhouette. The flame pieces cast their first frame.
     *
     * @param building the building
     * @return the images, empty when the piece has no art and casts the ellipse of its type
     */
    public Image[] shadowImages(Building building) {
        if (building instanceof Fence fence) {
            return fenceLayers(fence.getConnections());
        }
        final String name = building.getType() == BuildingType.CAMPFIRE || building.getType() == BuildingType.STANDING_TORCH
                ? restingName(building.getType(), 0) : spriteName(building, 0);
        final var image = image(name);
        return image != null ? new Image[]{image} : NO_IMAGES;
    }

    private static final Image[] NO_IMAGES = new Image[0];

    private Image image(String name) {
        if (cache.containsKey(name)) {
            return cache.get(name);
        }
        final var image = loader.apply(name);
        if (image == null) {
            log.debug("No building art {}, the procedural shape is drawn", name);
        }
        cache.put(name, image);
        return image;
    }

    /**
     * Gives the screen X of the left edge of a building image.
     *
     * @param centerX  screen X of the tile centre
     * @param widthArt image width in art pixels
     * @param artScale screen pixels per art pixel
     * @return the left edge
     */
    public static double drawX(double centerX, int widthArt, double artScale) {
        return centerX - widthArt * artScale / CENTERING_DIVISOR;
    }

    /**
     * Gives the screen Y of the top edge of a building image: its bottom edge lies {@value #TILE_CENTER_ART_PIXELS}
     * art pixels below the tile centre, on the bottom vertex of the tile.
     *
     * @param centerY   screen Y of the tile centre
     * @param heightArt image height in art pixels
     * @param artScale  screen pixels per art pixel
     * @return the top edge
     */
    public static double drawY(double centerY, int heightArt, double artScale) {
        return centerY + (TILE_CENTER_ART_PIXELS - heightArt) * artScale;
    }

    private static Image loadFromClasspath(String name) {
        final String path = FOLDER + name + ".png";
        try (InputStream is = BuildingSprites.class.getResourceAsStream(path)) {
            if (is == null) {
                return null;
            }
            final var image = new Image(is);
            if (image.isError()) {
                log.warn("Building art could not be decoded: {}", path, image.getException());
                return null;
            }
            return image;
        } catch (Exception e) {
            log.warn("Error loading building art {}", path, e);
            return null;
        }
    }
}
