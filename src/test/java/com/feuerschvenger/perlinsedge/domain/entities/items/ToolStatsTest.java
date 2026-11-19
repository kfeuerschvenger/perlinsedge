package com.feuerschvenger.perlinsedge.domain.entities.items;

import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolStatsTest {

    @Test
    void theRightToolBreaksATreeAStoneIronOreAndCrystalsInThreeHits() {
        assertEquals(3, ToolHits.hitsToBreak(ItemType.AXE, ResourceType.TREE));
        assertEquals(3, ToolHits.hitsToBreak(ItemType.PICKAXE, ResourceType.STONE));
        assertEquals(3, ToolHits.hitsToBreak(ItemType.PICKAXE, ResourceType.IRON_ORE));
        assertEquals(3, ToolHits.hitsToBreak(ItemType.PICKAXE, ResourceType.CRYSTALS));
    }

    @Test
    void aBareHandTakesAboutThreeTimesTheHitsOnATreeAndAStone() {
        for (ResourceType type : new ResourceType[]{ResourceType.TREE, ResourceType.STONE}) {
            int hand = ToolHits.hitsToBreak(null, type);
            int tool = ToolHits.hitsToBreak(type == ResourceType.TREE ? ItemType.AXE : ItemType.PICKAXE, type);

            assertTrue(hand >= tool * 2.5 && hand <= tool * 3.5, type + ": " + hand + " hits by hand against " + tool);
        }
    }

    @Test
    void aBareHandSwingsSlowerThanTheRightTool() {
        assertEquals(300, ToolStats.resourceCooldownMs(ItemType.AXE, ResourceType.TREE));
        assertEquals(300, ToolStats.resourceCooldownMs(ItemType.PICKAXE, ResourceType.STONE));
        assertEquals(600, ToolStats.resourceCooldownMs(null, ResourceType.TREE));
    }

    @Test
    void aWrongToolCountsAsAHand() {
        assertFalse(ToolStats.isRightTool(ItemType.AXE, ResourceType.STONE));
        assertEquals(ToolStats.resourceDamage(null, ResourceType.STONE),
                ToolStats.resourceDamage(ItemType.AXE, ResourceType.STONE));
        assertEquals(ToolStats.resourceCooldownMs(null, ResourceType.STONE),
                ToolStats.resourceCooldownMs(ItemType.AXE, ResourceType.STONE));
        assertEquals(ToolHits.hitsToBreak(null, ResourceType.TREE),
                ToolHits.hitsToBreak(ItemType.PICKAXE, ResourceType.TREE));
        assertEquals(ToolHits.hitsToBreak(null, ResourceType.TREE),
                ToolHits.hitsToBreak(ItemType.SWORD, ResourceType.TREE));
    }

    @Test
    void anItemThatIsNoToolActsAsAHand() {
        assertFalse(ToolStats.isRightTool(ItemType.STONE, ResourceType.STONE));
        assertEquals(ToolStats.HAND_RESOURCE_DAMAGE, ToolStats.resourceDamage(ItemType.COAL, ResourceType.TREE));
        assertEquals(ToolStats.HAND_COMBAT_DAMAGE, ToolStats.combatDamage(ItemType.POTION_HEALTH));
        assertEquals(ToolStats.HAND_COMBAT_DAMAGE, ToolStats.combatDamage(null));
    }

    @Test
    void theNeededToolsOfTheResourcesAreTheOnesThatSuitThem() {
        for (ResourceType type : ResourceType.values()) {
            if (type.getRequiredTool() != null) {
                assertTrue(ToolStats.isRightTool(type.getRequiredTool(), type), type.name());
            }
        }
        assertEquals(ItemType.PICKAXE, ResourceType.CRYSTALS.getRequiredTool());
        assertEquals(ItemType.PICKAXE, ResourceType.IRON_ORE.getRequiredTool());
    }

    @Test
    void theIronToolsDoubleTheDamageOfTheirStoneCounterparts() {
        for (ResourceType type : new ResourceType[]{ResourceType.STONE, ResourceType.IRON_ORE, ResourceType.CRYSTALS}) {
            assertEquals(2 * ToolStats.resourceDamage(ItemType.PICKAXE, type),
                    ToolStats.resourceDamage(ItemType.IRON_PICKAXE, type), type.name());
            assertTrue(ToolStats.isRightTool(ItemType.IRON_PICKAXE, type), type.name());
        }
        assertEquals(2 * ToolStats.resourceDamage(ItemType.AXE, ResourceType.TREE),
                ToolStats.resourceDamage(ItemType.IRON_AXE, ResourceType.TREE));
        assertEquals(2 * ToolStats.combatDamage(ItemType.SWORD), ToolStats.combatDamage(ItemType.IRON_SWORD), 1e-9);
        assertEquals(2 * ToolStats.combatDamage(ItemType.AXE), ToolStats.combatDamage(ItemType.IRON_AXE), 1e-9);
        assertEquals(ToolStats.resourceCooldownMs(ItemType.AXE, ResourceType.TREE),
                ToolStats.resourceCooldownMs(ItemType.IRON_AXE, ResourceType.TREE));
        assertFalse(ToolStats.isRightTool(ItemType.IRON_AXE, ResourceType.STONE));
    }

    @Test
    void theToolsHaveTheirTierAndTheirStoneBase() {
        assertEquals(ItemType.PICKAXE, ItemType.IRON_PICKAXE.baseTool());
        assertEquals(ItemType.AXE, ItemType.IRON_AXE.baseTool());
        assertEquals(ItemType.SWORD, ItemType.IRON_SWORD.baseTool());
        assertEquals(ItemType.HAMMER, ItemType.HAMMER.baseTool());
        assertEquals(ItemType.IRON_TIER, ItemType.IRON_SWORD.toolTier());
        assertEquals(ItemType.STONE_TIER, ItemType.PICKAXE.toolTier());
        assertEquals(0, ItemType.HAMMER.toolTier());
        assertEquals("Stone axe", ItemType.AXE.getDisplayName());
        assertEquals("Stone pickaxe", ItemType.PICKAXE.getDisplayName());
        assertEquals("Stone sword", ItemType.SWORD.getDisplayName());
        assertEquals("Iron pickaxe", ItemType.IRON_PICKAXE.getDisplayName());
        assertEquals("Charcoal", ItemType.CHARCOAL.getDisplayName());
    }

    @Test
    void everyItemHasADisplayName() {
        for (ItemType type : ItemType.values()) {
            assertNotNull(type.getDisplayName(), type.name());
            assertFalse(type.getDisplayName().isBlank(), type.name());
        }
    }

    @Test
    void onlyAnIronPickaxeCanMineCrystalsWhileAnyPickaxeMinesIronOre() {
        assertFalse(ResourceType.CRYSTALS.canBeHarvestedWith(ItemType.PICKAXE));
        assertTrue(ResourceType.CRYSTALS.canBeHarvestedWith(ItemType.IRON_PICKAXE));
        assertFalse(ResourceType.CRYSTALS.canBeHarvestedWith(ItemType.IRON_AXE));
        assertFalse(ResourceType.CRYSTALS.canBeHarvestedWith(null));
        assertTrue(ResourceType.IRON_ORE.canBeHarvestedWith(ItemType.PICKAXE));
        assertTrue(ResourceType.IRON_ORE.canBeHarvestedWith(ItemType.IRON_PICKAXE));
        assertFalse(ResourceType.IRON_ORE.canBeHarvestedWith(ItemType.IRON_AXE));
        assertTrue(ResourceType.STONE.canBeHarvestedWith(null));
        assertTrue(ResourceType.CRYSTALS.isToolKind(ItemType.PICKAXE));
    }

    @Test
    void theHammerIsTheRightToolForNothingAndHitsLikeABareHand() {
        for (ResourceType type : ResourceType.values()) {
            assertFalse(ToolStats.isRightTool(ItemType.HAMMER, type), type.name());
            assertEquals(ToolStats.resourceDamage(null, type), ToolStats.resourceDamage(ItemType.HAMMER, type));
            assertEquals(ToolStats.resourceCooldownMs(null, type), ToolStats.resourceCooldownMs(ItemType.HAMMER, type));
        }
        assertEquals(ToolStats.combatDamage(null), ToolStats.combatDamage(ItemType.HAMMER), 1e-9);
    }

    @Test
    void theSwordIsTwiceAsStrongAsAHandAndTheAxeIsASlightlyStrongerOne() {
        assertEquals(2 * ToolStats.HAND_COMBAT_DAMAGE, ToolStats.combatDamage(ItemType.SWORD), 1e-9);
        double axe = ToolStats.combatDamage(ItemType.AXE);
        assertTrue(axe > ToolStats.HAND_COMBAT_DAMAGE && axe < ToolStats.combatDamage(ItemType.SWORD));
        assertEquals(ToolStats.HAND_COMBAT_DAMAGE, ToolStats.combatDamage(ItemType.PICKAXE), 1e-9);
    }
}
