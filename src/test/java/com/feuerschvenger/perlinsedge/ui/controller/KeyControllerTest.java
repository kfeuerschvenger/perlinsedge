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
    void noGameKeyIsNamedQOrEBecauseTheyAreKeptFreeForOtherActions() {
        for (GameKey key : GameKey.values()) {
            assertFalse(key.name().equals("Q") || key.name().equals("E"), key.name());
        }
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
    void missingActionsAreIgnored() {
        KeyController bare = new KeyController(null, null, null, null, null, null, null, null, null);

        bare.onKeyAction(GameKey.PAGE_UP);
        bare.onKeyAction(GameKey.PAGE_DOWN);
        bare.onKeyAction(GameKey.DIGIT_1);
        bare.onKeyAction(GameKey.SPACE);
    }
}
