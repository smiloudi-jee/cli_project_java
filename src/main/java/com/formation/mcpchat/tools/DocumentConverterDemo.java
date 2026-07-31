package com.formation.mcpchat.tools;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Reprend les fichiers du cours mcp_docs.docx & mcp_docs.pdf présents dans src/main/resources/document.
 * Vérifie que la conversion Markdown produit bien un résultat non vide pour chaque format, ainsi que le
 * cas d'erreur fichier introuvable, et sauvegarde le Markdown généré dans src/main/resources
 * (même nom que le fichier source, extension ".md").
 */
public class DocumentConverterDemo {

    private static final String DOCX_RESOURCE = "/document/mcp_docs.docx";
    private static final String PDF_RESOURCE = "/document/mcp_docs.pdf";

    private static final Path OUTPUT_DIR =
            Path.of("src/main/resources/document/converter-output");

    public static void main(String[] args) throws IOException {
        check("docx -> markdown", () -> convertAndSave(DOCX_RESOURCE, OUTPUT_DIR.resolve("docx")));
        check("pdf -> markdown", () -> convertAndSave(PDF_RESOURCE, OUTPUT_DIR.resolve("pdf")));

        check("fichier introuvable -> IOException", () -> {
            try {
                DocumentConverter.documentPathToMarkdown("does_not_exist.pdf");
                throw new AssertionError("Une IOException était attendue");
            } catch (IOException expected) {
                System.out.println("IOException recue comme attendu : " + expected.getMessage());
            }
        });
    }

    /**
     * Copie la ressource source dans un dossier temporaire (hors du dossier
     * de sortie, supprimé aussitôt après) le temps d'appeler
     * {@code documentPathToMarkdown}, puis n'écrit que le résultat Markdown
     * dans {@code outputDir} : le fichier source n'est jamais recréé dans le
     * dossier de sortie.
     */
    private static void convertAndSave(String resourcePath, Path outputDir) throws IOException {
        String fileName = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);

        Path stagingDir = Files.createTempDirectory("document-converter-demo");
        stagingDir.toFile().deleteOnExit();
        Path stagedSource = stagingDir.resolve(fileName);

        try (InputStream is = DocumentConverterDemo.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Ressource introuvable dans le classpath : " + resourcePath);
            }
            Files.copy(is, stagedSource, StandardCopyOption.REPLACE_EXISTING);
        }

        String markdown;
        try {
            markdown = DocumentConverter.documentPathToMarkdown(stagedSource.toString());
        } finally {
            Files.deleteIfExists(stagedSource);
            Files.deleteIfExists(stagingDir);
        }
        assertNonEmpty(markdown);

        Files.createDirectories(outputDir);
        Path markdownPath = outputDir.resolve(withExtension(fileName, "md"));
        Files.writeString(markdownPath, markdown, StandardCharsets.UTF_8);

        System.out.println("Markdown sauvegarde : " + markdownPath);
        System.out.println(markdown.substring(0, Math.min(200, markdown.length())) + "...");
    }

    /** Remplace l'extension d'un nom de fichier en conservant le reste a l'identique. */
    private static String withExtension(String fileName, String newExtension) {
        int dotIndex = fileName.lastIndexOf('.');
        String baseName = dotIndex < 0 ? fileName : fileName.substring(0, dotIndex);
        return baseName + "." + newExtension;
    }

    private static void assertNonEmpty(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            throw new AssertionError("Le markdown genere est vide");
        }
    }

    private static void check(String label, ThrowingRunnable action) {
        System.out.println("=== " + label + " ===");
        try {
            action.run();
            System.out.println("OK\n");
        } catch (Exception e) {
            System.out.println("ECHEC : " + e + "\n");
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
