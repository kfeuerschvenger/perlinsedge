package com.feuerschvenger.perlinsedge.infra.fx.utils;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.io.InputStream;
import java.util.*;

/**
 * Centralized sprite management system for loading, caching, and providing access
 * to game assets with proper positioning offsets and fallback mechanisms.
 */
public final class SpriteManager {
    private static final String ASSETS_BASE_PATH = "/assets/";
    private static final String DEFAULT_MAP_TYPE = "default";
    private static final double DEFAULT_SPRITE_SIZE = 32.0;
    private static final Color FALLBACK_COLOR = Color.RED;
    private static final Color MISSING_SPRITE_COLOR = Color.GRAY;

    /**
     * Metadata container for sprites including image data and vertical offset.
     */
    public static final class SpriteMetadata {
        private final Image image;
        private final double verticalOffset;

        public SpriteMetadata(Image image, double verticalOffset) {
            this.image = image;
            this.verticalOffset = verticalOffset;
        }

        public Image getImage() {
            return image;
        }

        public double getVerticalOffset() {
            return verticalOffset;
        }
    }

    // Singleton instance holder
    private static class InstanceHolder {
        static final SpriteManager INSTANCE = new SpriteManager();
    }

    private final Map<MapType, Map<ResourceType, List<SpriteMetadata>>> resourceSprites;
    private final Map<String, List<List<Image>>> entitySprites;
    private final EnumMap<ItemType, SpriteMetadata> itemSprites;

    private SpriteManager() {
        this.resourceSprites = new EnumMap<>(MapType.class);
        this.entitySprites = new HashMap<>();
        this.itemSprites = new EnumMap<>(ItemType.class);
        initializeSpriteSystem();
    }

    /**
     * Retrieves the singleton instance of SpriteManager.
     *
     * @return The single instance of SpriteManager
     */
    public static SpriteManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Initializes the sprite loading system.
     */
    private void initializeSpriteSystem() {
        loadResourceSprites();
        loadEntitySprites();
        loadItemSprites();
    }

    /**
     * Loads all resource sprites organized by map type.
     */
    private void loadResourceSprites() {
        // Common sprites for most map types
        Map<ResourceType, List<SpriteMetadata>> commonSprites = createCommonResourceSprites();

        // Map-specific sprite configurations
        resourceSprites.put(MapType.DARK_FOREST, commonSprites);
        resourceSprites.put(MapType.RIVERS, commonSprites);
        resourceSprites.put(MapType.MEDITERRANEAN, commonSprites);
        resourceSprites.put(MapType.ISLANDS, commonSprites);
        resourceSprites.put(MapType.DESERT, createDesertResourceSprites());
        resourceSprites.put(MapType.SWAMP, createSwampResourceSprites());
        resourceSprites.put(MapType.TUNDRA, createTundraResourceSprites());
    }

    /**
     * Creates common resource sprites shared by multiple map types.
     */
    private Map<ResourceType, List<SpriteMetadata>> createCommonResourceSprites() {
        Map<ResourceType, List<SpriteMetadata>> sprites = new EnumMap<>(ResourceType.class);

        addResourceVariants(
                sprites, ResourceType.TREE, DEFAULT_MAP_TYPE,
                List.of("Tree", "Autumn_tree", "Burned_tree", "Christmas_tree"),
                List.of(3, 3, 3, 3),
                List.of(-10.0, -10.0, -10.0, -10.0)
        );

        addResourceSprites(
                sprites, ResourceType.STONE, DEFAULT_MAP_TYPE,
                "Rock1_", 5, "_no_shadow", -5.0
        );

        addResourceSprites(
                sprites, ResourceType.CRYSTALS, DEFAULT_MAP_TYPE,
                "Yellow_crystal", 4, null, -5.0
        );

        return sprites;
    }

    /**
     * Creates desert-specific resource sprites.
     */
    private Map<ResourceType, List<SpriteMetadata>> createDesertResourceSprites() {
        Map<ResourceType, List<SpriteMetadata>> sprites = new EnumMap<>(ResourceType.class);

        addResourceVariants(
                sprites, ResourceType.TREE, "desert",
                List.of("Cactus1_", "Cactus2_", "Palm_tree1_", "Palm_tree2_"),
                List.of(3, 3, 3, 3),
                List.of(-5.0, -5.0, -10.0, -10.0)
        );

        addResourceSprites(
                sprites, ResourceType.STONE, "desert",
                "Rock6_", 5, "_no_shadow", -5.0
        );

        addResourceSprites(
                sprites, ResourceType.CRYSTALS, "desert",
                "Red_crystal", 4, null, -5.0
        );

        return sprites;
    }

