package com.feuerschvenger.perlinsedge.ui.view;

import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FxToolkitSupportTest {

    @Test
    void anUnavailableToolkitFailsTheTestWhenCiIsSet() {
        assertThrows(AssertionError.class, () -> FxToolkitSupport.reportUnavailable("no display", "true"));
    }

    @Test
    void aToolkitThatCannotStartFailsTheTestWhenCiIsSet() {
        FxToolkitSupport.Launcher launcher = new FxToolkitSupport.Launcher();

        AssertionError failure = assertThrows(AssertionError.class,
                () -> launcher.start("true", task -> { throw new UnsupportedOperationException("no display"); }));

        assertTrue(failure.getMessage().contains("no display"));
        assertTrue(failure.getMessage().contains("CI is set"));
    }

    @Test
    void aToolkitThatCannotStartSkipsTheTestWhenCiIsNotSetAndEveryLaterCallReportsItAtOnce() {
        FxToolkitSupport.Launcher launcher = new FxToolkitSupport.Launcher();

        assertThrows(TestAbortedException.class,
                () -> launcher.start(null, task -> { throw new UnsupportedOperationException("no display"); }));
        assertThrows(TestAbortedException.class, () -> launcher.start(null, task -> task.run()),
                "the failure is remembered, the starter is not asked again");
        assertThrows(AssertionError.class, () -> launcher.start("true", task -> task.run()),
                "and it is reported against the CI value of the later call");
    }

    @Test
    void aMissingNativeLibraryCountsAsAToolkitThatCannotStart() {
        FxToolkitSupport.Launcher launcher = new FxToolkitSupport.Launcher();

        assertThrows(AssertionError.class,
                () -> launcher.start("1", task -> { throw new UnsatisfiedLinkError("glass"); }));
    }

    @Test
    void anUnavailableToolkitSkipsTheTestWhenCiIsNotSet() {
        assertThrows(TestAbortedException.class, () -> FxToolkitSupport.reportUnavailable("no display", null));
        assertThrows(TestAbortedException.class, () -> FxToolkitSupport.reportUnavailable("no display", " "));
    }
}
