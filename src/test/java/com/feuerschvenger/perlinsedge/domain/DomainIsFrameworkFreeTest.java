package com.feuerschvenger.perlinsedge.domain;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Architecture guard: the domain layer must not depend on JavaFX.
 * Reads the sources directly so no extra tooling is needed.
 */
class DomainIsFrameworkFreeTest {

    private static final Path DOMAIN_SOURCES =
            Path.of("src", "main", "java", "com", "feuerschvenger", "perlinsedge", "domain");

    @Test
    void domainSourcesNeverImportJavaFx() throws IOException {
        assertTrue(Files.isDirectory(DOMAIN_SOURCES), "Domain sources not found at " + DOMAIN_SOURCES.toAbsolutePath());

        List<String> offenders;
        try (Stream<Path> files = Files.walk(DOMAIN_SOURCES)) {
            offenders = files
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(DomainIsFrameworkFreeTest::importsJavaFx)
                    .map(Path::toString)
                    .toList();
        }

        assertTrue(offenders.isEmpty(), "Domain files importing javafx: " + offenders);
    }

    @Test
    void domainSourcesDoNotReferenceFxInfrastructure() throws IOException {
        List<String> offenders;
        try (Stream<Path> files = Files.walk(DOMAIN_SOURCES)) {
            offenders = files
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> contains(p, "perlinsedge.infra.fx"))
                    .map(Path::toString)
                    .toList();
        }

        assertTrue(offenders.isEmpty(),"Domain files referencing infra.fx: " + offenders);
    }

    private static boolean importsJavaFx(Path file) {
        return contains(file, "import javafx");
    }

    private static boolean contains(Path file, String needle) {
        try {
            return Files.readAllLines(file).stream().anyMatch(line -> line.contains(needle));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + file, e);
        }
    }
}
