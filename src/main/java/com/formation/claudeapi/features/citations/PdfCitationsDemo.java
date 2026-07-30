package com.formation.claudeapi.features.citations;

import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageParam;
import com.formation.claudeapi.AbstractClaudeConversation;

import java.util.ArrayList;
import java.util.List;

/**
 * Demo "Enabling Citations" - avec un document PDF.
 */
public class PdfCitationsDemo extends AbstractClaudeConversation {

    private static final String PDF_RESOURCE = "/pdf/earth.pdf";
    private static final String DOCUMENT_PDF_TITLE = "earth.pdf";

    private static final String ARTICLE_RESOURCE = "/citations/earth-article.txt";
    private static final String DOCUMENT_TEXTE_TITLE = "Earth Article";

    private static final String QUESTION = "How were Earth's atmosphere and oceans formed?";

    public static void main(String[] args) {
        System.out.println("=== Citation from PDF local ===");
        CitationPrinter.printResponse(citations(true));

        System.out.println();
        System.out.println("=== Citation from Text (extract from PDF local) ===");
        CitationPrinter.printResponse(citations(false));

    }

    private static Message citations(boolean isPdf){
        ContentBlockParam documentBlock = isPdf ?
            CitationBlocks.pdfWithCitations(PdfCitationsDemo.class, PDF_RESOURCE, DOCUMENT_PDF_TITLE) :
            CitationBlocks.plainTextWithCitations(PdfCitationsDemo.class, ARTICLE_RESOURCE, DOCUMENT_TEXTE_TITLE);

        List<ContentBlockParam> content = List.of(documentBlock, text(QUESTION));

        List<MessageParam> messages = new ArrayList<>();
        addUserMessage(messages, content);

        return chatWithTool(messages, null);
    }

}
