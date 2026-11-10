package com.feuerschvenger.perlinsedge.domain;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Architecture guard: the domain layer must not depend on JavaFX or on the outer layers of the project.
 * Reads the sources directly, with comments removed, so no extra tooling is needed.
 */
class DomainIsFrameworkFreeTest {

    private static final Path DOMAIN_SOURCES =
            Path.of("src", "main", "java", "com", "feuerschvenger", "perlinsedge", "domain");

    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern LINE_COMMENT = Pattern.compile("//[^\\n]*");
    private static final Pattern JAVAFX_USAGE = Pattern.compile("\\bjavafx\\.");
    private static final Pattern OUTER_LAYER_USAGE =
            Pattern.compile("\\bcom\\.feuerschvenger\\.perlinsedge\\.(app|ui|infra)\\b");

    @Test
    void domainSourcesNeverUseJavaFx() throws IOException {
        List<String> offenders = offendersMatching(JAVAFX_USAGE);

        assertTrue(offenders.isEmpty(), "Domain files using javafx (import or qualified name): " + offenders);
    }

    /**
     * Known layering debt: the map generator builds its noise sources from the infra noise adapter.
     * Remove this entry once the noise port lives in the domain; no other file may be added to the list.
     */
    private static final Set<String> KNOWN_OUTER_LAYER_DEBT = Set.of("PerlinMapGenerator.java");

    @Test
    void domainSourcesDoNotDependOnOuterLayers() throws IOException {
        List<String> offenders = offendersMatching(OUTER_LAYER_USAGE).stream()
                .filter(file -> KNOWN_OUTER_LAYER_DEBT.stream().noneMatch(file::endsWith))
                .toList();

        assertTrue(offenders.isEmpty(), "Domain files depending on app, ui or infra: " + offenders);
    }

    private static List<String> offendersMatching(Pattern pattern) throws IOException {
        assertTrue(Files.isDirectory(DOMAIN_SOURCES), "Domain sources not found at " + DOMAIN_SOURCES.toAbsolutePath());

        try (Stream<Path> files = Files.walk(DOMAIN_SOURCES)) {
            return files
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> pattern.matcher(codeOf(p)).find())
                    .map(Path::toString)
                    .toList();
        }
    }

    /** Source text with block and line comments removed. */
    private static String codeOf(Path file) {
        try {
            String source = Files.readString(file);
            String withoutBlocks = BLOCK_COMMENT.matcher(source).replaceAll("");
            return LINE_COMMENT.matcher(withoutBlocks).replaceAll("");
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + file, e);
        }
    }
}
