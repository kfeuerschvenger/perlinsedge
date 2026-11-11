package com.feuerschvenger.perlinsedge.config;

import com.feuerschvenger.perlinsedge.domain.entities.enemies.Mimic;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Slime;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LootManagerTest {

    private final LootManager loot = LootManager.getInstance();

    private static Map<ItemType, Integer> byType(List<Item> items) {
        return items.stream().collect(Collectors.toMap(Item::getType, Item::getQuantity));
    }

    @Test
    void isASingleton() {
        assertSame(LootManager.getInstance(), LootManager.getInstance());
    }

    @RepeatedTest(50)
    void treeDropsAlwaysContainWoodWithinTheTableRange() {
        Map<ItemType, Integer> drops = byType(loot.generateLootForResource(ResourceType.TREE));

        assertTrue(drops.containsKey(ItemType.WOOD_LOG));
        assertTrue(drops.get(ItemType.WOOD_LOG) >= 4 && drops.get(ItemType.WOOD_LOG) <= 6);
        assertTrue(drops.getOrDefault(ItemType.FIBER, 0) <= 2);
        assertTrue(drops.getOrDefault(ItemType.STICK, 0) <= 4);
    }

    @RepeatedTest(50)
    void stoneDropsAlwaysContainStone() {
        Map<ItemType, Integer> drops = byType(loot.generateLootForResource(ResourceType.STONE));

        assertTrue(drops.get(ItemType.STONE) >= 1 && drops.get(ItemType.STONE) <= 4);
        assertTrue(drops.getOrDefault(ItemType.COAL, 0) <= 1);
    }

    @RepeatedTest(50)
    void crystalDropsAlwaysContainCrystals() {
        Map<ItemType, Integer> drops = byType(loot.generateLootForResource(ResourceType.CRYSTALS));

        assertTrue(drops.get(ItemType.CRYSTALS) >= 1 && drops.get(ItemType.CRYSTALS) <= 3);
    }

    @RepeatedTest(50)
    void ironOreDropsAlwaysContainIronOre() {
        Map<ItemType, Integer> drops = byType(loot.generateLootForResource(ResourceType.IRON_ORE));

        assertTrue(drops.get(ItemType.IRON_ORE) >= 1 && drops.get(ItemType.IRON_ORE) <= 3);
        assertTrue(drops.getOrDefault(ItemType.STONE, 0) <= 2);
    }

    @RepeatedTest(50)
    void slimeAlwaysDropsSlimeBalls() {
        Map<ItemType, Integer> drops = byType(loot.generateLootForEnemy(Slime.class));

        assertEquals(Set.of(ItemType.SLIME_BALL), drops.keySet());
        assertTrue(drops.get(ItemType.SLIME_BALL) >= 1 && drops.get(ItemType.SLIME_BALL) <= 3);
    }

    @RepeatedTest(50)
    void mimicAlwaysDropsCrystalsAndPotions() {
        Map<ItemType, Integer> drops = byType(loot.generateLootForEnemy(Mimic.class));

        assertTrue(drops.get(ItemType.REFINED_CRYSTALS) >= 3 && drops.get(ItemType.REFINED_CRYSTALS) <= 10);
        assertTrue(drops.get(ItemType.POTION_HEALTH) >= 1 && drops.get(ItemType.POTION_HEALTH) <= 3);
    }

    @Test
    void generatedItemsAlwaysHavePositiveQuantity() {
        for (int i = 0; i < 200; i++) {
            loot.generateLootForResource(ResourceType.TREE).forEach(item -> assertTrue(item.getQuantity() > 0));
        }
    }

    @Test
    void unknownEnemyClassDropsNothing() {
        assertTrue(loot.generateLootForEnemy(String.class).isEmpty());
    }

    @Test
    void lootEntryValidatesItsRange() {
        assertThrows(IllegalArgumentException.class, () -> new LootManager.LootEntry(ItemType.STONE, -1, 2));
        assertThrows(IllegalArgumentException.class, () -> new LootManager.LootEntry(ItemType.STONE, 3, 2));
        assertEquals(2, new LootManager.LootEntry(ItemType.STONE, 2, 2).minQuantity());
    }
}
