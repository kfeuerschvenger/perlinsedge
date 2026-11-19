package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.ProcessingStation.LoadResult;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The engine the furnace, the stone oven and the campfire share: loading, caps, the time of each unit, fuel and the
 * lit state.
 */
class ProcessingStationTest {
    private static final AppConfig.StationsConfig CONFIG = AppConfig.getInstance().stations();
    private static final double TICK = 1.0 / 60.0;
    private static final String OVEN_EMPTY_HINT = "Empty, takes Raw meat, Wood log";

    private final Furnace furnace = new Furnace(3, 3);
    private final StoneOven oven = new StoneOven(3, 3);
    private final Campfire campfire = new Campfire(3, 3);

    private static void load(ProcessingStation station, ItemType type, int units) {
        for (var i = 0; i < units; i++) {
            assertEquals(LoadResult.LOADED, station.load(type));
        }
    }

    private static int units(List<Item> products, ItemType type) {
        return products.stream().filter(item -> item.getType() == type).mapToInt(Item::getQuantity).sum();
    }

    /** Lets time pass in game ticks, the way the game does. */
    private static void run(ProcessingStation station, double seconds) {
        var left = seconds;
        while (left > 0) {
            final var step = Math.min(TICK, left);
            station.advance(step);
            left -= step;
        }
    }

    // ------------------------------------------------------------------
    //  Loading
    // ------------------------------------------------------------------

    @Test
    void theFurnaceTakesOreLogsCoalAndCharcoalAndNothingElse() {
        assertTrue(furnace.accepts(ItemType.IRON_ORE));
        assertTrue(furnace.accepts(ItemType.WOOD_LOG));
        assertTrue(furnace.accepts(ItemType.COAL));
        assertTrue(furnace.accepts(ItemType.CHARCOAL));
        assertFalse(furnace.accepts(ItemType.RAW_MEAT));
        assertEquals(LoadResult.REJECTED, furnace.load(ItemType.STONE));
    }

    @Test
    void theOvenAndTheCampfireTakeRawMeatAndLogsOnly() {
        for (ProcessingStation station : List.of(oven, campfire)) {
            assertTrue(station.accepts(ItemType.RAW_MEAT));
            assertTrue(station.accepts(ItemType.WOOD_LOG));
            assertFalse(station.accepts(ItemType.COAL));
            assertFalse(station.accepts(ItemType.IRON_ORE));
        }
    }

    @Test
    void aUnitIsLoadedPerCallUpToTheCapOfItsRole() {
        load(furnace, ItemType.IRON_ORE, CONFIG.getFurnaceInputCap());

        assertEquals(LoadResult.FULL, furnace.load(ItemType.IRON_ORE));
        assertEquals(CONFIG.getFurnaceInputCap(), furnace.count(ItemType.IRON_ORE));
        assertEquals(LoadResult.LOADED, furnace.load(ItemType.COAL), "the fuel has a cap of its own");
    }

    @Test
    void theInputsOfAStationShareOneCap() {
        load(furnace, ItemType.IRON_ORE, CONFIG.getFurnaceInputCap() - 2);
        load(furnace, ItemType.WOOD_LOG, 2);

        assertEquals(LoadResult.FULL, furnace.load(ItemType.IRON_ORE));
        assertEquals(LoadResult.FULL, furnace.load(ItemType.WOOD_LOG));
        assertEquals(CONFIG.getFurnaceInputCap(), furnace.countInRole(ItemType.IRON_ORE));
    }

    @Test
    void theFuelsOfAStationShareOneCap() {
        load(furnace, ItemType.COAL, CONFIG.getFurnaceFuelCap() - 1);
        load(furnace, ItemType.CHARCOAL, 1);

        assertEquals(LoadResult.FULL, furnace.load(ItemType.COAL));
        assertEquals(LoadResult.FULL, furnace.load(ItemType.CHARCOAL));
        assertEquals(LoadResult.LOADED, furnace.load(ItemType.IRON_ORE), "the inputs have a cap of their own");
    }

    @Test
    void theRolesAreNamedForTheHints() {
        assertEquals("Input", furnace.roleName(ItemType.WOOD_LOG));
        assertEquals("Fuel", furnace.roleName(ItemType.CHARCOAL));
        assertEquals("Fuel", oven.roleName(ItemType.WOOD_LOG));
    }

