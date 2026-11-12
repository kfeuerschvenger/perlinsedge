package com.feuerschvenger.perlinsedge.domain.entities.items;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;

import java.util.EnumMap;
import java.util.Map;

/**
 * The one table that says what the item held in hand does: how much damage it deals to each kind of resource, how
 * quickly it can swing at one, and how hard it hits an enemy. Every number lives here, so balancing the tools is
 * editing this table and the resource health in {@code AppConfig}, and nothing else.
 * <p>
 * A bare hand and any item that is not listed, such as a stone or a potion, act as a hand. A tool that is listed but
 * has no entry for a resource acts as a hand on it, so a sword swung at a tree is a slow hand, not a tool.
 * <p>
 * Damage per hit, and the hits it takes with the resource health in {@code AppConfig}:
 * <pre>
 *                    health   hand (4)        axe (10)       pickaxe
 *   TREE               30     8 hits, slow    3 hits         hand
 *   STONE              30     8 hits, slow    hand           10 dmg, 3 hits
 *   IRON_ORE           36     needs pickaxe   needs pickaxe  12 dmg, 3 hits
 *   CRYSTALS           25     needs pickaxe   needs pickaxe  9 dmg, 3 hits
 * </pre>
 * A hit with the right tool lands every {@value #TOOL_COOLDOWN_MS} ms and a hand swings every
 * {@value #HAND_COOLDOWN_MS} ms, so a bare hand takes about three times the hits at half the pace.
 * Against enemies a hand and a pickaxe deal {@value #HAND_COMBAT_DAMAGE}, the axe 12 and the sword twice as much as a
 * hand. Only the item in the hotbar slot held in hand counts: a tool elsewhere in the hotbar or the inventory does nothing.
 */
public final class ToolStats {
    /** Milliseconds between two hits on the same resource with the right tool. */
    public static final long TOOL_COOLDOWN_MS = 300;
    /** Milliseconds between two hits on the same resource with a hand or a tool that does not suit it. */
    public static final long HAND_COOLDOWN_MS = 600;
    /** Damage a hand deals to a resource per hit. */
    public static final int HAND_RESOURCE_DAMAGE = 4;
    /** Damage a hand deals to an enemy per hit. */
    public static final double HAND_COMBAT_DAMAGE = 10.0;

    /**
     * What one tool does.
     *
     * @param resourceDamage damage per hit on each resource the tool suits, a resource that is absent is hit as by a hand
     * @param combatDamage   damage per hit on an enemy
     */
    private record Profile(Map<ResourceType, Integer> resourceDamage, double combatDamage) { }

    private static final Map<ItemType, Profile> PROFILES = new EnumMap<>(ItemType.class);

    static {
        PROFILES.put(ItemType.AXE, new Profile(Map.of(ResourceType.TREE, 10), 12.0));
        PROFILES.put(ItemType.PICKAXE, new Profile(Map.of(
                ResourceType.STONE, 10,
                ResourceType.IRON_ORE, 12,
                ResourceType.CRYSTALS, 9), HAND_COMBAT_DAMAGE));
        PROFILES.put(ItemType.SWORD, new Profile(Map.of(), HAND_COMBAT_DAMAGE * 2));
    }

    /** Not instantiable: the class only holds the table. */
    private ToolStats() {
    }

    /**
     * Tells whether a tool is the right one for a resource, which is what gives the fast hit and the full damage.
     *
     * @param tool   the item held in hand, null for a bare hand
     * @param target the resource
     * @return true when the tool has an entry for the resource
     */
    public static boolean isRightTool(ItemType tool, ResourceType target) {
        final Profile profile = tool != null ? PROFILES.get(tool) : null;
        return profile != null && profile.resourceDamage().containsKey(target);
    }

    /**
     * Gives the damage one hit deals to a resource.
     *
     * @param tool   the item held in hand, null for a bare hand
     * @param target the resource
     * @return the damage of the tool when it suits the resource, the damage of a hand otherwise
     */
    public static int resourceDamage(ItemType tool, ResourceType target) {
        return isRightTool(tool, target) ? PROFILES.get(tool).resourceDamage().get(target) : HAND_RESOURCE_DAMAGE;
    }

    /**
     * Gives how long a resource that was just hit stays safe from the next hit.
     *
     * @param tool   the item held in hand, null for a bare hand
     * @param target the resource
     * @return {@value #TOOL_COOLDOWN_MS} for the right tool, {@value #HAND_COOLDOWN_MS} otherwise
     */
    public static long resourceCooldownMs(ItemType tool, ResourceType target) {
        return isRightTool(tool, target) ? TOOL_COOLDOWN_MS : HAND_COOLDOWN_MS;
    }

    /**
     * Gives the number of hits it takes to break a fresh resource.
     *
     * @param tool   the item held in hand, null for a bare hand
     * @param target the resource
     * @return the hits, rounded up
     */
    public static int hitsToBreak(ItemType tool, ResourceType target) {
        final int health = AppConfig.getInstance().gameMechanics().getResourceBaseHealth(target);
        return (int) Math.ceil(health / (double) resourceDamage(tool, target));
    }

    /**
     * Gives the damage one hit deals to an enemy.
     *
     * @param tool the item held in hand, null for a bare hand
     * @return the damage of the tool when it is listed, the damage of a hand otherwise
     */
    public static double combatDamage(ItemType tool) {
        final Profile profile = tool != null ? PROFILES.get(tool) : null;
        return profile != null ? profile.combatDamage() : HAND_COMBAT_DAMAGE;
    }
}
