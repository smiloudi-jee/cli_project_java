package com.formation.mcpchat.core;

import com.formation.mcpchat.MCPClient;

import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Equivalent de core/chat.py
 * <p>
 * Cette classe encapsule le cycle complet d'un échange avec Claude,
 * boucle de conversation générique qui dialogue avec Claude et execute
 * les tools MCP demandes, jusqu'à obtenir une réponse textuelle finale.
 */
public class Chat {

    protected final Claude claudeService;
    protected final Map<String, MCPClient> clients;
    protected final List<MessageParam> messages = new ArrayList<>();

    public Chat(Claude claudeService, Map<String, MCPClient> clients) {
        this.claudeService = claudeService;
        this.clients = clients;
    }

    protected void processQuery(String query) {
        claudeService.addUserMessage(messages, query);
    }

    public String run(String query) {
        String finalTextResponse = "";

        processQuery(query);

        while (true) {
            Message response = claudeService.chat(messages, ToolManager.getAllTools(clients));

            claudeService.addAssistantMessage(messages, response);

            boolean usedTool = response.content().stream()
                    .anyMatch(block -> block.toolUse().isPresent());

            if (usedTool) {
                System.out.println(claudeService.textFromMessage(response));
                List<ContentBlockParam> toolResultParts = ToolManager.executeToolRequests(clients, response);
                claudeService.addUserMessage(messages, toolResultParts);
            } else {
                finalTextResponse = claudeService.textFromMessage(response);
                break;
            }
        }

        return finalTextResponse;
    }
}
