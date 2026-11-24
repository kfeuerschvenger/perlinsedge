package com.feuerschvenger.perlinsedge.domain.crafting.processing;

import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StationProfileTest {

    private static final int CAP = 10;
    private static final double SECONDS = 2.0;
    private static final double BURN_SECONDS = 5.0;

    private static StationProfile profile(List<ItemType> fuels) {
        var recipe = new ProcessingRecipe(ItemType.IRON_ORE, ItemType.IRON_INGOT, SECONDS, 1);
        return new StationProfile(List.of(recipe), fuels, FuelMode.BURNING, CAP, CAP, BURN_SECONDS);
    }

    @Test
    void fuelItemsDropsAFuelThatIsAlsoAnInput() {
        var profile = profile(List.of(ItemType.IRON_ORE, ItemType.COAL));

        assertEquals(List.of(ItemType.COAL), profile.fuelItems());
    }

    @Test
    void fuelItemsListsADuplicatedFuelOnceKeepingOrder() {
        var profile = profile(List.of(ItemType.WOOD_LOG, ItemType.COAL, ItemType.WOOD_LOG));

        assertEquals(List.of(ItemType.WOOD_LOG, ItemType.COAL), profile.fuelItems());
    }
}
