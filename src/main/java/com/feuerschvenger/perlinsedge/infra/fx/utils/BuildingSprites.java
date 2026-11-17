package com.feuerschvenger.perlinsedge.infra.fx.utils;

import com.feuerschvenger.perlinsedge.domain.entities.buildings.Building;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
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
 * {@code anvil}, {@code workbench}, {@code bed} and {@code dock}, and {@code bridge_0} and {@code bridge_1}. The
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

    private final Function<String, Image> loader;
    private final Map<String, Image> cache = new HashMap<>();

    /** The lazily created instance that reads the classpath. */
    private static final class InstanceHolder {
        static final BuildingSprites INSTANCE = new BuildingSprites(BuildingSprites::loadFromClasspath);
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
    private static final String[] FURNACE_NAMES = new String[4];

    static {
        for (BuildingType type : BuildingType.values()) {
            final String base = type.name().toLowerCase(Locale.ROOT);
            final String[] names = new String[type.rotationCount()];
            for (int i = 0; i < names.length; i++) {
                names[i] = type.isRotatable() ? base + "_" + i : base;
            }
            NAMES.put(type, names);
            final String[] frames = new String[FLAME_FRAMES];
            for (int i = 0; i < FLAME_FRAMES; i++) {
                frames[i] = base + "_" + i;
            }
            FLAME_NAMES.put(type, frames);
        }
        for (int i = 0; i < FURNACE_NAMES.length; i++) {
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
    private Image image(String name) {
        if (cache.containsKey(name)) {
            return cache.get(name);
        }
        final Image image = loader.apply(name);
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
        return centerX - widthArt * artScale / 2.0;
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
            final Image image = new Image(is);
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
