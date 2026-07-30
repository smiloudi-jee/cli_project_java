package com.formation.claudeapi.features.citations;

import com.anthropic.models.messages.Base64PdfSource;
import com.anthropic.models.messages.CitationsConfigParam;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.DocumentBlockParam;
import com.anthropic.models.messages.PlainTextSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static com.formation.claudeapi.AbstractClaudeConversation.readResourceBytes;

/**
 * Citations : Utilitaires pour construire un bloc de contenu "document" avec les citations activées.
 * <p>
 * Par rapport à un bloc "document" classique, deux champs sont ajoutés :
 * - {@code title} : un nom lisible pour le document
 * - {@code citations: {enabled: true}} : demande à Claude de tracer, pour
 *   chaque affirmation, l'endroit exact du document ou il a trouvé l'information.
 * <p>
 * Les citations fonctionnent aussi bien avec un PDF (source base64) qu'avec du texte brut.
 * Seule la localisation retournée change, numéros de page pour un PDF, positions de caractères pour du texte brut.
 */
public final class CitationBlocks {

    private CitationBlocks() {
        // classe utilitaire, non instanciable
    }

    /**
     * Construit un bloc "document" PDF (en base64 depuis le classpath) avec les citations activées.
     */
    public static ContentBlockParam pdfWithCitations(Class<?> anchor, String resourcePath, String title) {
        byte[] bytes = readResourceBytes(anchor, resourcePath);
        String base64Data = Base64.getEncoder().encodeToString(bytes);

        return ContentBlockParam.ofDocument(
            DocumentBlockParam.builder()
                .source(Base64PdfSource.builder()
                    .data(base64Data)
                    .build())
                .title(title)
                .citations(CitationsConfigParam.builder().enabled(true).build()) // Activation des Citations
                .build()
        );
    }

    /**
     * Construit un bloc "document" en texte brut (lu depuis une ressource du classpath) avec les citations activées.
     * Contrairement au PDF, Claude renverra des positions de caractères (start/end char index) plutôt que des numéros de page.
     */
    public static ContentBlockParam plainTextWithCitations(Class<?> anchor, String resourcePath, String title) {
        String text = readResourceText(anchor, resourcePath);

        return ContentBlockParam.ofDocument(
            DocumentBlockParam.builder()
                .source(PlainTextSource.builder()
                    .data(text)
                    .build())
                .title(title)
                .citations(CitationsConfigParam.builder().enabled(true).build())
                .build()
        );
    }

    private static String readResourceText(Class<?> anchor, String resourcePath) {
        return new String(readResourceBytes(anchor, resourcePath), StandardCharsets.UTF_8);
    }
}
