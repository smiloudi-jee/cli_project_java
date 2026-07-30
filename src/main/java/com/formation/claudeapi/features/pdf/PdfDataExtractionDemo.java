package com.formation.claudeapi.features.pdf;

import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.MessageParam;
import com.formation.claudeapi.AbstractClaudeConversation;

import java.util.ArrayList;
import java.util.List;

/**
 * Demo "Ce que Claude peut faire avec un fichier PDF" : Extraction ciblée de données.
 */
public class PdfDataExtractionDemo extends AbstractClaudeConversation {

    private static final String PDF_RESOURCE = "/pdf/earth.pdf";

    private static final String EXTRACTION_PROMPT = """
            Dans le tableau "Physical characteristics" de ce document, retrouve les \
            valeurs suivantes et restitue-les sous la forme d'une liste \
            "libelle : valeur" :
            - Mean radius
            - Mass
            - Mean density
            - Surface gravity
            - Escape velocity

            Si une valeur est absente du tableau, indique "non trouve" plutôt que \
            d'inventer un chiffre.""";

    public static void main(String[] args) {
        System.out.println(extractPhysicalCharacteristics());
    }

    private static String extractPhysicalCharacteristics() {
        List<ContentBlockParam> content = List.of(
                PdfBlocks.documentFromClasspath(PdfDataExtractionDemo.class, PDF_RESOURCE),
                text(EXTRACTION_PROMPT)
        );

        List<MessageParam> messages = new ArrayList<>();
        addUserMessage(messages, content);
        return chat(messages, null, null, null, null);
    }
}
