package com.feuerschvenger.perlinsedge.infra.fx.utils;

import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

import java.util.List;

/**
 * Classpath locations of the art and the rules that map game types to them.
 * The layout is the one the pixel art tool writes: tiles, resources by biome, items, entities and gui.
 */
public final class AssetPaths {

    private static final String BASE = "/assets/";

    /** The biome folder that holds art shared by every map type. */
    public static final String SHARED_FOLDER = "shared";

    /** The biome folder used when a map type has no art of its own for a resource. */
    public static final String DEFAULT_FOLDER = "default";

    /** The sheet with the player frames. */
    public static final String PLAYER_SHEET = BASE + "entities/player.png";

    /** The strip with the slime frames. */
    public static final String SLIME_STRIP = BASE + "entities/slime-1.png";

    /** The full heart icon. */
    public static final String HEART_FULL = BASE + "gui/heart-full.png";

    /** The half heart icon. */
    public static final String HEART_HALF = BASE + "gui/heart-half.png";

    /** The empty heart icon. */
    public static final String HEART_EMPTY = BASE + "gui/heart-empty.png";

    /** Rows of art pixels a water tile sinks below the land level, so its bottom edge lines up with the land faces. */
    public static final int WATER_SINK_PIXELS = 2;

    private AssetPaths() {}

    /**
     * Returns the art name of a tile type.
     *
     * @param type the tile type
     * @return the name, for example grass, or null when the type has no art and keeps the procedural drawing
     */
    public static String tileArt(TileType type) {
        return switch (type) {
            case GRASS -> "grass";
            case FOREST -> "forest";
            case SAND, BEACH -> "sand";
            case HILL -> "hill";
            case SHALLOW_WATER -> "water-shallow";
            case DEEP_WATER -> "water-deep";
            default -> null;
        };
    }

    /**
     * Returns the path of one variant of a tile art.
     *
     * @param art     the art name from {@link #tileArt}
     * @param variant the one-based variant number
     * @return the classpath location
     */
    public static String tile(String art, int variant) {
        return BASE + "tiles/" + art + "-" + variant + ".png";
    }

    /**
     * Returns the path of an item icon.
     *
     * @param type the item type
     * @return the classpath location
     */
    public static String item(ItemType type) {
        return BASE + "items/" + type.getSpritePath() + ".png";
    }

    /**
     * Returns the folder with the resource art of a map type.
     *
     * @param mapType the map type
     * @return the folder name under resources
     */
    public static String biomeFolder(MapType mapType) {
        return switch (mapType) {
            case DARK_FOREST -> "dark_forest";
            case SWAMP -> "swamp";
            case DESERT -> "desert";
            case TUNDRA -> "tundra";
            default -> DEFAULT_FOLDER;
        };
    }

    /**
     * Returns the art kinds that draw a resource type.
     * Every harvestable tree, bush, fern and cactus is a TREE resource.
     *
     * @param type the resource type
     * @return the kinds, the file name without its variant number
     */
    public static List<String> resourceKinds(ResourceType type) {
        return switch (type) {
            case TREE -> List.of("tree", "willow", "palm", "pine", "cactus", "bush", "shrub", "fern", "reed");
            case STONE -> List.of("rock");
            case CRYSTALS -> List.of("crystal");
            case IRON_ORE -> List.of("iron");
        };
    }

    /**
     * Returns the path of one variant of a resource art.
     *
     * @param folder  the biome folder
     * @param kind    the art kind from {@link #resourceKinds}
     * @param variant the one-based variant number
     * @return the classpath location
     */
    public static String resource(String folder, String kind, int variant) {
        return BASE + "resources/" + folder + "/" + kind + "-" + variant + ".png";
    }
}
