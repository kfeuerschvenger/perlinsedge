package com.feuerschvenger.perlinsedge.domain.entities.items;

import java.util.Locale;

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
            "A chunk of fuel. Burns hot and long, " +
                    "your campfire will thank you."
    ),
    CHARCOAL(
            64,
            "Wood burnt down to a light, hot fuel. " +
                    "Smells like a good barbecue."
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
    RAW_MEAT(
            64,
            "Fresh meat. Cook it soon, unless you're starting " +
                    "a very peculiar collection.",
            5
    ),
    COOKED_MEAT(
            64,
            "Juicy, savory, and very satisfying after " +
                    "a long day of exploring.",
            20
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
            "Perfect for chopping wood, " +
                    "or anything else that needs chopping."
    ),
    SWORD(
            1,
            "A trusted companion for adventurers. " +
                    "Sharp, strong, and always ready."
    ),
    IRON_PICKAXE(
            1,
            "Iron where the stone one gives up: " +
                    "it hits twice as hard and even breaks crystals."
    ),
    IRON_AXE(
            1,
            "Chops twice as fast as the stone axe, " +
                    "and so does everything else it hits."
    ),
    IRON_SWORD(
            1,
            "Forged iron with a proper edge. " +
                    "Hits twice as hard as the stone sword."
    ),
    HAMMER(
            1,
            "Hold it to raise fences, chests and workbenches, " +
                    "and to take them down again."
    ),
    POTION_HEALTH(
            8,
            "A magical brew for when things go wrong, " +
                    "or when a dragon disagrees.",
            50
    );

    /** Tier of the tools made of stone. */
    public static final int STONE_TIER = 1;
    /** Tier of the tools made of iron. */
    public static final int IRON_TIER = 2;

    // ==================================================================
    //  Instance Properties
    // ==================================================================
    private final int maxStackSize;
    private final String spritePath;
    private final String description;
    private final int healAmount;

    // ==================================================================
    //  Constructor
    // ==================================================================

    /**
     * Creates a new item type that cannot be used up.
     *
     * @param maxStackSize Maximum number of items per stack (1 for non-stackable)
     * @param description  Flavor text explaining the item's purpose and characteristics
     */
    ItemType(int maxStackSize, String description) {
        this(maxStackSize, description, 0);
    }

    /**
     * Creates a new item type definition.
     *
     * @param maxStackSize Maximum number of items per stack (1 for non-stackable)
     * @param description  Flavor text explaining the item's purpose and characteristics
     * @param healAmount   Health restored when one item is used, 0 for an item that cannot be used
     */
    ItemType(int maxStackSize, String description, int healAmount) {
        this.maxStackSize = maxStackSize;
        this.description = description;
        this.healAmount = healAmount;

        // Generate sprite path from enum constant
        this.spritePath = name().toLowerCase(Locale.ROOT);
    }

    // ==================================================================
    //  Naming and tool tiers
    // ==================================================================

    /**
     * Holds the display names by the ordinal of the constant, built once when the holder is first used. Init order:
     * the enum is fully initialised before this class is initialised, because the holder is only touched from
     * {@link #getDisplayName()}, so {@code values()} is complete when the table is built. A name cannot be built in
     * the constructor of the constants instead, since the switch that reads the constants runs before they all exist.
     */
    private static final class DisplayNames {
        private static final String[] BY_ORDINAL = build();

        private DisplayNames() {
        }

        private static String[] build() {
            final var values = ItemType.values();
            final var names = new String[values.length];
            for (var i = 0; i < values.length; i++) {
                names[i] = displayNameOf(values[i]);
            }
            return names;
        }

        /**
         * Builds the display name of a constant: the name in sentence case, with the plain tools called after the
         * stone they are made of, since the iron tier has constants of its own.
         */
        private static String displayNameOf(ItemType type) {
            final var name = type.name();
            final var generated = name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT).replace("_", " ");
            return switch (type) {
                case AXE, PICKAXE, SWORD -> "Stone " + generated.toLowerCase(Locale.ROOT);
                case WOOD_LOG, WOOD_PLANK, STONE, STONE_BRICK, IRON_ORE, IRON_INGOT, CRYSTALS, REFINED_CRYSTALS, COAL,
                     CHARCOAL, FIBER, SLIME_BALL, STICK, LEATHER, BONE, FLINT, RAW_MEAT, COOKED_MEAT, TORCH,
                     IRON_PICKAXE, IRON_AXE, IRON_SWORD, HAMMER, POTION_HEALTH -> generated;
            };
        }
    }

    /**
     * Gives the tool of the stone tier this tool is a better version of, which is what decides what the tool is used
     * for: an iron pickaxe is a pickaxe wherever a pickaxe is asked for.
     *
     * @return the stone tier tool, or the item itself when it is not an iron tool
     */
    public ItemType baseTool() {
        return switch (this) {
            case IRON_PICKAXE -> PICKAXE;
            case IRON_AXE -> AXE;
            case IRON_SWORD -> SWORD;
            default -> this;
        };
    }

    /**
     * Gives how good the tool is as a tool of its kind.
     *
     * @return {@value #IRON_TIER} for the iron tools, {@value #STONE_TIER} for the stone ones and 0 for any other item
     */
    public int toolTier() {
        return switch (this) {
            case IRON_PICKAXE, IRON_AXE, IRON_SWORD -> IRON_TIER;
            case PICKAXE, AXE, SWORD -> STONE_TIER;
            default -> 0;
        };
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
        return DisplayNames.BY_ORDINAL[ordinal()];
    }

    /** @return Descriptive text explaining the item's purpose */
    public String getDescription() {
        return description;
    }

    /** @return Health restored when one item is used, 0 for an item that cannot be used */
    public int getHealAmount() {
        return healAmount;
    }

    /** @return true when using one item restores health and uses the item up */
    public boolean isConsumable() {
        return healAmount > 0;
    }

}
