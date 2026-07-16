package com.formation.mcpchat.core;

import com.formation.mcpchat.MCPClient;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.Tool;
import com.anthropic.models.messages.ToolResultBlockParam;
import com.anthropic.models.messages.ToolUseBlock;

import io.modelcontextprotocol.spec.McpSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Equivalent de tools.py.
 * <p>
 * Fait le pont entre les tools MCP (exposés par les instances MCPClient)
 * et l'API tool-use d'Anthropic (cerveau d'Anthropic).
 */
public class ToolManager {

    /** Récupère tous les tools des clients fournis, convertis au format Tool d'Anthropic. */
    public static List<Tool> getAllTools(Map<String, MCPClient> clients) {
        List<Tool> tools = new ArrayList<>();
        for (MCPClient client : clients.values()) {
            for (McpSchema.Tool mcpTool : client.listTools()) {
                tools.add(Tool.builder()
                        .name(mcpTool.name())
                        .description(mcpTool.description())
                        .inputSchema(toAnthropicInputSchema(mcpTool.inputSchema()))
                        .build());
            }
        }
        return tools;
    }

    /** Convertit un schema JSON MCP brut (Map) en type Tool.InputSchema d'Anthropic. */
    private static Tool.InputSchema toAnthropicInputSchema(Map<String, Object> mcpSchema) {
        Tool.InputSchema.Builder schemaBuilder = Tool.InputSchema.builder();

        if (mcpSchema != null) {
            Object rawProperties = mcpSchema.get("properties");
            if (rawProperties instanceof Map<?, ?> propsMap) {
                Tool.InputSchema.Properties.Builder propsBuilder = Tool.InputSchema.Properties.builder();
                for (Map.Entry<?, ?> entry : propsMap.entrySet()) {
                    propsBuilder.putAdditionalProperty(
                            String.valueOf(entry.getKey()), JsonValue.from(entry.getValue()));
                }
                schemaBuilder.properties(propsBuilder.build());
            }

            Object rawRequired = mcpSchema.get("required");
            if (rawRequired instanceof List<?> requiredList) {
                List<String> required = requiredList.stream().map(String::valueOf).toList();
                schemaBuilder.required(required);
            }
        }

        return schemaBuilder.build();
    }

    /** Trouve le premier client qui possède l'outil spécifié. */
    private static MCPClient findClientWithTool(Map<String, MCPClient> clients, String toolName) {
        for (MCPClient client : clients.values()) {
            for (McpSchema.Tool tool : client.listTools()) {
                if (tool.name().equals(toolName)) {
                    return client;
                }
            }
        }
        return null;
    }

    /** Construit un bloc de contenu de type tool_result (API tool-use). */
    private static ContentBlockParam buildToolResultPart(String toolUseId, String text, boolean isError) {
        return ContentBlockParam.ofToolResult(ToolResultBlockParam.builder()
                .toolUseId(toolUseId)
                .content(ToolResultBlockParam.Content.ofString(text))
                .isError(isError)
                .build());
    }

    /** Execute chaque demande "tool_use" trouvée dans une réponse du modèle et retourne les blocs tool_result. */
    @SuppressWarnings("unchecked")
    public static List<ContentBlockParam> executeToolRequests(Map<String, MCPClient> clients, Message message) {
        List<ContentBlockParam> results = new ArrayList<>();

        List<ToolUseBlock> toolRequests = message.content().stream()
                .flatMap(block -> block.toolUse().stream())
                .toList();

        for (ToolUseBlock toolRequest : toolRequests) {
            String toolUseId = toolRequest.id();
            String toolName = toolRequest.name();
            Map<String, Object> toolInput = toolRequest._input().convert(Map.class);

            MCPClient client = findClientWithTool(clients, toolName);
            if (client == null) {
                results.add(buildToolResultPart(toolUseId, "Could not find that tool", true));
                continue;
            }

            try {
                McpSchema.CallToolResult toolOutput = client.callTool(toolName, toolInput);
                String content = "[]";
                boolean isError = false;
                if (toolOutput != null) {
                    List<String> texts = toolOutput.content().stream()
                            .filter(item -> item instanceof McpSchema.TextContent)
                            .map(item -> ((McpSchema.TextContent) item).text())
                            .toList();
                    content = texts.toString();
                    isError = Boolean.TRUE.equals(toolOutput.isError());
                }
                results.add(buildToolResultPart(toolUseId, content, isError));
            } catch (Exception e) {
                String errorMessage = "Error executing tool '" + toolName + "': " + e.getMessage();
                System.out.println(errorMessage);
                results.add(buildToolResultPart(toolUseId, errorMessage, true));
            }
        }

        return results;
    }
}
