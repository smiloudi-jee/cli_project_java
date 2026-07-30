package com.formation.claudeapi.features.pdf;

import com.anthropic.models.messages.Base64PdfSource;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.DocumentBlockParam;
import com.anthropic.models.messages.TextBlockParam;
import com.anthropic.models.messages.UrlPdfSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

import static com.formation.claudeapi.AbstractClaudeConversation.readResourceBytes;

/**
 * Utilitaires pour construire les blocs de contenu "document" (PDF) "text"
 */
public final class PdfBlocks {

    private PdfBlocks() {
        // classe utilitaire, non instanciable
    }

    /**
     * Construit un bloc "document" encode en base64 à partir d'une ressource
     * du classpath, par exemple "/pdf/earth.pdf".
     */
    public static ContentBlockParam documentFromClasspath(Class<?> anchor, String resourcePath) {
        byte[] bytes = readResourceBytes(anchor, resourcePath);
        String base64Data = Base64.getEncoder().encodeToString(bytes);

        return ContentBlockParam.ofDocument(
            DocumentBlockParam.builder()
                .source(Base64PdfSource.builder()
                    .data(base64Data)
                    .build())
                .build()
        );
    }

    /**
     * Construit un bloc "document" à partir d'une URL publique pointant vers un PDF.
     */
    public static ContentBlockParam documentFromUrl(String url) {
        return ContentBlockParam.ofDocument(
            DocumentBlockParam.builder()
                .source(UrlPdfSource.builder()
                    .url(url)
                    .build())
                .build()
        );
    }
}
