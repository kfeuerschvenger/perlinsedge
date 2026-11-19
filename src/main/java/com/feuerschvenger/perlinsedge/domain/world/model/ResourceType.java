package com.feuerschvenger.perlinsedge.domain.world.model;

import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;

/**
 * Enum representing different types of resources in the game world.
 */
public enum ResourceType {
    STONE,
    /** Crystals in rocky ground, only an iron pickaxe held in hand can mine them. */
    CRYSTALS(ItemType.PICKAXE, ItemType.IRON_TIER),
    TREE,
    /** Iron ore in rocky ground, only a pickaxe held in hand can mine it. */
    IRON_ORE(ItemType.PICKAXE, ItemType.STONE_TIER);

    private final ItemType requiredTool;
    private final int minimumTier;

    ResourceType() {
        this(null, 0);
    }

    ResourceType(ItemType requiredTool, int minimumTier) {
        this.requiredTool = requiredTool;
        this.minimumTier = minimumTier;
    }

    /**
     * Returns the tool the player must hold in hand to harvest this resource.
     *
     * @return the required tool, or null when the resource can be harvested by hand
     */
    public ItemType getRequiredTool() {
        return requiredTool;
    }

    /**
     * Tells whether an item held in hand is the kind of tool this resource needs, whatever its tier. An iron pickaxe is
     * a pickaxe as much as a stone one.
     *
     * @param held the item in hand, null for a bare hand
     * @return true when the resource needs no tool or the item is a tool of the needed kind
     */
    public boolean isToolKind(ItemType held) {
        return requiredTool == null || (held != null && held.baseTool() == requiredTool);
    }

    /**
     * Tells whether an item held in hand can harvest this resource: it is the tool of the needed kind and its tier
     * reaches the one the resource asks for.
     *
     * @param held the item in hand, null for a bare hand
     * @return true when the resource needs no tool or the item is a strong enough tool of the needed kind
     */
    public boolean canBeHarvestedWith(ItemType held) {
        return requiredTool == null || (isToolKind(held) && held.toolTier() >= minimumTier);
    }

    /**
     * Returns the first letter of the resource type name.
     * @return The first letter of the resource type name.
     */
    public String getInitial() {
        return name().charAt(0) + "";
    }

}
