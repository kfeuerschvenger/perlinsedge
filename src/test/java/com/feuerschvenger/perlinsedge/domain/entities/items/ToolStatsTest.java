package com.feuerschvenger.perlinsedge.domain.entities.items;

import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolStatsTest {

    @Test
    void theRightToolBreaksATreeAStoneIronOreAndCrystalsInThreeHits() {
        assertEquals(3, ToolStats.hitsToBreak(ItemType.AXE, ResourceType.TREE));
        assertEquals(3, ToolStats.hitsToBreak(ItemType.PICKAXE, ResourceType.STONE));
        assertEquals(3, ToolStats.hitsToBreak(ItemType.PICKAXE, ResourceType.IRON_ORE));
        assertEquals(3, ToolStats.hitsToBreak(ItemType.PICKAXE, ResourceType.CRYSTALS));
    }

    @Test
    void aBareHandTakesAboutThreeTimesTheHitsOnATreeAndAStone() {
        for (ResourceType type : new ResourceType[]{ResourceType.TREE, ResourceType.STONE}) {
            int hand = ToolStats.hitsToBreak(null, type);
            int tool = ToolStats.hitsToBreak(type == ResourceType.TREE ? ItemType.AXE : ItemType.PICKAXE, type);

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
        assertEquals(ToolStats.hitsToBreak(null, ResourceType.TREE),
                ToolStats.hitsToBreak(ItemType.PICKAXE, ResourceType.TREE));
        assertEquals(ToolStats.hitsToBreak(null, ResourceType.TREE),
                ToolStats.hitsToBreak(ItemType.SWORD, ResourceType.TREE));
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
    void theSwordIsTwiceAsStrongAsAHandAndTheAxeIsASlightlyStrongerOne() {
        assertEquals(2 * ToolStats.HAND_COMBAT_DAMAGE, ToolStats.combatDamage(ItemType.SWORD), 1e-9);
        double axe = ToolStats.combatDamage(ItemType.AXE);
        assertTrue(axe > ToolStats.HAND_COMBAT_DAMAGE && axe < ToolStats.combatDamage(ItemType.SWORD));
        assertEquals(ToolStats.HAND_COMBAT_DAMAGE, ToolStats.combatDamage(ItemType.PICKAXE), 1e-9);
    }
}
