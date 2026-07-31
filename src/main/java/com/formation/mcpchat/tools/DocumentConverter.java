package com.formation.mcpchat.tools;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Section de cours "Claude Code in action".
 */
public final class DocumentConverter {

    private static final Pattern HEADING_STYLE =
            Pattern.compile("^Heading(\\d+)$", Pattern.CASE_INSENSITIVE);

    private DocumentConverter() {
        // classe utilitaire, non instanciable
    }

    /**
     * Convertit des données binaires de document ({@code docx} ou {@code pdf}) en texte Markdown.
     */
    public static String binaryDocumentToMarkdown(byte[] binaryData, String fileType) {
        String normalizedType = fileType.toLowerCase(Locale.ROOT).replace(".", "");

        return switch (normalizedType) {
            case "docx" -> docxToMarkdown(binaryData);
            case "pdf" -> pdfToMarkdown(binaryData);
            default -> throw new IllegalArgumentException("Type de fichier non supporte : " + fileType);
        };
    }

    /**
     * Valide que le fichier existe, détermine son type à partir de l'extension du chemin,
     * lit les octets, puis délègue à {@link #binaryDocumentToMarkdown}.
     *
     * @throws IOException si aucun fichier n'existe au chemin donné.
     */
    public static String documentPathToMarkdown(String filePath) throws IOException {
        Path path = Path.of(filePath);
        if (!Files.isRegularFile(path)) {
            throw new IOException("Fichier introuvable : " + filePath);
        }

        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0) {
            throw new IllegalArgumentException(
                "Impossible de determiner le type de fichier (pas d'extension) : " + filePath);
        }
        String fileType = fileName.substring(dotIndex + 1);

        byte[] binaryData = Files.readAllBytes(path);
        return binaryDocumentToMarkdown(binaryData, fileType);
    }

    private static String docxToMarkdown(byte[] binaryData) {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(binaryData))) {
            StringBuilder markdown = new StringBuilder();

            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText();
                if (text == null || text.isBlank()) {
                    continue;
                }

                Matcher headingMatcher = HEADING_STYLE.matcher(String.valueOf(paragraph.getStyle()));
                if (headingMatcher.matches()) {
                    int level = Math.min(6, Integer.parseInt(headingMatcher.group(1)));
                    markdown.append("#".repeat(level)).append(' ').append(text).append("\n\n");
                } else if (paragraph.getNumID() != null) {
                    markdown.append("- ").append(text).append('\n');
                } else {
                    markdown.append(text).append("\n\n");
                }
            }

            return markdown.toString().trim();
        } catch (IOException e) {
            throw new RuntimeException("Impossible de lire le document docx", e);
        }
    }

    private static String pdfToMarkdown(byte[] binaryData) {
        try (PDDocument document = PDDocument.load(binaryData)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document).trim();
        } catch (IOException e) {
            throw new RuntimeException("Impossible de lire le document pdf", e);
        }
    }
}
