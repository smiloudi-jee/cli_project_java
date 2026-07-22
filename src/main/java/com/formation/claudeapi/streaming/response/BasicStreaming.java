package com.formation.claudeapi.streaming.response;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.formation.claudeapi.AbstractClaudeConversation;
import com.anthropic.core.http.StreamResponse;
import com.anthropic.models.messages.RawMessageStreamEvent;
import java.util.ArrayList;
import java.util.List;

public class BasicStreaming extends AbstractClaudeConversation {

    public static void main(String[] args) {
        List<MessageParam> messages = new ArrayList<>();

        addUserMessage(messages, "Write 3 sentences descriptions of a fake database");

        String response = chatStreaming(messages, null);

        System.out.println("Full response: " + response);
    }

    private static String chatStreaming(List<MessageParam> messages, String system) {
        AnthropicClient client = buildClient();

        MessageCreateParams.Builder paramsBuilder = MessageCreateParams.builder()
                .model(model)
                .maxTokens(1000)
                .messages(messages);

        if (system != null) {
            paramsBuilder.system(system);
        }

        StringBuilder fullResponse = new StringBuilder();

        try (StreamResponse<RawMessageStreamEvent> streamResponse =
                     client.messages().createStreaming(paramsBuilder.build())) {

            streamResponse.stream()
                    .flatMap(event -> event.contentBlockDelta().stream())
                    .flatMap(deltaEvent -> deltaEvent.delta().text().stream())
                    .forEach(textDelta -> {
                        System.out.print(textDelta.text());
                        System.out.flush();
                        fullResponse.append(textDelta.text());
                    });
        }

        System.out.println(); // saut de ligne une fois le stream terminé
        return fullResponse.toString();
    }
}
