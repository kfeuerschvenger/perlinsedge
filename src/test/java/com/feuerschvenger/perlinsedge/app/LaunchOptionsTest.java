package com.feuerschvenger.perlinsedge.app;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LaunchOptionsTest {

    @Test
    void theDebugKeysAreOffWithNoArguments() {
        assertFalse(LaunchOptions.parse(List.of()).debugKeys());
        assertFalse(LaunchOptions.parse(null).debugKeys());
        assertFalse(LaunchOptions.DEFAULT.debugKeys());
    }

    @Test
    void theDebugFlagTurnsTheDebugKeysOn() {
        assertTrue(LaunchOptions.parse(List.of("--debug")).debugKeys());
    }

    @Test
    void theDebugFlagIsFoundAmongOtherArguments() {
        assertTrue(LaunchOptions.parse(List.of("--other", "--debug", "value")).debugKeys());
    }

    @Test
    void unknownArgumentsAreIgnored() {
        assertFalse(LaunchOptions.parse(List.of("--verbose", "debug", "-debug", "--debugger")).debugKeys());
    }
}
