package com.feuerschvenger.perlinsedge.ui.view;

import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;

import static org.junit.jupiter.api.Assertions.assertThrows;

class FxToolkitSupportTest {

    @Test
    void anUnavailableToolkitFailsTheTestWhenCiIsSet() {
        assertThrows(AssertionError.class, () -> FxToolkitSupport.reportUnavailable("no display", "true"));
    }

    @Test
    void anUnavailableToolkitSkipsTheTestWhenCiIsNotSet() {
        assertThrows(TestAbortedException.class, () -> FxToolkitSupport.reportUnavailable("no display", null));
        assertThrows(TestAbortedException.class, () -> FxToolkitSupport.reportUnavailable("no display", " "));
    }
}
