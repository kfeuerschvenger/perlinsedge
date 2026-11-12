package com.feuerschvenger.perlinsedge.ui.controller;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.events.GameKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyControllerTest {

    private final List<Integer> pageChanges = new ArrayList<>();
    private final List<Integer> numbers = new ArrayList<>();
    private KeyController controller;

    @BeforeEach
    void setUp() {
        AppConfig.getInstance().debug().setFpsOverlayEnabled(false);
        controller = new KeyController(() -> { }, () -> { }, () -> { }, () -> { }, () -> { }, () -> { }, () -> { },
                numbers::add, pageChanges::add);
    }

    @AfterEach
    void tearDown() {
        AppConfig.getInstance().debug().setFpsOverlayEnabled(false);
    }

    @Test
    void pageUpAndPageDownChangeTheHotbarPageBackwardsAndForwards() {
        controller.onKeyAction(GameKey.PAGE_UP);
        controller.onKeyAction(GameKey.PAGE_DOWN);
        controller.onKeyAction(GameKey.PAGE_DOWN);

        assertEquals(List.of(-1, 1, 1), pageChanges);
    }

    @Test
    void noGameKeyIsNamedEBecauseItIsKeptFreeForOtherActions() {
        for (GameKey key : GameKey.values()) {
            assertFalse(key.name().equals("E"), key.name());
        }
    }

    @Test
    void theDropKeysRunTheirOwnActions() {
        List<String> calls = new ArrayList<>();
        controller.setDropActions(() -> calls.add("one"), () -> calls.add("stack"));

        controller.onKeyAction(GameKey.DROP_ONE);
        controller.onKeyAction(GameKey.DROP_STACK);
        controller.onKeyAction(GameKey.DROP_ONE);

        assertEquals(List.of("one", "stack", "one"), calls);
    }

    @Test
    void theDropKeysAreIgnoredWhileInputIsBlocked() {
        List<String> calls = new ArrayList<>();
        controller.setDropActions(() -> calls.add("one"), () -> calls.add("stack"));
        controller.setInputBlocked(() -> true);

        controller.onKeyAction(GameKey.DROP_ONE);
        controller.onKeyAction(GameKey.DROP_STACK);

        assertTrue(calls.isEmpty(), calls.toString());
    }

    @Test
    void theDropKeysWorkAgainOnceInputIsUnblocked() {
        List<String> calls = new ArrayList<>();
        boolean[] blocked = {true};
        controller.setDropActions(() -> calls.add("one"), () -> calls.add("stack"));
        controller.setInputBlocked(() -> blocked[0]);

        controller.onKeyAction(GameKey.DROP_ONE);
        blocked[0] = false;
        controller.onKeyAction(GameKey.DROP_STACK);

        assertEquals(List.of("stack"), calls);
    }

    @Test
    void dropKeysWithoutActionsAreIgnored() {
        controller.onKeyAction(GameKey.DROP_ONE);
        controller.onKeyAction(GameKey.DROP_STACK);
    }

    @Test
    void digitKeysSelectHotbarSlots() {
        controller.onKeyAction(GameKey.DIGIT_3);

        assertEquals(List.of(3), numbers);
        assertTrue(pageChanges.isEmpty());
    }

    @Test
    void fpsOverlayIsOffByDefaultAndF6TogglesIt() {
        assertFalse(AppConfig.getInstance().debug().isFpsOverlayEnabled());

        controller.onKeyAction(GameKey.F6);
        assertTrue(AppConfig.getInstance().debug().isFpsOverlayEnabled());

        controller.onKeyAction(GameKey.F6);
        assertFalse(AppConfig.getInstance().debug().isFpsOverlayEnabled());
    }

    @Test
    void fpsOverlayDoesNotNeedDebugMode() {
        boolean debugBefore = AppConfig.getInstance().debug().isEnabled();

        controller.onKeyAction(GameKey.F6);

        assertEquals(debugBefore, AppConfig.getInstance().debug().isEnabled());
        assertTrue(AppConfig.getInstance().debug().isFpsOverlayEnabled());
    }

    @Test
    void gameplayKeysAreIgnoredWhileInputIsBlocked() {
        List<String> calls = new ArrayList<>();
        KeyController blocked = new KeyController(() -> calls.add("pause"), () -> calls.add("centre"),
                () -> { }, () -> { }, () -> calls.add("inventory"), () -> calls.add("attack"),
                () -> calls.add("build"), n -> calls.add("digit"), p -> calls.add("page"));
        blocked.setInputBlocked(() -> true);

        blocked.onKeyAction(GameKey.SPACE);
        blocked.onKeyAction(GameKey.I);
        blocked.onKeyAction(GameKey.B);
        blocked.onKeyAction(GameKey.DIGIT_2);
        blocked.onKeyAction(GameKey.PAGE_UP);
        blocked.onKeyAction(GameKey.PAGE_DOWN);

        assertTrue(calls.isEmpty(), calls.toString());
    }

    @Test
    void pauseAndCameraKeysWorkWhileInputIsBlocked() {
        List<String> calls = new ArrayList<>();
        KeyController blocked = new KeyController(() -> calls.add("pause"), () -> calls.add("centre"),
                () -> { }, () -> { }, () -> { }, () -> { }, () -> { }, n -> { }, p -> { });
        blocked.setInputBlocked(() -> true);

        blocked.onKeyAction(GameKey.ESCAPE);
        blocked.onKeyAction(GameKey.C);

        assertEquals(List.of("pause", "centre"), calls);
    }

    @Test
    void gameplayKeysWorkAgainOnceInputIsUnblocked() {
        List<String> calls = new ArrayList<>();
        boolean[] blocked = {true};
        KeyController keys = new KeyController(() -> { }, () -> { }, () -> { }, () -> { }, () -> { },
                () -> calls.add("attack"), () -> { }, n -> { }, p -> { });
        keys.setInputBlocked(() -> blocked[0]);

        keys.onKeyAction(GameKey.SPACE);
        blocked[0] = false;
        keys.onKeyAction(GameKey.SPACE);

        assertEquals(List.of("attack"), calls);
    }

    @Test
    void missingActionsAreIgnored() {
        KeyController bare = new KeyController(null, null, null, null, null, null, null, null, null);

        bare.onKeyAction(GameKey.PAGE_UP);
        bare.onKeyAction(GameKey.PAGE_DOWN);
        bare.onKeyAction(GameKey.DIGIT_1);
        bare.onKeyAction(GameKey.SPACE);
    }
}
