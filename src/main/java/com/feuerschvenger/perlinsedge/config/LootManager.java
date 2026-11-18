package com.feuerschvenger.perlinsedge.config;

import com.feuerschvenger.perlinsedge.domain.entities.enemies.Mimic;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Skeleton;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Slime;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages loot generation for resources and enemies using predefined loot tables.
 * Implements the Singleton pattern to ensure a single instance manages all loot configurations.
 */
public final class LootManager {
    // Quantity ranges of the resource drops
    private static final int TREE_LOG_MIN = 4;
    private static final int TREE_LOG_MAX = 6;
    private static final int TREE_FIBER_MAX = 2;
    private static final int TREE_STICK_MAX = 4;
    private static final int CRYSTALS_STONE_MAX = 2;
    private static final int CRYSTALS_SHARD_MAX = 3;
    private static final int ROCK_STONE_MAX = 4;
    private static final int ROCK_FLINT_MAX = 3;
    private static final int IRON_DEPOSIT_ORE_MAX = 3;
    private static final int IRON_DEPOSIT_STONE_MAX = 2;

    // Quantity ranges of the enemy drops
    private static final int SLIME_BALL_MAX = 3;
    private static final int SKELETON_BONE_MIN = 2;
    private static final int SKELETON_BONE_MAX = 4;
    private static final int MIMIC_CRYSTALS_MIN = 3;
    private static final int MIMIC_CRYSTALS_MAX = 10;
    private static final int MIMIC_POTION_MAX = 3;

    // Singleton instance
    private static final LootManager INSTANCE = new LootManager();

    // Loot tables configuration
    private final Map<ResourceType, List<LootEntry>> resourceLootTables = new EnumMap<>(ResourceType.class);
    private final Map<Class<?>, List<LootEntry>> enemyLootTables = new HashMap<>();

    /**
     * Private constructor to enforce Singleton pattern.
     * Initializes loot tables during construction.
     */
    private LootManager() {
        initializeResourceLootTables();
        initializeEnemyLootTables();
    }

    /**
     * Retrieves the Singleton instance of LootManager.
     *
     * @return The single instance of LootManager
     */
    public static LootManager getInstance() {
        return INSTANCE;
    }

    // Public API -----------------------------------------------------------------

    /**
     * Generates loot items for a specific resource type.
     *
     * @param resourceType The type of resource to generate loot for
     * @return List of generated items (may be empty)
     */
    public List<Item> generateLootForResource(ResourceType resourceType) {
        List<LootEntry> entries = resourceLootTables.getOrDefault(resourceType, Collections.emptyList());
        return generateLootItems(entries);
    }

    /**
     * Generates loot items for a specific enemy class.
     *
     * @param enemyClass The class of enemy to generate loot for
     * @return List of generated items (may be empty)
     */
    public List<Item> generateLootForEnemy(Class<?> enemyClass) {
        List<LootEntry> entries = enemyLootTables.getOrDefault(enemyClass, Collections.emptyList());
        return generateLootItems(entries);
    }

    // Loot Table Initialization --------------------------------------------------

    /**
     * Initializes loot tables for all resource types.
     */
    private void initializeResourceLootTables() {
        // Tree resource loot
        resourceLootTables.put(ResourceType.TREE, Arrays.asList(
                new LootEntry(ItemType.WOOD_LOG, TREE_LOG_MIN, TREE_LOG_MAX),
                new LootEntry(ItemType.FIBER, 0, TREE_FIBER_MAX),
                new LootEntry(ItemType.STICK, 0, TREE_STICK_MAX)
        ));

        // Crystals resource loot
        resourceLootTables.put(ResourceType.CRYSTALS, Arrays.asList(
                new LootEntry(ItemType.STONE, 0, CRYSTALS_STONE_MAX),
                new LootEntry(ItemType.CRYSTALS, 1, CRYSTALS_SHARD_MAX),
                new LootEntry(ItemType.REFINED_CRYSTALS, 0, 1)
        ));

        // Stone resource loot
        resourceLootTables.put(ResourceType.STONE, Arrays.asList(
                new LootEntry(ItemType.STONE, 1, ROCK_STONE_MAX),
                new LootEntry(ItemType.FLINT, 0, ROCK_FLINT_MAX),
                new LootEntry(ItemType.COAL, 0, 1)
        ));

        // Iron ore resource loot
        resourceLootTables.put(ResourceType.IRON_ORE, Arrays.asList(
                new LootEntry(ItemType.IRON_ORE, 1, IRON_DEPOSIT_ORE_MAX),
                new LootEntry(ItemType.STONE, 0, IRON_DEPOSIT_STONE_MAX)
        ));
    }

    /**
     * Initializes loot tables for all enemy types.
     */
    private void initializeEnemyLootTables() {
        // Slime enemy loot
        enemyLootTables.put(Slime.class, Arrays.asList(
                new LootEntry(ItemType.SLIME_BALL, 1, SLIME_BALL_MAX)
        ));

        // Skeleton enemy loot
        enemyLootTables.put(Skeleton.class, Arrays.asList(
                new LootEntry(ItemType.BONE, SKELETON_BONE_MIN, SKELETON_BONE_MAX)
        ));

        // Mimic enemy loot
        enemyLootTables.put(Mimic.class, Arrays.asList(
                new LootEntry(ItemType.REFINED_CRYSTALS, MIMIC_CRYSTALS_MIN, MIMIC_CRYSTALS_MAX),
                new LootEntry(ItemType.POTION_HEALTH, 1, MIMIC_POTION_MAX)
        ));
    }

    // Loot Generation Logic ------------------------------------------------------

    /**
     * Generates items from a list of loot entries.
     *
     * @param entries List of loot entries to process
     * @return Generated items with randomized quantities
     */
    private static List<Item> generateLootItems(List<LootEntry> entries) {
        List<Item> lootItems = new ArrayList<>();
        var random = ThreadLocalRandom.current();

        for (LootEntry entry : entries) {
            var quantity = random.nextInt(entry.minQuantity, entry.maxQuantity + 1);
            if (quantity > 0) {
                lootItems.add(new Item(entry.itemType, quantity));
            }
        }
        return lootItems;
    }

    // Loot Entry Definition ------------------------------------------------------

    /**
     * Represents an entry in a loot table defining possible item drops.
     * Immutable to ensure consistency in loot configuration.
     */
    public record LootEntry(ItemType itemType, int minQuantity, int maxQuantity) {

        /**
         * Creates a new loot table entry.
         *
         * @param itemType    The type of item to drop
         * @param minQuantity Minimum quantity to drop (inclusive)
         * @param maxQuantity Maximum quantity to drop (inclusive)
         * @throws IllegalArgumentException If quantities are invalid
         */
        public LootEntry {
            validateQuantities(minQuantity, maxQuantity);

        }

        /**
         * Validates quantity parameters.
         *
         * @param min Minimum quantity
         * @param max Maximum quantity
         * @throws IllegalArgumentException If min < 0 or max < min
         */
        private static void validateQuantities(int min, int max) {
            if (min < 0) {
                throw new IllegalArgumentException("Min quantity cannot be negative");
            }
            if (max < min) {
                throw new IllegalArgumentException("Max quantity cannot be less than min quantity");
            }
        }

    }

}
