package com.feuerschvenger.perlinsedge.config;

import com.feuerschvenger.perlinsedge.domain.entities.enemies.Mimic;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Skeleton;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Slime;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages loot generation for resources and enemies using predefined loot tables.
 * Implements the Singleton pattern to ensure a single instance manages all loot configurations.
 */
public class LootManager {
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
                new LootEntry(ItemType.WOOD_LOG, 4, 6),
                new LootEntry(ItemType.FIBER, 0, 2),
                new LootEntry(ItemType.STICK, 0, 4)
        ));

        // Crystals resource loot
        resourceLootTables.put(ResourceType.CRYSTALS, Arrays.asList(
                new LootEntry(ItemType.STONE, 0, 2),
                new LootEntry(ItemType.CRYSTALS, 1, 3),
                new LootEntry(ItemType.REFINED_CRYSTALS, 0, 1)
        ));

        // Stone resource loot
        resourceLootTables.put(ResourceType.STONE, Arrays.asList(
                new LootEntry(ItemType.STONE, 1, 4),
                new LootEntry(ItemType.FLINT, 0, 3),
                new LootEntry(ItemType.COAL, 0, 1)
        ));
    }

    /**
     * Initializes loot tables for all enemy types.
     */
    private void initializeEnemyLootTables() {
        // Slime enemy loot
        enemyLootTables.put(Slime.class, Arrays.asList(
                new LootEntry(ItemType.SLIME_BALL, 1, 3)
        ));

        // Skeleton enemy loot
        enemyLootTables.put(Skeleton.class, Arrays.asList(
                new LootEntry(ItemType.BONE, 2, 4),
                new LootEntry(ItemType.ARROW, 0, 5)
        ));

        // Mimic enemy loot
        enemyLootTables.put(Mimic.class, Arrays.asList(
                new LootEntry(ItemType.REFINED_CRYSTALS, 3, 10),
                new LootEntry(ItemType.POTION_HEALTH, 1, 3)
        ));
    }

    // Loot Generation Logic ------------------------------------------------------

    /**
     * Generates items from a list of loot entries.
     *
     * @param entries List of loot entries to process
     * @return Generated items with randomized quantities
     */
    private List<Item> generateLootItems(List<LootEntry> entries) {
        List<Item> lootItems = new ArrayList<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (LootEntry entry : entries) {
            int quantity = random.nextInt(entry.minQuantity, entry.maxQuantity + 1);
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
        private void validateQuantities(int min, int max) {
            if (min < 0) {
                throw new IllegalArgumentException("Min quantity cannot be negative");
            }
            if (max < min) {
                throw new IllegalArgumentException("Max quantity cannot be less than min quantity");
            }
        }

    }

}