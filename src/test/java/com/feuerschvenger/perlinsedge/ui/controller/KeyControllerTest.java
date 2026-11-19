package com.feuerschvenger.perlinsedge.ui.controller;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.events.GameKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyControllerTest {

    private final List<Integer> numbers = new ArrayList<>();
    private KeyController controller;

    @BeforeEach
    void setUp() {
        controller = new KeyController(() -> { }, () -> { }, () -> { }, () -> { }, numbers::add);
    }

    @AfterEach
    void tearDown() {
        AppConfig.getInstance().dayCycle().setFastTime(false);
        final AppConfig.DebugConfig debug = AppConfig.getInstance().debug();
        debug.setEnabled(false);
        debug.setGrayscaleEnabled(false);
        debug.setTextEnabled(false);
    }

    private KeyController withCallbacks(List<String> calls) {
        return new KeyController(() -> calls.add("pause"), () -> calls.add("centre"), () -> calls.add("newMap"),
                () -> calls.add("inventory"), numbers::add);
    }

    @Test
    void f5TogglesTheFastTimeOfTheDayCycle() {
        assertFalse(AppConfig.getInstance().dayCycle().isFastTime());
        assertEquals(1.0, AppConfig.getInstance().dayCycle().timeScale());

        controller.onKeyAction(GameKey.F5);
        assertTrue(AppConfig.getInstance().dayCycle().isFastTime());
        assertEquals(30.0, AppConfig.getInstance().dayCycle().timeScale(), 1e-9, "a day of 1200 s in 40 s");

        controller.onKeyAction(GameKey.F5);
        assertFalse(AppConfig.getInstance().dayCycle().isFastTime());
    }

    @Test
    void theDayCycleSettingsAreBuiltOnceSoTheTimeScaleAllocatesNothingEveryTick() {
        final AppConfig.DayCycleConfig config = AppConfig.getInstance().dayCycle();

        assertSame(config.settings(), config.settings());
        config.setFastTime(true);
        assertEquals(config.getFastTimeMultiplier(), config.timeScale(), 0.0);
        assertEquals(config.settings().cycleSeconds() / 40.0, config.getFastTimeMultiplier(), 1e-9);
    }

    @Test
    void theUseKeyRunsItsAction() {
        List<String> calls = new ArrayList<>();
        controller.setUseAction(() -> calls.add("use"));

        controller.onKeyAction(GameKey.USE_ITEM);
        controller.onKeyAction(GameKey.USE_ITEM);

        assertEquals(List.of("use", "use"), calls);
    }

    @Test
    void theUseKeyIsAGameplayKeyThatIsIgnoredWhileInputIsBlocked() {
        List<String> calls = new ArrayList<>();
        controller.setUseAction(() -> calls.add("use"));
        controller.setInputBlocked(() -> true);

        controller.onKeyAction(GameKey.USE_ITEM);

        assertTrue(calls.isEmpty());
    }

    @Test
    void theUseKeyWithNoActionSetDoesNothing() {
        assertDoesNotThrow(() -> controller.onKeyAction(GameKey.USE_ITEM));
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
        assertDoesNotThrow(() -> {
            controller.onKeyAction(GameKey.DROP_ONE);
            controller.onKeyAction(GameKey.DROP_STACK);
        });
    }

    @Test
    void digitKeysSelectHotbarSlots() {
        controller.onKeyAction(GameKey.DIGIT_3);

        assertEquals(List.of(3), numbers);
    }

    @Test
    void gameplayKeysAreIgnoredWhileInputIsBlocked() {
        List<String> calls = new ArrayList<>();
        KeyController blocked = new KeyController(() -> calls.add("pause"), () -> calls.add("centre"),
                () -> { }, () -> calls.add("inventory"),
                n -> calls.add("digit"));
        blocked.setInputBlocked(() -> true);

        blocked.onKeyAction(GameKey.I);
        blocked.onKeyAction(GameKey.DIGIT_2);

        assertTrue(calls.isEmpty(), calls.toString());
    }

    @Test
    void pauseAndCameraKeysWorkWhileInputIsBlocked() {
        List<String> calls = new ArrayList<>();
        KeyController blocked = new KeyController(() -> calls.add("pause"), () -> calls.add("centre"),
                () -> { }, () -> { }, n -> { });
        blocked.setInputBlocked(() -> true);

        blocked.onKeyAction(GameKey.ESCAPE);
        blocked.onKeyAction(GameKey.SPACE);

        assertEquals(List.of("pause", "centre"), calls);
    }

    @Test
    void theSpaceBarCentresTheCameraAndNothingElse() {
        List<String> calls = new ArrayList<>();
        KeyController keys = new KeyController(() -> calls.add("pause"), () -> calls.add("centre"),
                () -> calls.add("map"), () -> calls.add("inventory"),
                n -> calls.add("digit"));

        keys.onKeyAction(GameKey.SPACE);
        keys.onKeyAction(GameKey.SPACE);

        assertEquals(List.of("centre", "centre"), calls);
    }

    @Test
    void gameplayKeysWorkAgainOnceInputIsUnblocked() {
        List<String> calls = new ArrayList<>();
        boolean[] blocked = {true};
        KeyController keys = new KeyController(() -> { }, () -> { }, () -> { },
                () -> calls.add("inventory"), n -> { });
        keys.setInputBlocked(() -> blocked[0]);

        keys.onKeyAction(GameKey.I);
        blocked[0] = false;
        keys.onKeyAction(GameKey.I);

        assertEquals(List.of("inventory"), calls);
    }

    @Test
    void missingActionsAreIgnored() {
        KeyController bare = new KeyController(null, null, null, null, null);

        assertDoesNotThrow(() -> {
            bare.onKeyAction(GameKey.DIGIT_1);
            bare.onKeyAction(GameKey.SPACE);
        });
    }

    @Test
    void theRotateKeyRunsItsActionAndIsIgnoredWhileInputIsBlocked() {
        List<String> calls = new ArrayList<>();
        controller.setRotateAction(() -> calls.add("rotate"));

        controller.onKeyAction(GameKey.ROTATE);
        controller.setInputBlocked(() -> true);
        controller.onKeyAction(GameKey.ROTATE);

        assertEquals(List.of("rotate"), calls);
    }

    @Test
    void theRotateKeyWithNoActionSetDoesNothing() {
        assertDoesNotThrow(() -> controller.onKeyAction(GameKey.ROTATE));
    }

    @Test
    void theFixedKeysRunTheirOwnCallbacks() {
        final List<String> calls = new ArrayList<>();
        final KeyController wired = withCallbacks(calls);

        wired.onKeyAction(GameKey.ESCAPE);
        wired.onKeyAction(GameKey.SPACE);
        wired.onKeyAction(GameKey.I);

        assertEquals(List.of("pause", "centre", "inventory"), calls);
    }

    @Test
    void f1TogglesTheDebugMode() {
        final AppConfig.DebugConfig debug = AppConfig.getInstance().debug();

        controller.onKeyAction(GameKey.F1);
        assertTrue(debug.isEnabled());

        controller.onKeyAction(GameKey.F1);
        assertFalse(debug.isEnabled());
    }

    @Test
    void f2TogglesTheGrayscaleView() {
        final AppConfig.DebugConfig debug = AppConfig.getInstance().debug();

        controller.onKeyAction(GameKey.F2);
        assertTrue(debug.isGrayscaleEnabled());

        controller.onKeyAction(GameKey.F2);
        assertFalse(debug.isGrayscaleEnabled());
    }

    @Test
    void f3TogglesTheDebugTextOnlyWhileTheGrayscaleViewIsOn() {
        final AppConfig.DebugConfig debug = AppConfig.getInstance().debug();

        controller.onKeyAction(GameKey.F3);
        assertFalse(debug.isTextEnabled(), "the text belongs to the grayscale view");

        controller.onKeyAction(GameKey.F2);
        controller.onKeyAction(GameKey.F3);
        assertTrue(debug.isTextEnabled());

        controller.onKeyAction(GameKey.F3);
        assertFalse(debug.isTextEnabled());
    }

    @Test
    void theNewMapKeyOnlyActsWhileTheDebugModeIsOn() {
        final List<String> calls = new ArrayList<>();
        final KeyController wired = withCallbacks(calls);

        wired.onKeyAction(GameKey.F4);
        assertTrue(calls.isEmpty(), "debug mode is off");

        wired.onKeyAction(GameKey.F1);
        wired.onKeyAction(GameKey.F4);
        assertEquals(List.of("newMap"), calls);
    }

    @Test
    void theNewMapKeyDoesNothingWithoutCallbacksEvenInDebugMode() {
        final KeyController bare = new KeyController(null, null, null, null, null);
        bare.onKeyAction(GameKey.F1);

        bare.onKeyAction(GameKey.F4);
        bare.onKeyAction(GameKey.ESCAPE);
        bare.onKeyAction(GameKey.DIGIT_3);

        assertTrue(AppConfig.getInstance().debug().isEnabled(), "only the debug toggle ran");
    }
}
