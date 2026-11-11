package com.feuerschvenger.perlinsedge.ui.view;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Keeps the panels on the theme: styling lives in the stylesheet, never in inline strings or per-panel fonts. */
class ThemeUsageTest {
    private static final Path VIEW_SOURCES =
            Path.of("src/main/java/com/feuerschvenger/perlinsedge/ui/view");

    private static List<Path> viewSources() throws IOException {
        try (Stream<Path> files = Files.walk(VIEW_SOURCES)) {
            return files.filter(p -> p.toString().endsWith(".java")).toList();
        }
    }

    @Test
    void viewSourcesAreFound() throws IOException {
        assertTrue(viewSources().size() > 10);
    }

    @Test
    void noPanelStylesItselfWithAnInlineStyleString() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (Path source : viewSources()) {
            if (Files.readString(source).contains(".setStyle(")) {
                offenders.add(source.getFileName().toString());
            }
        }
        assertEquals(List.of(), offenders);
    }

    @Test
    void noPanelPicksItsOwnFontFamily() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (Path source : viewSources()) {
            String text = Files.readString(source);
            if (text.contains("\"Arial\"") || text.contains("setFont(") || text.contains("setTextFill(")) {
                offenders.add(source.getFileName().toString());
            }
        }
        assertEquals(List.of(), offenders);
    }
}
