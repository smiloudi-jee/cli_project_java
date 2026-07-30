package com.formation.claudeapi.features.citations;

import com.anthropic.models.messages.CitationCharLocation;
import com.anthropic.models.messages.CitationPageLocation;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.TextBlock;
import com.anthropic.models.messages.TextCitation;

/**
 * Affiche une réponse de Claude ainsi que le detail des citations structurées associées.
 * <p>
 * Chaque bloc "text" de la réponse peut porter une liste de {@link TextCitation}.
 * Chaque citation contient le texte source exact ({@code citedText}),
 * le document concerne (index + titre) et une localisation
 */
public final class CitationPrinter {

    private CitationPrinter() {
        // classe utilitaire, non instanciable
    }

    public static void printResponse(Message message) {
        message.content().stream()
                .flatMap(block -> block.text().stream())
                .forEach(CitationPrinter::printTextBlock);
    }

    private static void printTextBlock(TextBlock textBlock) {
        System.out.println(textBlock.text());

        textBlock.citations().ifPresent(citations -> {
            if (citations.isEmpty()) {
                return;
            }
            System.out.println("--- " + citations.size() + " citation(s) ---");
            citations.forEach(CitationPrinter::printCitation);
        });
    }

    private static void printCitation(TextCitation citation) {
        if (citation.isPageLocation()) {
            CitationPageLocation location = citation.asPageLocation();
            System.out.printf(
                    "[%s, p.%d-%d] \"%s\"%n",
                    location.documentTitle().orElse("document"),
                    location.startPageNumber(),
                    location.endPageNumber(),
                    location.citedText()
            );
        } else if (citation.isCharLocation()) {
            CitationCharLocation location = citation.asCharLocation();
            System.out.printf(
                    "[%s, car.%d-%d] \"%s\"%n",
                    location.documentTitle().orElse("document"),
                    location.startCharIndex(),
                    location.endCharIndex(),
                    location.citedText()
            );
        } else {
            System.out.println("[citation d'un type non gère par cette demo : " + citation + "]");
        }
    }
}
