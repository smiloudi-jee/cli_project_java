package com.formation.claudeapi.first.access;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.formation.claudeapi.AbstractClaudeConversation;

public class FirstRequest extends AbstractClaudeConversation {

    public static void main(String[] args) {
        System.out.println("Hello, this is the first request to Claude by using Claude API!");
        AnthropicClient client = AnthropicOkHttpClient.builder()
            .apiKey(dotenv.get("ANTHROPIC_API_KEY"))
            .build();

        MessageCreateParams params = MessageCreateParams.builder()
            .model(model)
            .maxTokens(1000)
            .addUserMessage("What is quantum computing? Answer in one sentence")
            .build();

        Message message = client.messages().create(params);

        String responseText = message.content().getFirst().asText().text();
        System.out.println(responseText);
    }
}
