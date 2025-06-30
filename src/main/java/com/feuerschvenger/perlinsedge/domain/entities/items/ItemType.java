package com.feuerschvenger.perlinsedge.domain.entities.items;

/**
 * Defines all item types available in the game with their properties.
 * Each item type specifies:
 * - Maximum stack size
 * - Display name
 * - Sprite resource path
 * - Descriptive text
 *
 * Items are categorized by their functional purpose for better organization.
 */
public enum ItemType {
    // ==================================================================
    //  Raw Materials
    // ==================================================================
    WOOD_LOG(
            64,
            "Wood log, a basic material for crafting and building. " +
                    "It also looks nice stacked next to the fire pit."
    ),
    WOOD_PLANK(
            64,
            "A smooth plank of wood, ideal for making walls, floors, " +
                    "and that rustic table you always wanted."
    ),
    STONE(
            64,
            "A solid chunk of stone. Good for making tools, walls, " +
                    "or making a statement."
    ),
    STONE_BRICK(
            64,
            "A sturdier stone block, built for castles and " +
                    "cozy hearths alike."
    ),
    IRON_ORE(
            64,
            "Fresh from the earth. Smelt it down for strength, " +
                    "or just admire its rustic charm."
    ),
    IRON_INGOT(
            64,
            "Forged from ore, this bar of iron is the backbone " +
                    "of tools and weapons."
    ),
    CRYSTALS(
            64,
            "Shiny and mysterious. Might be magical, " +
                    "or just very pretty."
    ),
    REFINED_CRYSTALS(
            64,
            "Polished and glowing. Definitely magical, " +
                    "or a very fancy paperweight."
    ),
    COAL(
            64,
            "A chunk of fuel. Burns hot and long — " +
                    "your campfire will thank you."
    ),
    FIBER(
            64,
            "Soft and flexible threads. Perfect for ropes, " +
                    "cloth, and making friends."
    ),
    SLIME_BALL(
            64,
            "A wobbly, sticky prize from a defeated slime. " +
                    "Try not to drop it."
    ),
    STICK(
            64,
            "A humble stick. Sometimes it's a weapon, " +
                    "sometimes it's kindling. Always handy."
    ),
    LEATHER(
            64,
            "Tough and reliable. Excellent for armor, " +
                    "saddles, and fancy pants."
    ),
    BONE(
            64,
            "What remains of fallen creatures. " +
                    "Might be sharp, might be creepy."
    ),
    FLINT(
            64,
            "A sharp stone, ideal for making tools " +
                    "or starting a cozy campfire."
    ),

    // ==================================================================
    //  Food Items
    // ==================================================================
    RAW_FISH(
            64,
            "Fresh from the lake. Might be tasty when cooked—" +
                    "or a cat's best friend."
    ),
    COOKED_FISH(
            64,
            "Cooked to perfection. A delicious and " +
                    "filling treat for long adventures."
    ),
    RAW_MEAT(
            64,
            "Fresh meat. Cook it soon, unless you're starting " +
                    "a very peculiar collection."
    ),
    COOKED_MEAT(
            64,
            "Juicy, savory, and very satisfying after " +
                    "a long day of exploring."
    ),

    // ==================================================================
    //  Tools & Equipment
    // ==================================================================
    TORCH(
            64,
            "Light up the darkness, ward off monsters, " +
                    "or just decorate your cave."
    ),
    PICKAXE(
            1,
            "Break stone, dig deep, and find treasures " +
                    "hidden in the earth."
    ),
    AXE(
            1,
            "Perfect for chopping wood—" +
                    "or anything else that needs chopping."
    ),
    SWORD(
            1,
            "A trusted companion for adventurers. " +
                    "Sharp, strong, and always ready."
    ),
    SHOVEL(
            1,
            "For when you really need to dig a hole—" +
                    "or bury a secret."
    ),
    BOW(
            1,
            "Silent and deadly, a hunter's best friend."
    ),
    ARROW(
            64,
            "Pointy and precise. Goes great with bows " +
                    "and unlucky enemies."
    ),
    POTION_HEALTH(
            8,
            "A magical brew for when things go wrong—" +
                    "or when a dragon disagrees."
    );

    // ==================================================================
    //  Instance Properties
    // ==================================================================
    private final int maxStackSize;
    private final String displayName;
    private final String spritePath;
    private final String description;

    // ==================================================================
    //  Constructor
    // ==================================================================

    /**
     * Creates a new item type definition.
     *
     * @param maxStackSize Maximum number of items per stack (1 for non-stackable)
     * @param description  Flavor text explaining the item's purpose and characteristics
     */
    ItemType(int maxStackSize, String description) {
        this.maxStackSize = maxStackSize;
        this.description = description;

        // Generate display name from enum constant
        String name = name();
        this.displayName = name.charAt(0) +
                name.substring(1).toLowerCase().replace("_", " ");

        // Generate sprite path from enum constant
        this.spritePath = name.toLowerCase();
    }

    // ==================================================================
    //  Getters
    // ==================================================================

    /** @return Maximum number of items that can be stacked together */
    public int getMaxStackSize() {
        return maxStackSize;
    }

    /** @return Resource path for the item's sprite image */
    public String getSpritePath() {
        return spritePath;
    }

    /** @return Human-readable name for UI display */
    public String getDisplayName() {
        return displayName;
    }

    /** @return Descriptive text explaining the item's purpose */
    public String getDescription() {
        return description;
    }

}