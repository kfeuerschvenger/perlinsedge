package com.feuerschvenger.perlinsedge.ui.view;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiThemeTest {

    private static String css() throws IOException {
        try (InputStream in = UiTheme.class.getResourceAsStream(UiTheme.STYLESHEET_PATH)) {
            assertNotNull(in, "stylesheet must be on the classpath");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void fontAndItsLicenseAreBundled() throws IOException {
        try (InputStream font = UiTheme.class.getResourceAsStream(UiTheme.FONT_PATH);
             InputStream license = UiTheme.class.getResourceAsStream("/assets/fonts/OFL.txt")) {
            assertNotNull(font);
            assertNotNull(license);
            assertTrue(new String(license.readAllBytes(), StandardCharsets.UTF_8).contains("SIL Open Font License"));
        }
    }

    @Test
    void stylesheetNamesTheBundledFontFamily() throws IOException {
        assertTrue(css().contains("\"" + UiTheme.FONT_FAMILY + "\""));
    }

    @Test
    void stylesheetDeclaresTheNamedPaletteTokens() throws IOException {
        String css = css();
        for (String token : List.of("-pe-panel:", "-pe-text: #fcf4e4", "-pe-muted: #aeadb8", "-pe-accent: #ffa02a",
                "-pe-danger: #d4705c", "-pe-focus: #fcf4e4")) {
            assertTrue(css.contains(token), "missing token " + token);
        }
        assertTrue(css.contains("rgba(29, 24, 48, 0.8)"), "panel must be #1d1830 at 80% opacity");
    }

    @Test
    void everyStyleClassConstantHasARuleInTheStylesheet() throws Exception {
        String css = css();
        List<String> missing = new ArrayList<>();
        for (Field field : UiTheme.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == String.class
                    && field.getName().matches("[A-Z_]+")) {
                String value = (String) field.get(null);
                if (value.startsWith("pe-") && !css.contains("." + value)) {
                    missing.add(value);
                }
            }
        }
        assertEquals(List.of(), missing);
    }

    @Test
    void stylesheetUsesNoGradientsOrBlur() throws IOException {
        String css = css().replaceAll("(?s)/\\*.*?\\*/", "");
        assertTrue(!css.contains("linear-gradient") && !css.contains("radial-gradient"));
        // every effect is a drop shadow with radius 0, a hard offset
        for (String line : css.split("\n")) {
            if (line.contains("-fx-effect")) {
                assertTrue(line.matches(".*dropshadow\\(one-pass-box, [^,]+, 0, 0, [12], [12]\\);.*"), line);
            }
        }
    }

    @Test
    void capsUpperCasesAndTreatsNullAsEmpty() {
        assertEquals("NEW GAME", UiTheme.caps("New Game"));
        assertEquals("", UiTheme.caps(null));
    }
}
