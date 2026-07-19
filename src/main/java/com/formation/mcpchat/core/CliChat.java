package com.formation.mcpchat.core;

import com.formation.mcpchat.MCPClient;

import com.anthropic.models.messages.MessageParam;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.modelcontextprotocol.spec.McpSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Equivalent de cli_chat.py, ajoute deux choses spécifiques à la CLI :
 * * 1. Détecter et résoudre les mentions @document (aller chercher le contenu du doc via MCP avant d'envoyer le message).
 * * 2. Interpréter les commandes /xxx (les traduire en prompts MCP plutôt que de les envoyer telles quelles à Claude).
 * <p>
 * * C'est la couche qui fait le pont entre l'interface utilisateur (CLI) et la logique de chat pure.
 */
public class CliChat extends Chat {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final MCPClient docClient;

    public CliChat(MCPClient docClient, Map<String, MCPClient> clients, Claude claudeService) {
        super(claudeService, clients);
        this.docClient = docClient;
    }

    public List<McpSchema.Prompt> listPrompts() {
        return docClient.listPrompts();
    }

    public List<String> listDocIds() {
        McpSchema.ReadResourceResult result = docClient.readResource("docs://documents");
        List<String> ids = new ArrayList<>();
        for (McpSchema.ResourceContents content : result.contents()) {
            if (content instanceof McpSchema.TextResourceContents textContent) {
                ids.addAll(parseJsonStringList(textContent.text()));
            }
        }
        return ids;
    }

    public String getDocContent(String docId) {
        McpSchema.ReadResourceResult result = docClient.readResource("docs://documents/" + docId);
        StringBuilder content = new StringBuilder();
        for (McpSchema.ResourceContents item : result.contents()) {
            if (item instanceof McpSchema.TextResourceContents textContent) {
                content.append(textContent.text());
            }
        }
        return content.toString();
    }

    public McpSchema.GetPromptResult getPrompt(String command, String docId) {
        return docClient.getPrompt(command, Map.of("doc_id", docId));
    }

    private String extractResources(String query) {
        List<String> mentions = new ArrayList<>();
        for (String word : query.split("\\s+")) {
            if (word.startsWith("@")) {
                mentions.add(word.substring(1));
            }
        }
        if (mentions.isEmpty()) {
            return "";
        }

        List<String> docIds = listDocIds();
        StringBuilder builder = new StringBuilder();
        for (String docId : docIds) {
            if (mentions.contains(docId)) {
                String content = getDocContent(docId);
                builder.append("\n<document id=\"").append(docId).append("\">\n")
                        .append(content)
                        .append("\n</document>\n");
            }
        }
        return builder.toString();
    }

    private boolean processCommand(String query) {
        if (!query.startsWith("/")) {
            return false;
        }

        String[] words = query.split("\\s+");
        String command = words[0].replace("/", "");
        String docId = words.length > 1 ? words[1] : "";

        McpSchema.GetPromptResult promptResult = getPrompt(command, docId);
        messages.addAll(convertPromptMessagesToMessageParams(promptResult.messages()));
        return true;
    }

    @Override
    protected void processQuery(String query) {
        if (processCommand(query)) {
            return;
        }

        String addedResources = extractResources(query);

        String prompt = """
                The user has a question:
                <query>
                %s
                </query>

                The following context may be useful in answering their question:
                <context>
                %s
                </context>

                Note the user's query might contain references to documents like "@report.docx". The "@" is only
                included as a way of mentioning the doc. The actual name of the document would be "report.docx".
                If the document content is included in this prompt, you don't need to use an additional tool to read the document.
                Answer the user's question directly and concisely. Start with the exact information they need.
                Don't refer to or mention the provided context in any way - just use it to inform your answer.
                """.formatted(query, addedResources);

        claudeService.addUserMessage(messages, prompt);
    }

    private List<String> parseJsonStringList(String json) {
        try {
            return JSON.readValue(json, JSON.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            return List.of();
        }
    }

    private static MessageParam convertPromptMessageToMessageParam(McpSchema.PromptMessage promptMessage) {
        MessageParam.Role role = promptMessage.role() == McpSchema.Role.USER
                ? MessageParam.Role.USER
                : MessageParam.Role.ASSISTANT;

        String text = promptMessage.content() instanceof McpSchema.TextContent textContent
                ? textContent.text()
                : "";

        return MessageParam.builder()
                .role(role)
                .content(MessageParam.Content.ofString(text))
                .build();
    }

    private static List<MessageParam> convertPromptMessagesToMessageParams(List<McpSchema.PromptMessage> promptMessages) {
        return promptMessages.stream()
                .map(CliChat::convertPromptMessageToMessageParam)
                .toList();
    }
}
