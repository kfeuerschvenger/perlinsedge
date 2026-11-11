package com.feuerschvenger.perlinsedge.infra.fx.utils;

import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    /** The art of the one translucent sheet every water tile is covered with. */
    public static final String WATER_SHEET = "sheet";

    /** The grassy land types, which look wrong on a map whose ground is sand or snow. */
    private static final List<TileType> GRASSY = List.of(
            TileType.GRASS, TileType.FOREST, TileType.SAVANNA, TileType.HILL, TileType.SWAMP);

    /** The tile art a map type draws in place of the shared art of a tile type. */
    private static final Map<MapType, Map<TileType, String>> BIOME_TILE_ART = Map.of(
            MapType.DESERT, overrides("dune"),
            MapType.TUNDRA, overrides("snow"));

    private AssetPaths() {}

    private static Map<TileType, String> overrides(String art) {
        final Map<TileType, String> map = new EnumMap<>(TileType.class);
        for (TileType type : GRASSY) {
            map.put(type, art);
        }
        return Map.copyOf(map);
    }

    /**
     * Returns the art name of a tile type.
     *
     * @param type the tile type
     * @return the name, for example grass; water types answer the sunk sand block under their water, fogged more for deep water
     */
    public static String tileArt(TileType type) {
        return switch (type) {
            case GRASS -> "grass";
            case FOREST -> "forest";
            case SAVANNA -> "savanna";
            case HILL -> "hill";
            case SAND, BEACH -> "sand";
            case SHALLOW_WATER, RIVER, COAST -> "bed";
            case DEEP_WATER -> "bed-deep";
            case MOUNTAIN -> "mountain";
            case SNOW -> "snow";
            case ICE -> "ice";
            case SWAMP -> "swamp";
        };
    }

    /**
     * Returns the art name of a tile type on a map type.
     * Some biomes draw their grassy land in the ground of the biome: sand dunes in the desert and snow in the tundra.
     * The overrides are data in {@link #BIOME_TILE_ART}, so a biome needs no code to change what a type looks like.
     *
     * @param mapType the map type, or null for the art every map shares
     * @param type    the tile type
     * @return the art name
     */
    public static String tileArt(MapType mapType, TileType type) {
        final Map<TileType, String> overrides = mapType == null ? null : BIOME_TILE_ART.get(mapType);
        final String override = overrides == null ? null : overrides.get(type);
        return override != null ? override : tileArt(type);
    }

    /**
     * Returns every tile art name a map type or the shared rules can draw, for the loader.
     *
     * @return the distinct names
     */
    public static Set<String> allTileArts() {
        final Set<String> arts = new LinkedHashSet<>();
        for (TileType type : TileType.values()) {
            arts.add(tileArt(type));
        }
        for (Map<TileType, String> overrides : BIOME_TILE_ART.values()) {
            arts.addAll(overrides.values());
        }
        return arts;
    }

    /**
     * Returns the name of the translucent water layer drawn over the sand bed of a water tile.
     * Every water type shares one sheet, the depth shows in the bed.
     *
     * @param type the tile type
     * @return {@link #WATER_SHEET}, or null when the type is not water
     */
    public static String waterArt(TileType type) {
        return type.isWater() ? WATER_SHEET : null;
    }

    /**
     * Returns the path of the face strip of a tile art, the part of its side faces below the face of the tile sprite.
     *
     * @param art the art name from {@link #tileArt}
     * @return the classpath location
     */
    public static String faceStrip(String art) {
        return BASE + "faces/" + art + ".png";
    }

    /**
     * Returns the path of a translucent water layer.
     *
     * @param art the water art name from {@link #waterArt}
     * @return the classpath location
     */
    public static String waterTop(String art) {
        return BASE + "water/" + art + ".png";
    }

    /**
     * Returns the path of one shimmer line sprite.
     *
     * @param line the zero-based line index, below {@link WaterShimmer#MAX_LINES}
     * @return the classpath location
     */
    public static String shimmer(int line) {
        return BASE + "water/shimmer-" + (line + 1) + ".png";
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
     * Returns the path of a decal sprite.
     *
     * @param name the decal name, for example tuft-1
     * @return the classpath location
     */
    public static String decal(String name) {
        return BASE + "decals/" + name + ".png";
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
            case DENSE_FOREST -> "dense_forest";
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
