package com.feuerschvenger.perlinsedge.infra.fx.input;

import com.feuerschvenger.perlinsedge.domain.events.GameKey;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.FxCamera;
import com.feuerschvenger.perlinsedge.ui.view.FxToolkitSupport;
import javafx.event.Event;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The key tracking sits in scene filters, so no node can hide a release from it. */
class FxInputHandlerTest {

    private static KeyEvent event(javafx.event.EventType<KeyEvent> type, KeyCode code) {
        return new KeyEvent(type, "", "", code, false, false, false, false);
    }

    /** A scene with a child node that consumes every key release, a handler on it, and the keys it delivers. */
    private static final class Rig {
        final Group child = new Group();
        final List<GameKey> delivered = new ArrayList<>();
        final FxInputHandler handler;

        Rig() {
            Scene scene = new Scene(new Group(child));
            child.setOnKeyReleased(Event::consume);
            handler = new FxInputHandler(scene, new Canvas(10, 10), new FxCamera(0, 0, 1));
            handler.setGameKeyListener(delivered::add);
        }

        void press(KeyCode code) {
            Event.fireEvent(child, event(KeyEvent.KEY_PRESSED, code));
        }

        void release(KeyCode code) {
            Event.fireEvent(child, event(KeyEvent.KEY_RELEASED, code));
        }
    }

    @Test
    void aReleaseConsumedByAChildNodeStillEndsTheHold() {
        List<GameKey> delivered = FxToolkitSupport.onFx(() -> {
            Rig rig = new Rig();
            rig.press(KeyCode.I);
            rig.press(KeyCode.I);
            rig.release(KeyCode.I);
            rig.press(KeyCode.I);
            return rig.delivered;
        });

        assertEquals(List.of(GameKey.I, GameKey.I), delivered);
    }

    @Test
    void aRepeatedPressIsNotDeliveredWhileTheKeyIsHeld() {
        List<GameKey> delivered = FxToolkitSupport.onFx(() -> {
            Rig rig = new Rig();
            rig.press(KeyCode.I);
            rig.press(KeyCode.I);
            rig.press(KeyCode.I);
            return rig.delivered;
        });

        assertEquals(List.of(GameKey.I), delivered);
    }

    @Test
    void regainingTheFocusForgetsTheHeldKeys() {
        List<GameKey> delivered = FxToolkitSupport.onFx(() -> {
            Rig rig = new Rig();
            rig.press(KeyCode.I);
            rig.handler.focusListener().changed(null, false, true);
            rig.press(KeyCode.I);
            return rig.delivered;
        });

        assertEquals(List.of(GameKey.I, GameKey.I), delivered);
    }
}
