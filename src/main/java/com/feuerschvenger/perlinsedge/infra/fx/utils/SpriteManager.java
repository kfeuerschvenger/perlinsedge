package com.feuerschvenger.perlinsedge.infra.fx.utils;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads the pixel art once and hands it out by game type.
 * Art is looked up by name under {@link AssetPaths}, and a variant number that is missing ends the list, so adding a
 * file needs no code change. Anything without art answers null and the caller draws its stand-in shape. Every missing
 * asset is logged once at load, never per frame.
 */
@Slf4j
public final class SpriteManager {
    private static final int MAX_VARIANTS = 99;
    private static final List<String> RESOURCE_FOLDERS = List.of(
            AssetPaths.DEFAULT_FOLDER, "dense_forest", "swamp", "desert", "tundra", AssetPaths.SHARED_FOLDER);

    /**
     * Metadata container for sprites including image data and vertical offset.
     */
    public static final class SpriteMetadata {
        private final Image image;
        private final double verticalOffset;
        private final int dropArtPixels;
        private final int contactArtPixels;

        public SpriteMetadata(Image image, double verticalOffset) {
            this(image, verticalOffset, 0, 0);
        }

        /**
         * Creates the metadata of a resource sprite.
         *
         * @param image            the sprite
         * @param verticalOffset   the offset of an item icon, zero for a resource
         * @param dropArtPixels    how far the sprite is drawn below the default anchor, see {@link ResourceAnchor}
         * @param contactArtPixels how far below the tile centre the lowest painted pixel lies, where the shadow is centred
         */
        public SpriteMetadata(Image image, double verticalOffset, int dropArtPixels, int contactArtPixels) {
            this.image = image;
            this.verticalOffset = verticalOffset;
            this.dropArtPixels = dropArtPixels;
            this.contactArtPixels = contactArtPixels;
        }

        public Image getImage() {
            return image;
        }

        public double getVerticalOffset() {
            return verticalOffset;
        }

        public int getDropArtPixels() {
            return dropArtPixels;
        }

        public int getContactArtPixels() {
            return contactArtPixels;
        }
    }

    // Singleton instance holder
    private static class InstanceHolder {
        static final SpriteManager INSTANCE = new SpriteManager();
    }

    private final Map<MapType, Map<ResourceType, List<SpriteMetadata>>> resourceSprites = new EnumMap<>(MapType.class);
    private final Map<String, List<List<Image>>> entitySprites = new HashMap<>();
    private final Map<TileType, List<Image>> tileSprites = new EnumMap<>(TileType.class);
    private final Map<ItemType, SpriteMetadata> itemSprites = new EnumMap<>(ItemType.class);
    private final Map<String, Image> decalSprites = new HashMap<>();
    private final Map<String, Image> waterSprites = new HashMap<>();
    private List<Image> shimmerLines = List.of();
    private List<Image> slimeFrames = List.of();

    private SpriteManager() {
        loadTileSprites();
        loadDecalSprites();
        loadWaterSprites();
        loadResourceSprites();
        loadEntitySprites();
        loadItemSprites();
    }

    /**
     * Retrieves the singleton instance of SpriteManager.
     *
     * @return The single instance of SpriteManager
     */
    public static SpriteManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    // ==================================================================
    //  LOADING
    // ==================================================================

    private void loadTileSprites() {
        Map<String, List<Image>> byArt = new HashMap<>();
        for (TileType type : TileType.values()) {
            String art = AssetPaths.tileArt(type);
            if (art == null) {
                continue;
            }
            List<Image> variants = byArt.computeIfAbsent(art, this::loadTileVariants);
            if (variants.isEmpty()) {
                log.info("No tile art for {}, the procedural tile is drawn", type);
            } else {
                tileSprites.put(type, variants);
            }
        }
    }

    private void loadDecalSprites() {
        List<String> missing = new ArrayList<>();
        for (String name : GroundDecals.ALL) {
            Image image = loadImage(AssetPaths.decal(name));
            if (image == null) {
                missing.add(name);
            } else {
                decalSprites.put(name, image);
            }
        }
        if (!missing.isEmpty()) {
            log.info("No decal art for {}, those decals are skipped", missing);
        }
    }

    private void loadWaterSprites() {
        List<String> arts = new ArrayList<>();
        for (TileType type : TileType.values()) {
            String art = AssetPaths.waterArt(type);
            if (art != null) {
                arts.add(art);
            }
        }
        arts.add(AssetPaths.DEEP_EDGE_WATER);
        for (String art : arts) {
            Image image = loadImage(AssetPaths.waterTop(art));
            if (image == null) {
                log.info("No water art {}, those tiles are drawn without water", art);
            } else {
                waterSprites.put(art, image);
            }
        }
        List<Image> lines = new ArrayList<>();
        for (int line = 0; line < WaterShimmer.MAX_LINES; line++) {
            Image image = loadImage(AssetPaths.shimmer(line));
            if (image == null) {
                log.info("No shimmer art for line {}, the water is drawn without highlights", line);
                return;
            }
            lines.add(image);
        }
        shimmerLines = List.copyOf(lines);
    }

