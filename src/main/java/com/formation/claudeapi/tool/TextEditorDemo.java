package com.formation.claudeapi.tool;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.*;
import com.formation.claudeapi.AbstractClaudeConversation;
import com.formation.claudeapi.tool.existing.TextEditorTool;
import com.formation.claudeapi.tool.existing.ToolSchemasForExistingTools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utilisation du text editor tool : Claude lit/modifie de vrais fichiers.
 */
public class TextEditorDemo extends AbstractClaudeConversation {

    public static void main(String[] args) {
        List<MessageParam> messages = new ArrayList<>();
        addUserMessage(messages, """
                Crée un fichier Greeting.java avec une méthode greeting() qui retourne \
                "Hello, world!". Puis relis le fichier pour vérifier son contenu.\
                Cette classe java devra être dans le package com.formation.claudeapi.tool.existing.greeting.
                """);
        runConversation(messages);
    }

    private static void runConversation(List<MessageParam> messages) {
        AnthropicClient client = buildClient();

        while (true) {
            MessageCreateParams params = MessageCreateParams.builder()
                    .model(model)
                    .maxTokens(1024L)
                    .messages(messages)
                    .tools(List.of(ToolSchemasForExistingTools.TEXT_EDITOR_TOOL))
                    .build();

            Message response = client.messages().create(params);
            addAssistantMessage(messages, response);

            response.content().stream()
                    .flatMap(block -> block.text().stream())
                    .forEach(text -> System.out.println(text.text()));

            List<ToolUseBlock> toolCalls = response.content().stream()
                    .flatMap(block -> block.toolUse().stream())
                    .toList();

            if (toolCalls.isEmpty()) {
                break;
            }

            List<ContentBlockParam> toolResults = new ArrayList<>();
            for (ToolUseBlock call : toolCalls) {
                @SuppressWarnings("unchecked")
                Map<String, Object> input = call._input().convert(Map.class);

                String content;
                boolean isError;
                try {
                    content = TextEditorTool.handle(input);
                    isError = false;
                } catch (Exception e) {
                    content = "Error: " + e.getMessage();
                    isError = true;
                }
                System.out.println((isError ? "[erreur] " : "") + input.get("command") + " -> " + content);

                toolResults.add(ContentBlockParam.ofToolResult(ToolResultBlockParam.builder()
                        .toolUseId(call.id())
                        .content(ToolResultBlockParam.Content.ofString(content))
                        .isError(isError)
                        .build()));
            }
            addUserMessage(messages, toolResults);
        }
    }
}