    /**
     * Creates swamp-specific resource sprites.
     */
    private Map<ResourceType, List<SpriteMetadata>> createSwampResourceSprites() {
        Map<ResourceType, List<SpriteMetadata>> sprites = new EnumMap<>(ResourceType.class);

        addResourceVariants(
                sprites, ResourceType.TREE, "swamp",
                List.of("Curved_tree", "Moss_tree", "White_tree", "Willow"),
                List.of(3, 3, 2, 3),
                List.of(-10.0, -10.0, -10.0, -10.0)
        );

        addResourceSprites(
                sprites, ResourceType.STONE, "swamp",
                "Rock4_", 5, "_no_shadow", -5.0
        );

        addResourceSprites(
                sprites, ResourceType.CRYSTALS, "swamp",
                "Yellow-green_crystal", 4, null, -5.0
        );

        return sprites;
    }

    /**
     * Creates tundra-specific resource sprites.
     */
    private Map<ResourceType, List<SpriteMetadata>> createTundraResourceSprites() {
        Map<ResourceType, List<SpriteMetadata>> sprites = new EnumMap<>(ResourceType.class);

        addResourceVariants(
                sprites, ResourceType.TREE, "tundra",
                List.of("Snow_christmass_tree", "Snow_tree"),
                List.of(3, 3),
                List.of(-10.0, -10.0)
        );

        addResourceSprites(
                sprites, ResourceType.STONE, "tundra",
                "Rokc3_snow_shadow_dark", 5, null, -5.0
        );

        addResourceSprites(
                sprites, ResourceType.CRYSTALS, "tundra",
                "White_crystal", 4, null, -5.0
        );

        return sprites;
    }

    /**
     * Loads entity sprites from configuration.
     */
    private void loadEntitySprites() {
        AppConfig.PlayerConfig.SpriteSheetConfig config =
                AppConfig.getInstance().player().spriteSheet;

        try (InputStream is = getResourceStream(config.path)) {
            if (is == null) {
                throw new IllegalStateException("Player spritesheet not found: " + config.path);
            }

            Image playerSheet = new Image(is);
            validateSpritesheetDimensions(playerSheet, config);
            entitySprites.put("player", extractSpritesheetFrames(playerSheet, config));
        } catch (Exception e) {
            System.out.println("Error loading player spritesheet: " + config.path);
            entitySprites.put("player", createFallbackFrames(config));
        }
    }

    /**
     * Loads all item sprites.
     */
    private void loadItemSprites() {
        String itemsBasePath = ASSETS_BASE_PATH + "entities/items/";

        for (ItemType type : ItemType.values()) {
            String fullPath = itemsBasePath + type.getSpritePath() + ".png";
            itemSprites.put(type, loadImageMetadata(fullPath, 0.0));
        }
    }

    // ==================================================================
    //  PUBLIC ACCESS METHODS
    // ==================================================================

    /**
     * Retrieves a resource sprite with proper vertical offset.
     *
     * @param mapType Current map type
     * @param resourceType Resource type
     * @param visualSeed Seed for consistent variant selection
     * @return Sprite metadata or null if not found
     */
    public SpriteMetadata getResourceSprite(MapType mapType, ResourceType resourceType, long visualSeed) {
        Map<ResourceType, List<SpriteMetadata>> mapSprites = resourceSprites.getOrDefault(
                mapType, resourceSprites.get(MapType.DARK_FOREST)
        );

        if (mapSprites == null) return null;

        List<SpriteMetadata> variants = mapSprites.get(resourceType);
        if (variants == null || variants.isEmpty()) return null;

        return variants.get(new Random(visualSeed).nextInt(variants.size()));
    }

    /**
     * Retrieves an entity sprite frame.
     *
     * @param entityKey Entity identifier ("player")
     * @param row Animation row (direction)
     * @param col Animation column (frame)
     * @return Requested sprite frame or fallback if not found
     */
    public Image getEntitySprite(String entityKey, int row, int col) {
        List<List<Image>> frames = entitySprites.get(entityKey);
        if (frames == null) {
            System.out.println("Entity sprite key not found: " + entityKey);
            return createSolidColorImage(DEFAULT_SPRITE_SIZE, DEFAULT_SPRITE_SIZE, MISSING_SPRITE_COLOR);
        }

        if (row < 0 || row >= frames.size()) {
            return createSolidColorImage(DEFAULT_SPRITE_SIZE, DEFAULT_SPRITE_SIZE, MISSING_SPRITE_COLOR);
        }

        List<Image> rowFrames = frames.get(row);
        return (col >= 0 && col < rowFrames.size()) ?
                rowFrames.get(col) :
                createSolidColorImage(DEFAULT_SPRITE_SIZE, DEFAULT_SPRITE_SIZE, MISSING_SPRITE_COLOR);
    }

    /**
     * Retrieves sprite metadata for an item type.
     *
     * @param itemType The type of item
     * @return SpriteMetadata with image and offset
     */
    public SpriteMetadata getItemSprite(ItemType itemType) {
        return itemSprites.getOrDefault(itemType, new SpriteMetadata(null, 0));
    }

    // ==================================================================
    //  SPRITE LOADING HELPERS
    // ==================================================================