    private List<Image> loadTileVariants(String art) {
        List<Image> variants = new ArrayList<>();
        for (int variant = 1; variant <= MAX_VARIANTS; variant++) {
            Image image = loadImage(AssetPaths.tile(art, variant));
            if (image == null) {
                break;
            }
            variants.add(image);
        }
        return variants;
    }

    /**
     * Loads the resource art of every biome folder, then resolves for each map type the list its resources draw from:
     * the biome folder first, then the default folder, then the shared one.
     */
    private void loadResourceSprites() {
        Map<String, Map<ResourceType, List<SpriteMetadata>>> byFolder = new HashMap<>();
        for (String folder : RESOURCE_FOLDERS) {
            Map<ResourceType, List<SpriteMetadata>> perType = new EnumMap<>(ResourceType.class);
            for (ResourceType type : ResourceType.values()) {
                perType.put(type, loadResourceVariants(folder, type));
            }
            byFolder.put(folder, perType);
        }

        List<String> withoutArt = new ArrayList<>();
        for (MapType mapType : MapType.values()) {
            String biome = AssetPaths.biomeFolder(mapType);
            Map<ResourceType, List<SpriteMetadata>> resolved = new EnumMap<>(ResourceType.class);
            for (ResourceType type : ResourceType.values()) {
                List<SpriteMetadata> list = firstNotEmpty(
                        byFolder.get(biome).get(type),
                        byFolder.get(AssetPaths.DEFAULT_FOLDER).get(type),
                        byFolder.get(AssetPaths.SHARED_FOLDER).get(type));
                resolved.put(type, list);
                if (list.isEmpty()) {
                    withoutArt.add(mapType + "/" + type);
                }
            }
            resourceSprites.put(mapType, resolved);
        }
        if (!withoutArt.isEmpty()) {
            log.info("No resource art for {}, stand-in shapes are drawn", withoutArt);
        }
    }

    @SafeVarargs
    private static List<SpriteMetadata> firstNotEmpty(List<SpriteMetadata>... candidates) {
        for (List<SpriteMetadata> candidate : candidates) {
            if (!candidate.isEmpty()) {
                return candidate;
            }
        }
        return List.of();
    }

    private List<SpriteMetadata> loadResourceVariants(String folder, ResourceType type) {
        List<SpriteMetadata> variants = new ArrayList<>();
        for (String kind : AssetPaths.resourceKinds(type)) {
            for (int variant = 1; variant <= MAX_VARIANTS; variant++) {
                Image image = loadImage(AssetPaths.resource(folder, kind, variant));
                if (image == null) {
                    break;
                }
                variants.add(new SpriteMetadata(image, 0.0,
                        ResourceAnchor.dropArtPixels(kind, bottomPadding(image)), ResourceAnchor.contactArtPixels(kind)));
            }
        }
        return variants;
    }

    /** Counts the fully transparent rows under the lowest painted pixel of an image. */
    private static int bottomPadding(Image image) {
        final PixelReader reader = image.getPixelReader();
        final int width = (int) image.getWidth();
        return ResourceAnchor.bottomPadding((int) image.getHeight(), row -> {
            for (int x = 0; x < width; x++) {
                if ((reader.getArgb(x, row) >>> 24) != 0) {
                    return false;
                }
            }
            return true;
        });
    }

    private void loadEntitySprites() {
        AppConfig.PlayerConfig.SpriteSheetConfig config = AppConfig.getInstance().player().spriteSheet;
        Image sheet = loadImage(config.path);
        if (sheet == null) {
            log.info("No player sheet at {}, the stand-in circle is drawn", config.path);
        } else if (sheet.getWidth() < config.startX + config.columns * config.columnOffset
                || sheet.getHeight() < config.rowYCoordinates[config.rows - 1] + config.spriteHeight) {
            log.warn("Player sheet {} is {}x{}, too small for {} columns by {} rows, the stand-in circle is drawn",
                    config.path, sheet.getWidth(), sheet.getHeight(), config.columns, config.rows);
        } else {
            entitySprites.put("player", extractSpritesheetFrames(sheet, config));
        }

        Image strip = loadImage(AssetPaths.SLIME_STRIP);
        if (strip == null) {
            log.info("No slime strip at {}, the stand-in oval is drawn", AssetPaths.SLIME_STRIP);
            return;
        }
        int frameWidth = (int) (strip.getWidth() / SpriteLayout.SLIME_FRAMES);
        List<Image> frames = new ArrayList<>();
        for (int i = 0; i < SpriteLayout.SLIME_FRAMES; i++) {
            frames.add(new WritableImage(strip.getPixelReader(), i * frameWidth, 0, frameWidth, (int) strip.getHeight()));
        }
        slimeFrames = List.copyOf(frames);
    }

    private void loadItemSprites() {
        List<String> missing = new ArrayList<>();
        for (ItemType type : ItemType.values()) {
            String path = AssetPaths.item(type);
            Image image = loadImage(path);
            itemSprites.put(type, new SpriteMetadata(image, 0.0));
            if (image == null) {
                missing.add(type.name());
            }
        }
        if (!missing.isEmpty()) {
            log.info("{} item icons have no art, stand-in shapes are drawn", missing.size());
            log.debug("Items without art: {}", missing);
        }
    }

