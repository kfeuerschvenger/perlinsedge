package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.crafting.BuildingRecipe;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HotbarPagerTest {

    private static List<Integer> numbers(int count) {
        List<Integer> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            list.add(i);
        }
        return list;
    }

    @Test
    void twelveEntriesNeedTwoPagesOfNine() {
        HotbarPager<Integer> pager = new HotbarPager<>(numbers(12), 9);

        assertEquals(2, pager.getPageCount());
        assertEquals(9, pager.currentPage().size());
        pager.next();
        assertEquals(List.of(10, 11, 12), pager.currentPage());
    }

    @Test
    void slotNumbersAreRelativeToTheCurrentPage() {
        HotbarPager<Integer> pager = new HotbarPager<>(numbers(12), 9);

        assertEquals(1, pager.select(1));
        assertEquals(9, pager.select(9));
        pager.next();
        assertEquals(10, pager.select(1));
        assertEquals(12, pager.select(3));
        assertNull(pager.select(4), "the second page has only three entries");
        assertNull(pager.select(0));
        assertNull(pager.select(10));
    }

    @Test
    void pagingWrapsAroundInBothDirections() {
        HotbarPager<Integer> pager = new HotbarPager<>(numbers(12), 9);

        pager.previous();
        assertEquals(1, pager.getPage());
        pager.next();
        assertEquals(0, pager.getPage());
    }

    @Test
    void aSinglePageDoesNotMove() {
        HotbarPager<Integer> pager = new HotbarPager<>(numbers(5), 9);

        pager.next();
        pager.previous();

        assertEquals(0, pager.getPage());
        assertEquals(1, pager.getPageCount());
    }

    @Test
    void emptyListHasOneEmptyPage() {
        HotbarPager<Integer> pager = new HotbarPager<>(List.of(), 9);

        assertEquals(1, pager.getPageCount());
        assertTrue(pager.currentPage().isEmpty());
        assertNull(pager.select(1));
    }

    @Test
    void showPageOfFindsTheEntryAndItsSlot() {
        HotbarPager<Integer> pager = new HotbarPager<>(numbers(12), 9);

        assertTrue(pager.showPageOf(11));
        assertEquals(1, pager.getPage());
        assertEquals(2, pager.slotOf(11));
        assertFalse(pager.showPageOf(99));
        assertEquals(-1, pager.slotOf(99));
    }

    @Test
    void rejectsANonPositivePageSize() {
        assertThrows(IllegalArgumentException.class, () -> new HotbarPager<>(numbers(3), 0));
    }

    @Test
    void everyBuildingRecipeIsReachableFromSomePageAndSlot() {
        List<BuildingType> types = new RecipeManager().getAllBuildingRecipes().stream()
                .map(BuildingRecipe::buildingType).toList();
        HotbarPager<BuildingType> pager = new HotbarPager<>(types, 9);
        Set<BuildingType> reached = EnumSet.noneOf(BuildingType.class);

        for (int page = 0; page < pager.getPageCount(); page++) {
            for (int slot = 1; slot <= 9; slot++) {
                BuildingType type = pager.select(slot);
                if (type != null) {
                    reached.add(type);
                }
            }
            pager.next();
        }

        assertEquals(EnumSet.copyOf(types), reached);
        assertEquals(12, reached.size());
    }
}