    @Test
    void theCapsFollowTheConfiguration() {
        assertEquals(CONFIG.getFurnaceInputCap(), furnace.capOf(ItemType.IRON_ORE));
        assertEquals(CONFIG.getFurnaceInputCap(), furnace.capOf(ItemType.WOOD_LOG));
        assertEquals(CONFIG.getFurnaceFuelCap(), furnace.capOf(ItemType.COAL));
        assertEquals(CONFIG.getOvenInputCap(), oven.capOf(ItemType.RAW_MEAT));
        assertEquals(CONFIG.getOvenFuelCap(), oven.capOf(ItemType.WOOD_LOG));
        assertEquals(CONFIG.getCampfireInputCap(), campfire.capOf(ItemType.RAW_MEAT));
        assertEquals(CONFIG.getCampfireFuelCap(), campfire.capOf(ItemType.WOOD_LOG));
        assertEquals(0, campfire.capOf(ItemType.COAL));
    }

    @Test
    void theCampfireStopsTakingLogsAtItsCap() {
        load(campfire, ItemType.WOOD_LOG, CONFIG.getCampfireFuelCap());

        assertEquals(LoadResult.FULL, campfire.load(ItemType.WOOD_LOG));
    }

    @Test
    void theStatusHintGivesTheUnitsOfTheInputsAndOfTheFuelsWithTheirCaps() {
        load(furnace, ItemType.IRON_ORE, 3);
        load(furnace, ItemType.WOOD_LOG, 2);
        load(furnace, ItemType.COAL, 3);

        assertEquals("Input 5/20, Fuel 3/20", furnace.statusHint());
    }

    @Test
    void anEmptyStationSaysWhatItTakes() {
        assertEquals(OVEN_EMPTY_HINT, oven.statusHint());
    }

    @Test
    void aStationWhoseLoadWasUsedUpIsEmptyAgain() {
        load(oven, ItemType.RAW_MEAT, 1);
        load(oven, ItemType.WOOD_LOG, 1);

        run(oven, CONFIG.getOvenCookSeconds() + 1);

        assertEquals(1, units(oven.drainProducts(), ItemType.COOKED_MEAT));
        assertEquals(OVEN_EMPTY_HINT, oven.statusHint());
    }

    // ------------------------------------------------------------------
    //  Furnace
    // ------------------------------------------------------------------

    @Test
    void theFurnaceSmeltsAnOreIntoAnIngotAfterItsTimeAndUsesUpOneCoal() {
        load(furnace, ItemType.IRON_ORE, 1);
        load(furnace, ItemType.COAL, 1);

        furnace.advance(CONFIG.getFurnaceSmeltSeconds() - 0.5);
        assertEquals(1, furnace.count(ItemType.IRON_ORE), "the ore stays loaded until the unit is done");
        assertEquals(1, furnace.count(ItemType.COAL), "so does the fuel");
        assertTrue(furnace.drainProducts().isEmpty());

        furnace.advance(0.5);
        assertEquals(0, furnace.count(ItemType.IRON_ORE));
        assertEquals(0, furnace.count(ItemType.COAL));
        assertEquals(1, units(furnace.drainProducts(), ItemType.IRON_INGOT));
    }

    @Test
    void theFurnaceDoesNotSmeltWithoutFuel() {
        load(furnace, ItemType.IRON_ORE, 2);

        run(furnace, CONFIG.getFurnaceSmeltSeconds() * 3);

        assertEquals(2, furnace.count(ItemType.IRON_ORE));
        assertTrue(furnace.drainProducts().isEmpty());
        assertFalse(furnace.isWorking());
        assertFalse(furnace.isLit());
    }

    @Test
    void charcoalIsMadeFromLogsWithNoFuelAndTakesItsOwnTime() {
        load(furnace, ItemType.WOOD_LOG, 2);

        furnace.advance(CONFIG.getFurnaceCharcoalSeconds() - 0.5);
        assertTrue(furnace.drainProducts().isEmpty());
        furnace.advance(0.5);

        assertEquals(1, furnace.count(ItemType.WOOD_LOG));
        assertEquals(1, units(furnace.drainProducts(), ItemType.CHARCOAL));
    }

    @Test
    void aLogIsNeverFuelInTheFurnace() {
        load(furnace, ItemType.IRON_ORE, 1);
        load(furnace, ItemType.WOOD_LOG, 1);

        furnace.advance(CONFIG.getFurnaceCharcoalSeconds() + CONFIG.getFurnaceSmeltSeconds());

        assertEquals(1, furnace.count(ItemType.IRON_ORE), "the log made charcoal and the ore found no fuel");
        assertEquals(1, units(furnace.drainProducts(), ItemType.CHARCOAL));
    }

    @Test
    void charcoalIsFuelForSmeltingJustLikeCoal() {
        load(furnace, ItemType.IRON_ORE, 1);
        load(furnace, ItemType.CHARCOAL, 1);

        furnace.advance(CONFIG.getFurnaceSmeltSeconds());

        assertEquals(0, furnace.count(ItemType.CHARCOAL));
        assertEquals(1, units(furnace.drainProducts(), ItemType.IRON_INGOT));
    }

