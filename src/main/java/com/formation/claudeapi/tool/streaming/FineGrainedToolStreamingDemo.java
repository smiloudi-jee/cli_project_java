package com.formation.claudeapi.tool.streaming;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.http.StreamResponse;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.RawMessageStreamEvent;
import com.anthropic.models.messages.ToolUnion;
import com.formation.claudeapi.AbstractClaudeConversation;
import com.formation.claudeapi.tool.existing.ToolSchemasForExistingTools;

import java.util.ArrayList;
import java.util.List;

/**
 * Section "Fine grained tool calling" : "Streaming des arguments JSON d'un tool_use".
 * Sans activation spéciale, l'API bufferiser les fragments et ne les livre que par
 * paire clé/valeur une fois complèté et validée, d'où l'effet "un peu rafale".
 * Le mode Fine-Grained lève ce buffer : les fragments de JSON arrivent au fil de l'eau,
 * potentiellement invalides tant que l'objet n'est pas complet, à nous de les accumuler
 * et de ne parser qu'à la fin (ou d'utiliser un parseur JSON tolérant aux erreurs).
 */
public class FineGrainedToolStreamingDemo extends AbstractClaudeConversation {

    public static void main(String[] args) {
        List<MessageParam> messages = new ArrayList<>();
        addUserMessage(messages, "Generate and save an article about computer science");

        AnthropicClient client = buildClient();

        MessageCreateParams params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(1024L)
                .messages(messages)
                .tools(List.of(ToolUnion.ofTool(SaveArticleTool.SAVE_ARTICLE_SCHEMA)))
                .build();

        try (StreamResponse<RawMessageStreamEvent> streamResponse = client.messages().createStreaming(params)) {
            streamResponse.stream()
                    .flatMap(event -> event.contentBlockDelta().stream())
                    .forEach(deltaEvent -> {
                        deltaEvent.delta().text().ifPresent(textDelta ->
                                System.out.print(textDelta.text()));
                        deltaEvent.delta().inputJson().ifPresent(inputJsonDelta ->
                                System.out.println("[input_json] partial_json=\"" + inputJsonDelta.partialJson() + "\""));
                    });
        }

        System.out.println();
    }
}
