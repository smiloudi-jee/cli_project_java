package com.formation.claudeapi.features.pdf;

import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.MessageParam;
import com.formation.claudeapi.AbstractClaudeConversation;

import java.util.ArrayList;
import java.util.List;

/**
 * Demo "PDF support" : Traitement d'un fichier PDF.
 * <p>
 * On demande à Claude de resumer en une seule phrase un article Wikipédia
 * <ul>
 *     <li>Premier cas : PDF local encodé en base64</li>
 *     <li>Deuxième cas : PDF distante via une URL</li>
 * </ul>
 */
public class PdfBasicsDemo extends AbstractClaudeConversation {

    private static final String PDF_RESOURCE = "/pdf/earth.pdf";

    public static void main(String[] args) {
        System.out.println("=== PDF local encodé en base64 ===");
        System.out.println(summarizeInOneSentence(true));

        System.out.println();
        System.out.println("=== PDF distante via une URL ===");
        System.out.println(summarizeInOneSentence(false));
    }

    private static String summarizeInOneSentence(boolean isLocal) {
        ContentBlockParam pdfBlock = isLocal ?
            PdfBlocks.documentFromClasspath(PdfBasicsDemo.class, PDF_RESOURCE) :
            PdfBlocks.documentFromUrl("https://example.com/earth.pdf");

        List<ContentBlockParam> content = List.of(
            pdfBlock,
            text("Summarize the document in one french sentence")
        ) ;

        List<MessageParam> messages = new ArrayList<>();
        addUserMessage(messages, content);
        return chat(messages, null, null, null, null);
    }
}