    @Test
    void theFurnaceWorksThroughItsQueueOneUnitAtATimeEachWithItsOwnFuel() {
        load(furnace, ItemType.IRON_ORE, 3);
        load(furnace, ItemType.COAL, 2);

        furnace.advance(CONFIG.getFurnaceSmeltSeconds() * 2);
        assertEquals(2, units(furnace.drainProducts(), ItemType.IRON_INGOT));
        assertEquals(0, furnace.count(ItemType.COAL));

        furnace.advance(CONFIG.getFurnaceSmeltSeconds() * 2);
        assertEquals(1, furnace.count(ItemType.IRON_ORE), "the third unit has no fuel and waits");
        assertTrue(furnace.drainProducts().isEmpty());

        load(furnace, ItemType.COAL, 1);
        furnace.advance(CONFIG.getFurnaceSmeltSeconds());
        assertEquals(1, units(furnace.drainProducts(), ItemType.IRON_INGOT));
    }

    @Test
    void aLongSliceOfTimeMakesEveryUnitItCovers() {
        load(furnace, ItemType.IRON_ORE, 4);
        load(furnace, ItemType.COAL, 4);

        furnace.advance(CONFIG.getFurnaceSmeltSeconds() * 4 + 1);

        assertEquals(4, units(furnace.drainProducts(), ItemType.IRON_INGOT));
        assertEquals(0, furnace.count(ItemType.IRON_ORE));
    }

    @Test
    void timeInGameTicksAddsUpToTheSameResult() {
        load(furnace, ItemType.IRON_ORE, 1);
        load(furnace, ItemType.COAL, 1);

        run(furnace, CONFIG.getFurnaceSmeltSeconds() - 0.1);
        assertTrue(furnace.drainProducts().isEmpty());
        run(furnace, 0.2);

        assertEquals(1, units(furnace.drainProducts(), ItemType.IRON_INGOT));
    }

    @Test
    void theFurnaceIsLitOnlyWhileItWorks() {
        assertFalse(furnace.isLit());
        load(furnace, ItemType.IRON_ORE, 1);
        assertFalse(furnace.isLit(), "ore without fuel does not work");
        load(furnace, ItemType.COAL, 1);
        assertTrue(furnace.isLit());
        assertTrue(furnace.isWorking());

        furnace.advance(CONFIG.getFurnaceSmeltSeconds());

        assertFalse(furnace.isLit());
        assertFalse(furnace.isWorking());
    }

    @Test
    void aZeroOrNegativeSlicePassesNoTime() {
        load(furnace, ItemType.WOOD_LOG, 1);

        furnace.advance(0);
        furnace.advance(-5);

        assertEquals(1, furnace.count(ItemType.WOOD_LOG));
        assertTrue(furnace.drainProducts().isEmpty());
    }

    // ------------------------------------------------------------------
    //  Stone oven
    // ------------------------------------------------------------------

    @Test
    void theOvenCooksAPieceInItsTimeAndBurnsOneLogForEveryTwoPieces() {
        load(oven, ItemType.RAW_MEAT, 4);
        load(oven, ItemType.WOOD_LOG, 2);

        oven.advance(CONFIG.getOvenCookSeconds());
        assertEquals(1, units(oven.drainProducts(), ItemType.COOKED_MEAT));
        assertEquals(1, oven.count(ItemType.WOOD_LOG), "the log is used up when the first piece finishes");

        oven.advance(CONFIG.getOvenCookSeconds());
        assertEquals(1, units(oven.drainProducts(), ItemType.COOKED_MEAT));
        assertEquals(1, oven.count(ItemType.WOOD_LOG), "the second piece is paid by the same log");

        oven.advance(CONFIG.getOvenCookSeconds() * 2);
        assertEquals(2, units(oven.drainProducts(), ItemType.COOKED_MEAT));
        assertEquals(0, oven.count(ItemType.WOOD_LOG));
        assertEquals(0, oven.count(ItemType.RAW_MEAT));
    }

    @Test
    void theOvenDoesNotWorkWithoutALogAndShowsNoFire() {
        load(oven, ItemType.RAW_MEAT, 1);

        run(oven, CONFIG.getOvenCookSeconds() * 2);

        assertEquals(1, oven.count(ItemType.RAW_MEAT));
        assertFalse(oven.isLit());
    }

    @Test
    void theOvenIsLitWhileItCooks() {
        load(oven, ItemType.RAW_MEAT, 1);
        load(oven, ItemType.WOOD_LOG, 1);

        assertTrue(oven.isLit());
        oven.advance(CONFIG.getOvenCookSeconds());
        assertFalse(oven.isLit());
    }

    // ------------------------------------------------------------------
    //  Campfire
    // ------------------------------------------------------------------

