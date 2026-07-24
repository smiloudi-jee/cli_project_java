package com.formation.claudeapi.tool;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.*;
import com.formation.claudeapi.AbstractClaudeConversation;
import com.formation.claudeapi.tool.existing.ToolSchemasForExistingTools;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilisation du web search tool : entièrement géré côté serveur Anthropic (recherche + lecture des pages),
 */
public class WebSearchDemo extends AbstractClaudeConversation {

    public static void main(String[] args) {
        List<MessageParam> messages = new ArrayList<>();
        addUserMessage(messages, "What are the latest developments in quantum computing?");

        AnthropicClient client = buildClient();

        MessageCreateParams params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(1024L)
                .messages(messages)
                .tools(List.of(ToolSchemasForExistingTools.WEB_SEARCH_TOOL))
                .build();

        Message response = client.messages().create(params);

        for (ContentBlock block : response.content()) {
            block.text().ifPresent(text ->
                    System.out.println("[texte] " + text.text()));
            block.serverToolUse().ifPresent(query ->
                    System.out.println("[recherche exécutée] input=" + query._input()));
            block.webSearchToolResult().ifPresent(result ->
                    System.out.println("[résultats + citations] " + result));
        }
    }
}
