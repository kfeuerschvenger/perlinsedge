package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.containers.SlotRef;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Chest;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DragSessionTest {

    private Player player;
    private Item stack;
    private DragSession session;

    @BeforeEach
    void setUp() {
        player = new Player(0, 0);
        stack = new Item(ItemType.STONE, 5);
        player.setItemAt(2, stack);
        session = new DragSession(new SlotRef(player, 2));
    }

    @Test
    void theSessionHoldsTheStackOfTheSlot() {
        assertSame(stack, session.item());
        assertEquals(2, session.origin().slot());
        assertTrue(session.isStillValid());
    }

    @Test
    void anEmptySlotCannotBeDragged() {
        assertThrows(IllegalArgumentException.class, () -> new DragSession(new SlotRef(player, 7)));
    }

    @Test
    void aMissingSlotCannotBeDragged() {
        assertThrows(NullPointerException.class, () -> new DragSession(null));
    }

    @Test
    void releasingOutsideEverySlotThrowsTheStack() {
        assertEquals(DragSession.Release.DROP_STACK, session.release(null));
    }

    @Test
    void releasingOverAnotherSlotMovesTheStackThere() {
        assertEquals(DragSession.Release.TRANSFER, session.release(new SlotRef(player, 5)));
    }

    @Test
    void releasingOverASlotOfAnotherContainerMovesTheStackThere() {
        Chest chest = new Chest(0, 0, 9);

        assertEquals(DragSession.Release.TRANSFER, session.release(new SlotRef(chest, 2)));
    }

    @Test
    void releasingOverTheSlotItCameFromChangesNothing() {
        assertEquals(DragSession.Release.CANCEL, session.release(new SlotRef(player, 2)));
    }

    @Test
    void aStackThatLeftItsSlotCancelsEveryRelease() {
        player.setItemAt(2, null);

        assertFalse(session.isStillValid());
        assertEquals(DragSession.Release.CANCEL, session.release(null));
        assertEquals(DragSession.Release.CANCEL, session.release(new SlotRef(player, 5)));
    }

    @Test
    void aSlotThatNowHoldsAnotherStackCancelsEveryRelease() {
        player.setItemAt(2, new Item(ItemType.STONE, 5));

        assertFalse(session.isStillValid());
        assertEquals(DragSession.Release.CANCEL, session.release(null));
    }

    @Test
    void aStackThatShrankIsStillTheSameStack() {
        stack.removeQuantity(2);

        assertTrue(session.isStillValid());
        assertEquals(DragSession.Release.DROP_STACK, session.release(null));
    }
}
