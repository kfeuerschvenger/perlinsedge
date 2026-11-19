package com.feuerschvenger.perlinsedge.domain.entities.items;

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
 * hand. An iron tool does the damage of its stone counterpart twice, on resources and on
 * enemies alike, and counts as that counterpart for every resource that needs one. Only the item in the hotbar slot held in hand counts: a tool elsewhere in the hotbar or the inventory does nothing.
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

    /** How many times the damage of a stone tool an iron tool of the same kind deals. */
    private static final int IRON_DAMAGE_FACTOR = 2;

    /** Damage per hit of each tool on the resources it suits. */
    private static final int AXE_TREE_DAMAGE = 10;
    private static final int PICKAXE_STONE_DAMAGE = 10;
    private static final int PICKAXE_IRON_ORE_DAMAGE = 12;
    private static final int PICKAXE_CRYSTALS_DAMAGE = 9;
    /** Damage per hit of an axe on an enemy. */
    private static final double AXE_COMBAT_DAMAGE = 12.0;
    /** How many times a hand a sword hits harder on an enemy. */
    private static final double SWORD_COMBAT_DAMAGE_FACTOR = 2.0;

    private static final Map<ItemType, Profile> PROFILES = new EnumMap<>(ItemType.class);

    static {
        PROFILES.put(ItemType.AXE, new Profile(Map.of(ResourceType.TREE, AXE_TREE_DAMAGE), AXE_COMBAT_DAMAGE));
        PROFILES.put(ItemType.PICKAXE, new Profile(Map.of(
                ResourceType.STONE, PICKAXE_STONE_DAMAGE,
                ResourceType.IRON_ORE, PICKAXE_IRON_ORE_DAMAGE,
                ResourceType.CRYSTALS, PICKAXE_CRYSTALS_DAMAGE), HAND_COMBAT_DAMAGE));
        PROFILES.put(ItemType.SWORD, new Profile(Map.of(), HAND_COMBAT_DAMAGE * SWORD_COMBAT_DAMAGE_FACTOR));
        // The hammer builds, it does not harvest or fight: it suits no resource and hits like a hand
        PROFILES.put(ItemType.HAMMER, new Profile(Map.of(), HAND_COMBAT_DAMAGE));
    }

    /** Not instantiable: the class only holds the table. */
    private ToolStats() {
    }

    /**
     * Tells whether a tool is the right one for a resource, which is what gives the fast hit and the full damage.
     * The damage and the cooldown below build on it, and the tests read it directly.
     *
     * @param tool   the item held in hand, null for a bare hand
     * @param target the resource
     * @return true when the tool has an entry for the resource
     */
    static boolean isRightTool(ItemType tool, ResourceType target) {
        final var profile = profileOf(tool);
        return profile != null &&profile.resourceDamage().containsKey(target);
    }

    /** The profile of a tool, which for an iron tool is the one of its stone counterpart, or null for a hand or a non tool. */
    private static Profile profileOf(ItemType tool) {
        return tool != null ? PROFILES.get(tool.baseTool()) : null;
    }

    /** The factor an iron tool multiplies the damage of its stone counterpart by, 1 for every other item. */
    private static int tierFactor(ItemType tool) {
        return tool != null && tool.toolTier() == ItemType.IRON_TIER ? IRON_DAMAGE_FACTOR : 1;
    }

    /**
     * Gives the damage one hit deals to a resource.
     *
     * @param tool   the item held in hand, null for a bare hand
     * @param target the resource
     * @return the damage of the tool when it suits the resource, the damage of a hand otherwise
     */
    public static int resourceDamage(ItemType tool, ResourceType target) {
        return isRightTool(tool, target)
                ? profileOf(tool).resourceDamage().get(target) * tierFactor(tool)
                : HAND_RESOURCE_DAMAGE;
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
     * Gives the damage one hit deals to an enemy.
     *
     * @param tool the item held in hand, null for a bare hand
     * @return the damage of the tool when it is listed, the damage of a hand otherwise
     */
    public static double combatDamage(ItemType tool) {
        final var profile = profileOf(tool);
        return profile != null ?profile.combatDamage() * tierFactor(tool) : HAND_COMBAT_DAMAGE;
    }
}
