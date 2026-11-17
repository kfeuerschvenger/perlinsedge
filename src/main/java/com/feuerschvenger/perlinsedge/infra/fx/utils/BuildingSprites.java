package com.feuerschvenger.perlinsedge.infra.fx.utils;

import com.feuerschvenger.perlinsedge.domain.entities.buildings.Building;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Door;
import javafx.scene.image.Image;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Loads the building art once and hands it out by building. The files live in {@code assets/buildings} and are named
 * after the piece: {@code wall}, {@code fence}, {@code chest}, {@code anvil}, {@code workbench}, {@code bed},
 * {@code door_closed}, {@code door_open}, {@code bridge}, {@code dock}, {@code furnace_off}, {@code furnace_on},
 * and the four frames {@code campfire_0} to {@code campfire_3} and {@code standing_torch_0} to
 * {@code standing_torch_3}. A piece with no file answers null, and the caller draws the procedural shape instead.
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
     * Gives the file name, without extension, of the art of a building in its present state.
     *
     * @param building the building
     * @param frame    the animation frame, used by the pieces that animate
     * @return the name of the art
     */
    public static String spriteName(Building building, int frame) {
        return switch (building.getType()) {
            case DOOR -> building instanceof Door door && door.isOpen() ? "door_open" : "door_closed";
            // The furnace has no lit state yet, so it is always shown cold
            case FURNACE -> "furnace_off";
            case CAMPFIRE, STANDING_TORCH -> building.getType().name().toLowerCase() + "_"
                    + Math.floorMod(frame, FLAME_FRAMES);
            default -> building.getType().name().toLowerCase();
        };
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
     * Gives the art of a type in its resting state, which is what the palette and the placement ghost show.
     *
     * @param type the building type
     * @return the image, or null when the piece has no art
     */
    public Image restingImage(BuildingType type) {
        return switch (type) {
            case DOOR -> image("door_closed");
            case FURNACE -> image("furnace_off");
            case CAMPFIRE, STANDING_TORCH -> image(type.name().toLowerCase() + "_0");
            default -> image(type.name().toLowerCase());
        };
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
