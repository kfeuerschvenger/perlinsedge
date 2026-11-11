package com.feuerschvenger.perlinsedge.domain;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Architecture guard: the domain layer must not depend on JavaFX or on the outer layers of the project.
 * Reads the sources directly, with comments removed, so no extra tooling is needed.
 */
class DomainIsFrameworkFreeTest {

    private static final Path DOMAIN_SOURCES =
            Path.of("src", "main", "java", "com", "feuerschvenger", "perlinsedge", "domain");

    private static final Pattern JAVAFX_USAGE = Pattern.compile("\\bjavafx\\.[\\w.]*");
    private static final Pattern OUTER_LAYER_USAGE =
            Pattern.compile("\\bcom\\.feuerschvenger\\.perlinsedge\\.(?:app|ui|infra)\\b[\\w.]*");

    /**
     * Known layering debt, keyed by the exact path of the file relative to the domain folder.
     * Only references that start with the listed package prefix are exempt, any other outer-layer reference in the
     * same file still fails the build. An entry whose file no longer has an exempt reference fails the build too,
     * so the debt list can only shrink. Do not add entries.
     */
    private static final Map<String, String> KNOWN_OUTER_LAYER_DEBT =
            Map.of("world/generation/PerlinMapGenerator.java", "com.feuerschvenger.perlinsedge.infra.noise");

    @Test
    void domainSourcesNeverUseJavaFx() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (Path file : domainFiles()) {
            if (JAVAFX_USAGE.matcher(codeOf(file)).find()) {
                offenders.add(relative(file));
            }
        }

        assertTrue(offenders.isEmpty(), "Domain files using javafx (import or qualified name): " + offenders);
    }

    @Test
    void domainSourcesDoNotDependOnOuterLayers() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (Path file : domainFiles()) {
            String path = relative(file);
            String exemptPrefix = KNOWN_OUTER_LAYER_DEBT.get(path);
            for (String reference : outerLayerReferences(codeOf(file))) {
                if (exemptPrefix == null || !reference.startsWith(exemptPrefix)) {
                    offenders.add(path + " -> " + reference);
                }
            }
        }

        assertTrue(offenders.isEmpty(), "Domain files depending on app, ui or infra: " + offenders);
    }

    @Test
    void everyKnownDebtEntryStillViolatesSoTheListOnlyShrinks() throws IOException {
        List<String> stale = new ArrayList<>();
        for (Map.Entry<String, String> entry : KNOWN_OUTER_LAYER_DEBT.entrySet()) {
            Path file = DOMAIN_SOURCES.resolve(entry.getKey());
            assertTrue(Files.isRegularFile(file), "Debt entry points at a missing file: " + entry.getKey());
            boolean stillViolates = outerLayerReferences(codeOf(file)).stream()
                    .anyMatch(reference -> reference.startsWith(entry.getValue()));
            if (!stillViolates) {
                stale.add(entry.getKey());
            }
        }

        assertTrue(stale.isEmpty(), "Remove these entries from KNOWN_OUTER_LAYER_DEBT, they no longer violate: " + stale);
    }

    @Test
    void commentStrippingKeepsCodeAfterAStringThatLooksLikeAComment() {
        String source = "String url = \"http://example.com\"; int a = 1; // trailing\nint b = 2;";

        String code = stripComments(source);

        assertTrue(code.contains("\"http://example.com\""), code);
        assertTrue(code.contains("int a = 1;"), code);
        assertTrue(code.contains("int b = 2;"), code);
        assertEquals(-1, code.indexOf("trailing"), code);
    }

    @Test
    void commentStrippingKeepsAStringThatContainsABlockCommentOpener() {
        String source = "String glob = \"src/**/*.java\"; import javafx.scene.Node; /* gone */ int c = 3;";

        String code = stripComments(source);

        assertTrue(code.contains("\"src/**/*.java\""), code);
        assertTrue(code.contains("import javafx.scene.Node;"), code);
        assertTrue(code.contains("int c = 3;"), code);
        assertEquals(-1, code.indexOf("gone"), code);
    }

    @Test
    void commentStrippingRemovesReferencesThatLiveOnlyInComments() {
        String source = "// import javafx.scene.Node;\n/* com.feuerschvenger.perlinsedge.infra.Foo */\n"
                + "/** {@link javafx.scene.Node} */\nclass A { char slash = '/'; char quote = '\"'; String s = \"a\\\"//b\"; }";

        String code = stripComments(source);

        assertTrue(outerLayerReferences(code).isEmpty(), code);
        assertTrue(!JAVAFX_USAGE.matcher(code).find(), code);
        assertTrue(code.contains("char slash = '/';"), code);
        assertTrue(code.contains("String s = \"a\\\"//b\";"), code);
    }

    @Test
    void commentStrippingHandlesTextBlocks() {
        String source = "String t = \"\"\"\n  // not a comment\n  /* nor this */\n  \"\"\"; int d = 4; // real comment";

        String code = stripComments(source);

        assertTrue(code.contains("// not a comment"), code);
        assertTrue(code.contains("/* nor this */"), code);
        assertTrue(code.contains("int d = 4;"), code);
        assertEquals(-1, code.indexOf("real comment"), code);
    }

    private static List<String> outerLayerReferences(String code) {
        List<String> references = new ArrayList<>();
        Matcher matcher = OUTER_LAYER_USAGE.matcher(code);
        while (matcher.find()) {
            references.add(matcher.group());
        }
        return references;
    }

    private static List<Path> domainFiles() throws IOException {
        assertTrue(Files.isDirectory(DOMAIN_SOURCES), "Domain sources not found at " + DOMAIN_SOURCES.toAbsolutePath());

        try (Stream<Path> files = Files.walk(DOMAIN_SOURCES)) {
            return files.filter(p -> p.toString().endsWith(".java")).toList();
        }
    }

    private static String relative(Path file) {
        return DOMAIN_SOURCES.relativize(file).toString().replace('\\', '/');
    }

    private static String codeOf(Path file) {
        try {
            return stripComments(Files.readString(file));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + file, e);
        }
    }

    /**
     * Removes line and block comments while leaving string, char and text block literals untouched,
     * so a literal such as {@code "http://x"} or {@code "src/**"} is never mistaken for a comment.
     */
    static String stripComments(String source) {
        StringBuilder out = new StringBuilder(source.length());
        int i = 0;
        int n = source.length();
        while (i < n) {
            char c = source.charAt(i);
            if (source.startsWith("\"\"\"", i)) {
                int end = source.indexOf("\"\"\"", i + 3);
                int stop = end < 0 ? n : end + 3;
                out.append(source, i, stop);
                i = stop;
            } else if (c == '"' || c == '\'') {
                int stop = endOfQuoted(source, i, c);
                out.append(source, i, stop);
                i = stop;
            } else if (source.startsWith("//", i)) {
                int end = source.indexOf('\n', i);
                i = end < 0 ? n : end;
            } else if (source.startsWith("/*", i)) {
                int end = source.indexOf("*/", i + 2);
                i = end < 0 ? n : end + 2;
                out.append(' ');
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    private static int endOfQuoted(String source, int start, char quote) {
        int i = start + 1;
        while (i < source.length()) {
            char c = source.charAt(i);
            if (c == '\\') {
                i += 2;
            } else if (c == quote || c == '\n') {
                return i + 1;
            } else {
                i++;
            }
        }
        return source.length();
    }
}
