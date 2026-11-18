package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.domain.entities.items.DroppedItem;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.ui.view.CanvasProbe;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FxItemDrawingHelperTest {

    private static final int SIZE = 120;
    private static final int BOX_X = 40;
    private static final int BOX_Y = 40;
    private static final int BOX = 40;
    private static final int TOLERANCE = 8;

    private static CanvasProbe fallback(ItemType type) {
        return CanvasProbe.render(SIZE, SIZE,
                gc -> FxItemDrawingHelper.drawFallbackShape(gc, BOX_X, BOX_Y, BOX, BOX, type, 1.0));
    }

    private static CanvasProbe dropped(ItemType type, int quantity, double gameTime) {
        final DroppedItem item = new DroppedItem(3, 3, new Item(type, quantity), 0, DroppedItem.DropSource.RESOURCE);
        return CanvasProbe.render(SIZE, SIZE, gc -> FxItemDrawingHelper.drawDroppedItem(gc, 60, 40, 1.0, item, gameTime));
    }

    @Test
    void everyItemTypeHasAFallbackShapeInsideItsBox() {
        for (ItemType type : ItemType.values()) {
            final CanvasProbe shot = fallback(type);

            assertTrue(shot.painted(BOX_X, BOX_Y, BOX_X + BOX, BOX_Y + BOX) > 0, type + " draws nothing");
            assertEquals(0, shot.painted(0, 0, SIZE, BOX_Y - 2), type + " leaks above the box");
            assertEquals(0, shot.painted(0, BOX_Y + BOX + 2, SIZE, SIZE), type + " leaks below the box");
            assertEquals(0, shot.painted(0, 0, BOX_X - 2, SIZE), type + " leaks left of the box");
            assertEquals(0, shot.painted(BOX_X + BOX + 2, 0, SIZE, SIZE), type + " leaks right of the box");
        }
    }

    @Test
    void theResourceItemsKeepTheirOwnColours() {
        assertTrue(fallback(ItemType.WOOD_LOG).near(Color.BROWN.darker(), TOLERANCE) > 100, "log");
        assertTrue(fallback(ItemType.WOOD_PLANK).near(Color.BROWN.brighter(), TOLERANCE) > 100, "plank");
        assertTrue(fallback(ItemType.STONE).near(Color.GRAY.darker(), TOLERANCE) > 100, "stone");
        assertTrue(fallback(ItemType.CRYSTALS).near(Color.CYAN.brighter(), TOLERANCE) > 50, "crystals");
        assertTrue(fallback(ItemType.SLIME_BALL).near(Color.LIMEGREEN.brighter(), TOLERANCE) > 100, "slime");
    }

    @Test
    void everyToolIsAHandleWithADarkHead() {
        for (ItemType tool : new ItemType[]{ItemType.PICKAXE, ItemType.AXE, ItemType.SWORD, ItemType.HAMMER}) {
            final CanvasProbe shot = fallback(tool);

            assertTrue(shot.near(Color.SILVER, TOLERANCE) > 20, tool + " handle");
            assertTrue(shot.near(Color.GRAY.darker(), TOLERANCE) > 5, tool + " head");
        }
    }

    @Test
    void theToolHeadsDifferFromOneAnother() {
        assertFalse(fallback(ItemType.PICKAXE).sameAs(fallback(ItemType.AXE)));
        assertFalse(fallback(ItemType.AXE).sameAs(fallback(ItemType.HAMMER)));
        assertFalse(fallback(ItemType.HAMMER).sameAs(fallback(ItemType.SWORD)));
    }

    @Test
    void anItemWithNoSpecialShapeIsAGreyBox() {
        assertTrue(fallback(ItemType.COAL).near(Color.DARKGRAY, TOLERANCE) > 100);
    }

    @Test
    void aDroppedItemIsDrawnAboveTheTileTopAndMoreThanOneShowsItsQuantity() {
        for (ItemType type : ItemType.values()) {
            assertTrue(dropped(type, 1, 0).painted() > 0, type + " dropped item is invisible");
        }

        final CanvasProbe single = dropped(ItemType.STONE, 1, 0);
        final CanvasProbe stack = dropped(ItemType.STONE, 7, 0);

        assertTrue(stack.painted() > single.painted(), "the quantity is drawn");
        assertTrue(stack.near(Color.WHITE, 0) > 0, "the digit is white");
    }

    @Test
    void aDroppedItemBobsWithTheGameClock() {
        final CanvasProbe low = dropped(ItemType.STONE, 1, 0);
        final CanvasProbe high = dropped(ItemType.STONE, 1, Math.PI / 6);

        assertFalse(low.sameAs(high));
    }

    @Test
    void aThrownItemIsLiftedWhileItFlies() {
        final DroppedItem flying = new DroppedItem(0, 0, 3, 3, new Item(ItemType.STONE, 1), 0,
                DroppedItem.DropSource.PLAYER);
        final DroppedItem landed = new DroppedItem(3, 3, new Item(ItemType.STONE, 1), 0, DroppedItem.DropSource.PLAYER);

        final CanvasProbe inFlight = CanvasProbe.render(SIZE, SIZE,
                gc -> FxItemDrawingHelper.drawDroppedItem(gc, 60, 70, 1.0, flying, 0.2));
        final CanvasProbe onGround = CanvasProbe.render(SIZE, SIZE,
                gc -> FxItemDrawingHelper.drawDroppedItem(gc, 60, 70, 1.0, landed, 0.2));

        assertFalse(inFlight.sameAs(onGround));
    }
}