    // ==================================================================
    //  PUBLIC ACCESS METHODS
    // ==================================================================

    /**
     * Retrieves the tile sprite for a tile, choosing the variant from its coordinates.
     *
     * @param type the tile type
     * @param x    the tile column
     * @param y    the tile row
     * @return the sprite, or null when the type has no art
     */
    public Image getTileSprite(TileType type, int x, int y) {
        List<Image> variants = tileSprites.get(type);
        if (variants == null) {
            return null;
        }
        return variants.get(SpriteVariants.forTile(x, y, variants.size()));
    }

    /**
     * Retrieves a decal sprite.
     *
     * @param name the decal name, see {@link GroundDecals}
     * @return the sprite, or null when the name is null or has no art
     */
    public Image getDecalSprite(String name) {
        return name == null ? null : decalSprites.get(name);
    }

    /**
     * Retrieves a translucent water layer.
     *
     * @param art the water art name, see {@link WaterTones#artFor}
     * @return the top diamond to draw over the bed, or null when the name is null or has no art
     */
    public Image getWaterSprite(String art) {
        return art == null ? null : waterSprites.get(art);
    }

    /**
     * Retrieves the sprite of one shimmer line.
     *
     * @param line the line index, below {@link WaterShimmer#MAX_LINES}
     * @return the sprite, or null when there is no shimmer art
     */
    public Image getShimmerSprite(int line) {
        return line >= 0 && line < shimmerLines.size() ? shimmerLines.get(line) : null;
    }

    /**
     * Retrieves a resource sprite.
     *
     * @param mapType Current map type
     * @param resourceType Resource type
     * @param x Tile column of the resource
     * @param y Tile row of the resource
     * @param worldSeed Seed of the world, which with the coordinates fixes the variant
     * @return Sprite metadata or null if not found
     */
    public SpriteMetadata getResourceSprite(MapType mapType, ResourceType resourceType, int x, int y, long worldSeed) {
        Map<ResourceType, List<SpriteMetadata>> mapSprites = resourceSprites.get(mapType);
        if (mapSprites == null) {
            return null;
        }
        List<SpriteMetadata> variants = mapSprites.get(resourceType);
        if (variants == null || variants.isEmpty()) {
            return null;
        }
        return variants.get(SpriteVariants.forResource(worldSeed, x, y, variants.size()));
    }

    /**
     * Retrieves an entity sprite frame.
     *
     * @param entityKey Entity identifier ("player")
     * @param row Animation row (walk a, idle, walk b)
     * @param col Sheet column, see {@link SpriteLayout#playerColumn}
     * @return Requested sprite frame, or null when the entity has no art
     */
    public Image getEntitySprite(String entityKey, int row, int col) {
        List<List<Image>> frames = entitySprites.get(entityKey);
        if (frames == null || row < 0 || row >= frames.size()) {
            return null;
        }
        List<Image> rowFrames = frames.get(row);
        return (col >= 0 && col < rowFrames.size()) ? rowFrames.get(col) : null;
    }

    /**
     * Retrieves one frame of the slime strip.
     *
     * @param frame the frame, see {@link SpriteLayout#slimeFrame}
     * @return the frame, or null when there is no slime art
     */
    public Image getSlimeFrame(int frame) {
        return (frame >= 0 && frame < slimeFrames.size()) ? slimeFrames.get(frame) : null;
    }

    /**
     * Retrieves sprite metadata for an item type.
     *
     * @param itemType The type of item
     * @return SpriteMetadata with image and offset, the image is null when the item has no art
     */
    public SpriteMetadata getItemSprite(ItemType itemType) {
        return itemSprites.getOrDefault(itemType, new SpriteMetadata(null, 0));
    }

    // ==================================================================
    //  SPRITE LOADING HELPERS
    // ==================================================================

    private List<List<Image>> extractSpritesheetFrames(Image sheet, AppConfig.PlayerConfig.SpriteSheetConfig config) {
        List<List<Image>> frames = new ArrayList<>();
        PixelReader reader = sheet.getPixelReader();

        for (int row = 0; row < config.rows; row++) {
            int y = config.rowYCoordinates[row];
            List<Image> rowFrames = new ArrayList<>();
            for (int col = 0; col < config.columns; col++) {
                int x = config.startX + (col * config.columnOffset);
                rowFrames.add(new WritableImage(reader, x, y, config.spriteWidth, config.spriteHeight));
            }
            frames.add(rowFrames);
        }
        return frames;
    }

    /**
     * Loads an image from the classpath.
     *
     * @return the image, or null when the resource does not exist or cannot be decoded
     */
    private Image loadImage(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                log.debug("Asset not found: {}", path);
                return null;
            }
            Image image = new Image(is);
            if (image.isError()) {
                log.warn("Asset could not be decoded: {}", path, image.getException());
                return null;
            }
            return image;
        } catch (Exception e) {
            log.warn("Error loading asset {}", path, e);
            return null;
        }
    }

}
