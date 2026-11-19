package com.feuerschvenger.perlinsedge.domain.crafting.processing;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;

import java.util.List;

/**
 * The profile of each processing station, read from {@link AppConfig.StationsConfig}.
 * <ul>
 *   <li>The furnace smelts iron ore for a unit of coal or charcoal each, and turns logs into charcoal with no fuel. A
 *   log is always a raw material there, never fuel.</li>
 *   <li>The stone oven cooks meat and burns one log for a number of pieces. A log is always fuel there.</li>
 *   <li>The campfire cooks meat slowly and burns logs with time, whether it cooks or not.</li>
 * </ul>
 */
public final class StationProfiles {
    /** Products one unit of fuel pays for when every product costs a unit of fuel of its own. */
    private static final int ONE_OUTPUT_PER_INPUT = 1;
    /** What the furnace burns to smelt, in the order it takes it. */
    private static final List<ItemType> FURNACE_FUELS = List.of(ItemType.COAL, ItemType.CHARCOAL);
    /** What the oven and the campfire burn. */
    private static final List<ItemType> LOG_FUEL = List.of(ItemType.WOOD_LOG);

    /** Not instantiable: the class only holds the profiles. */
    private StationProfiles() {
    }

    /** @return the profile of the furnace */
    public static StationProfile furnace() {
        final var config = AppConfig.getInstance().stations();
        return new StationProfile(
                List.of(new ProcessingRecipe(ItemType.IRON_ORE, ItemType.IRON_INGOT, config.getFurnaceSmeltSeconds(),
                                ONE_OUTPUT_PER_INPUT),
                        new ProcessingRecipe(ItemType.WOOD_LOG, ItemType.CHARCOAL, config.getFurnaceCharcoalSeconds(), 0)),
                FURNACE_FUELS, FuelMode.PER_OUTPUT, config.getFurnaceInputCap(), config.getFurnaceFuelCap(), 0.0);
    }

    /** @return the profile of the stone oven */
    public static StationProfile stoneOven() {
        final var config = AppConfig.getInstance().stations();
        return new StationProfile(
                List.of(new ProcessingRecipe(ItemType.RAW_MEAT, ItemType.COOKED_MEAT, config.getOvenCookSeconds(),
                        config.getOvenPiecesPerLog())),
                LOG_FUEL, FuelMode.PER_OUTPUT, config.getOvenInputCap(), config.getOvenFuelCap(), 0.0);
    }

    /** @return the profile of the campfire */
    public static StationProfile campfire() {
        final var config = AppConfig.getInstance().stations();
        return new StationProfile(
                List.of(new ProcessingRecipe(ItemType.RAW_MEAT, ItemType.COOKED_MEAT, config.getCampfireCookSeconds(),
                        ONE_OUTPUT_PER_INPUT)),
                LOG_FUEL, FuelMode.BURNING, config.getCampfireInputCap(), config.getCampfireFuelCap(),
                config.getCampfireLogBurnSeconds());
    }
}