    /**
     * Validates spritesheet dimensions against configuration.
     */
    private void validateSpritesheetDimensions(Image sheet, AppConfig.PlayerConfig.SpriteSheetConfig config) {
        double minWidth = config.startX + (config.columns * config.spriteWidth);
        double minHeight = config.rowYCoordinates[config.rows - 1] + config.spriteHeight;

        if (sheet.getWidth() < minWidth || sheet.getHeight() < minHeight) {
            System.out.printf("Spritesheet too small: Required %.0fx%.0f, Actual %.0fx%.0f%n",
                    minWidth, minHeight, sheet.getWidth(), sheet.getHeight());
        }
    }

    /**
     * Extracts frames from a spritesheet.
     */
    private List<List<Image>> extractSpritesheetFrames(Image sheet, AppConfig.PlayerConfig.SpriteSheetConfig config) {
        List<List<Image>> frames = new ArrayList<>();
        PixelReader reader = sheet.getPixelReader();

        for (int row = 0; row < config.rows; row++) {
            int y = config.rowYCoordinates[row];
            List<Image> rowFrames = new ArrayList<>();

            for (int col = 0; col < config.columns; col++) {
                int x = config.startX + (col * config.columnOffset);
                rowFrames.add(extractFrame(reader, x, y, config.spriteWidth, config.spriteHeight));
            }
            frames.add(rowFrames);
        }
        return frames;
    }

    /**
     * Extracts a single frame from a spritesheet.
     */
    private Image extractFrame(PixelReader reader, int x, int y, int width, int height) {
        try {
            return new WritableImage(reader, x, y, width, height);
        } catch (Exception e) {
            System.out.printf("Error extracting frame at (%d,%d): %s%n", x, y, e.getMessage());
            return createSolidColorImage(width, height, FALLBACK_COLOR);
        }
    }

    /**
     * Creates fallback frames for missing spritesheets.
     */
    private List<List<Image>> createFallbackFrames(AppConfig.PlayerConfig.SpriteSheetConfig config) {
        List<List<Image>> frames = new ArrayList<>();

        for (int row = 0; row < config.rows; row++) {
            List<Image> rowFrames = new ArrayList<>();

            for (int col = 0; col < config.columns; col++) {
                rowFrames.add(createSolidColorImage(config.spriteWidth, config.spriteHeight, FALLBACK_COLOR));
            }
            frames.add(rowFrames);
        }
        return frames;
    }

    /**
     * Adds multiple sprite variants to a resource type.
     */
    private void addResourceVariants(
            Map<ResourceType, List<SpriteMetadata>> sprites,
            ResourceType type,
            String folder,
            List<String> prefixes,
            List<Integer> counts,
            List<Double> verticalOffsets
    ) {
        List<SpriteMetadata> variants = new ArrayList<>();

        for (int i = 0; i < prefixes.size(); i++) {
            variants.addAll(loadSpriteSet(
                    folder, prefixes.get(i), counts.get(i), null, verticalOffsets.get(i)
            ));
        }
        sprites.put(type, variants);
    }

    /**
     * Adds a set of sprites for a resource type.
     */
    private void addResourceSprites(
            Map<ResourceType, List<SpriteMetadata>> sprites,
            ResourceType type,
            String folder,
            String prefix,
            int count,
            String suffix,
            double verticalOffset
    ) {
        sprites.put(type, loadSpriteSet(folder, prefix, count, suffix, verticalOffset));
    }

    /**
     * Loads a set of sprites from the filesystem.
     */
    private List<SpriteMetadata> loadSpriteSet(String folder, String prefix, int count,
                                               String suffix, double verticalOffset) {
        List<SpriteMetadata> sprites = new ArrayList<>();
        String basePath = "resources/" + folder + "/";

        for (int i = 1; i <= count; i++) {
            String path = ASSETS_BASE_PATH + basePath + prefix + i +
                    (suffix != null ? suffix : "") + ".png";
            sprites.add(loadImageMetadata(path, verticalOffset));
        }
        return sprites;
    }

    /**
     * Loads image metadata from a resource path.
     */
    private SpriteMetadata loadImageMetadata(String fullPath, double verticalOffset) {
        try (InputStream is = getResourceStream(fullPath)) {
            return new SpriteMetadata(is != null ? new Image(is) : null, verticalOffset);
        } catch (Exception e) {
            System.out.println("Error loading sprite: " + fullPath + " - " + e.getMessage());
            return new SpriteMetadata(null, verticalOffset);
        }
    }

    /**
     * Creates a solid color fallback image.
     */
    private Image createSolidColorImage(double width, double height, Color color) {
        int w = (int) Math.max(1, width);
        int h = (int) Math.max(1, height);
        WritableImage img = new WritableImage(w, h);
        PixelWriter writer = img.getPixelWriter();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                writer.setColor(x, y, color);
            }
        }
        return img;
    }

    /**
     * Safely retrieves a resource stream.
     */
    private InputStream getResourceStream(String path) {
        return getClass().getResourceAsStream(path);
    }

}