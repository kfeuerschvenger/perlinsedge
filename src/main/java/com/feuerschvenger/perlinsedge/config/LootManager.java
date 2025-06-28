package com.feuerschvenger.perlinsedge.config;

import com.feuerschvenger.perlinsedge.domain.entities.enemies.Mimic;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Skeleton;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Slime;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class LootManager {
    private static final LootManager instance = new LootManager();

    public static LootManager getInstance() {
        return instance;
    }

    private final Map<ResourceType, List<LootEntry>> resourceLootTables = new HashMap<>();
    private final Map<Class<?>, List<LootEntry>> enemyLootTables = new HashMap<>();

    private LootManager() {
        initializeResourceLootTables();
        initializeEnemyLootTables();
    }

    private void initializeResourceLootTables() {

        // Tree loot
        resourceLootTables.put(ResourceType.TREE, List.of(
                new LootEntry(ItemType.WOOD_LOG, 4, 6),
                new LootEntry(ItemType.FIBER, 0, 2),
                new LootEntry(ItemType.STICK, 0, 4)
        ));

        // Crystals loot
        resourceLootTables.put(ResourceType.CRYSTALS, List.of(
                new LootEntry(ItemType.STONE, 0, 2),
                new LootEntry(ItemType.CRYSTALS, 1, 3),
                new LootEntry(ItemType.REFINED_CRYSTALS, 0, 1)
        ));

        // Stone loot
        resourceLootTables.put(ResourceType.STONE, List.of(
                new LootEntry(ItemType.STONE, 1, 4),
                new LootEntry(ItemType.FLINT, 0, 3),
                new LootEntry(ItemType.COAL, 0, 1)
        ));

    }

    private void initializeEnemyLootTables() {

        // Slime drops
        enemyLootTables.put(Slime.class, List.of(
                new LootEntry(ItemType.SLIME_BALL, 1, 3)
        ));

        // Skeleton drops
        enemyLootTables.put(Skeleton.class, List.of(
                new LootEntry(ItemType.BONE, 2, 4),
                new LootEntry(ItemType.ARROW, 0, 5)
        ));

        // Mimic drops
        enemyLootTables.put(Mimic.class, List.of(
                new LootEntry(ItemType.REFINED_CRYSTALS, 3, 10),
                new LootEntry(ItemType.POTION_HEALTH, 1, 3)
        ));

    }

    public List<Item> generateLootForResource(ResourceType resourceType) {
        List<LootEntry> entries = resourceLootTables.getOrDefault(resourceType, Collections.emptyList());
        return generateLoot(entries);
    }

    public List<Item> generateLootForEnemy(Class<?> enemyClass) {
        List<LootEntry> entries = enemyLootTables.getOrDefault(enemyClass, Collections.emptyList());
        return generateLoot(entries);
    }

    private List<Item> generateLoot(List<LootEntry> entries) {
        List<Item> drops = new ArrayList<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (LootEntry entry : entries) {
            int quantity = random.nextInt(entry.minQuantity, entry.maxQuantity + 1);
            if (quantity > 0) {
                drops.add(new Item(entry.itemType, quantity));
            }
        }
        return drops;
    }

    public static class LootEntry {
        private final ItemType itemType;
        private final int minQuantity;
        private final int maxQuantity;

        public LootEntry(ItemType itemType, int minQuantity, int maxQuantity) {
            validateQuantities(minQuantity, maxQuantity);
            this.itemType = itemType;
            this.minQuantity = minQuantity;
            this.maxQuantity = maxQuantity;
        }

        private void validateQuantities(int min, int max) {
            if (min < 0 || max < min) {
                throw new IllegalArgumentException("Invalid quantities for " + itemType);
            }
        }
    }

}