    @Test
    void theCampfireIsLitOnlyWhileItHasFuel() {
        assertFalse(campfire.isLit());

        load(campfire, ItemType.WOOD_LOG, 1);
        assertTrue(campfire.isLit());

        campfire.advance(CONFIG.getCampfireLogBurnSeconds() - 1);
        assertTrue(campfire.isLit());
        campfire.advance(1);
        assertFalse(campfire.isLit());
    }

    @Test
    void theCampfireBurnsLogsOneAfterAnotherWhetherItCooksOrNot() {
        load(campfire, ItemType.WOOD_LOG, 2);

        campfire.advance(CONFIG.getCampfireLogBurnSeconds() * 1.5);

        assertEquals(0, campfire.count(ItemType.WOOD_LOG), "the second log was lit when the first burnt out");
        assertTrue(campfire.isLit());
        campfire.advance(CONFIG.getCampfireLogBurnSeconds() * 0.5);
        assertFalse(campfire.isLit());
    }

    @Test
    void theCampfireCooksOnePieceInItsTime() {
        load(campfire, ItemType.RAW_MEAT, 2);
        load(campfire, ItemType.WOOD_LOG, 1);

        campfire.advance(CONFIG.getCampfireCookSeconds() - 1);
        assertTrue(campfire.drainProducts().isEmpty());
        campfire.advance(1);

        assertEquals(1, units(campfire.drainProducts(), ItemType.COOKED_MEAT));
        assertEquals(1, campfire.count(ItemType.RAW_MEAT));
    }

    @Test
    void theCampfireDoesNotCookWithoutFuel() {
        load(campfire, ItemType.RAW_MEAT, 1);

        run(campfire, CONFIG.getCampfireCookSeconds() * 2);

        assertEquals(1, campfire.count(ItemType.RAW_MEAT));
        assertFalse(campfire.isWorking());
    }

    @Test
    void theCampfireDoesNotCookAfterItsFuelBurnedOutUntilItIsFedAgain() {
        load(campfire, ItemType.WOOD_LOG, 1);
        campfire.advance(CONFIG.getCampfireLogBurnSeconds());

        load(campfire, ItemType.RAW_MEAT, 1);
        campfire.advance(CONFIG.getCampfireCookSeconds() * 2);

        assertEquals(1, campfire.count(ItemType.RAW_MEAT));
        assertFalse(campfire.isWorking());
        assertFalse(campfire.isLit());
    }

    @Test
    void aPieceHalfCookedWhenTheFuelRunsOutKeepsItsProgress() {
        final var half = CONFIG.getCampfireCookSeconds() / 2;
        load(campfire, ItemType.WOOD_LOG, 1);
        campfire.advance(CONFIG.getCampfireLogBurnSeconds() - half);
        load(campfire, ItemType.RAW_MEAT, 1);

        campfire.advance(half);
        assertTrue(campfire.drainProducts().isEmpty(), "the log burnt out with half of the piece cooked");
        assertFalse(campfire.isWorking());

        load(campfire, ItemType.WOOD_LOG, 1);
        campfire.advance(half - 1);
        assertTrue(campfire.drainProducts().isEmpty());
        campfire.advance(1);
        assertEquals(1, units(campfire.drainProducts(), ItemType.COOKED_MEAT));
    }

    // ------------------------------------------------------------------
    //  Emptying
    // ------------------------------------------------------------------

    @Test
    void emptyingGivesBackEverythingLoadedAndEveryProductNotYetTaken() {
        load(furnace, ItemType.IRON_ORE, 3);
        load(furnace, ItemType.COAL, 2);
        furnace.advance(CONFIG.getFurnaceSmeltSeconds());

        final var contents = furnace.emptyContents();

        assertEquals(2, units(contents, ItemType.IRON_ORE));
        assertEquals(1, units(contents, ItemType.COAL));
        assertEquals(1, units(contents, ItemType.IRON_INGOT));
        assertTrue(furnace.drainProducts().isEmpty());
        assertEquals(0, furnace.count(ItemType.IRON_ORE));
        assertFalse(furnace.isWorking());
    }

    @Test
    void anEmptyStationHasNoContents() {
        assertTrue(furnace.emptyContents().isEmpty());
    }

    @Test
    void aCampfireThatWasEmptiedIsNotLit() {
        load(campfire, ItemType.WOOD_LOG, 1);
        campfire.advance(1);

        campfire.emptyContents();

        assertFalse(campfire.isLit());
    }

    @Test
    void everyStationIsInteractableAndFacesTheLowerRightByDefault() {
        for (ProcessingStation station : List.of(furnace, oven, campfire)) {
            assertTrue(station.isInteractable());
            assertTrue(station.onInteract(null));
            assertEquals(1, station.frontOffset()[0]);
            assertEquals(0, station.frontOffset()[1]);
        }
    }
}
