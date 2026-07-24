package com.formation.claudeapi;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.Tool;
import com.anthropic.models.messages.ToolUnion;
import com.networknt.schema.utils.Strings;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.List;

public abstract class AbstractClaudeConversation {
    protected static final Dotenv dotenv = Dotenv.configure().load();
    protected static Model model = Model.CLAUDE_SONNET_4_5;
    private static final long MAX_TOKENS = 1024L;
//    protected static final String stringModel = "claude-sonnet-5";

    protected static AnthropicClient buildClient(){
        return AnthropicOkHttpClient.builder()
                .apiKey(dotenv.get("ANTHROPIC_API_KEY"))
                .build();
    }

    public static void addUserMessage(List<MessageParam> messages, String text) {
        messages.add(MessageParam.builder()
                .role(MessageParam.Role.USER)
                .content(MessageParam.Content.ofString(text))
                .build());
    }

    protected static void addAssistantMessage(List<MessageParam> messages, String text) {
        messages.add(MessageParam.builder()
                .role(MessageParam.Role.ASSISTANT)
                .content(MessageParam.Content.ofString(text))
                .build());
    }

    /** Rejoue la réponse brute de Claude (texte et/ou tool_use) comme tour "assistant". */
    protected static void addAssistantMessage(List<MessageParam> messages, Message message) {
        messages.add(message.toParam());
    }

    /** Ajoute un tour "user" construit à partir de blocs de contenu (ex. des tool_result). */
    protected static void addUserMessage(List<MessageParam> messages, List<ContentBlockParam> content) {
        messages.add(MessageParam.builder()
                .role(MessageParam.Role.USER)
                .content(MessageParam.Content.ofBlockParams(content))
                .build());
    }

    protected static String chat(
            List<MessageParam> messages,
            String systemPrompt,
            List<String> stopSequences,
            Double temperature,
            List<Tool> tools) {
        AnthropicClient client = buildClient();

        MessageCreateParams.Builder params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(MAX_TOKENS)
                .messages(messages);

        if(!Strings.isBlank(systemPrompt)) {
            params.system(systemPrompt);
        }

        boolean estVideOuNull = stopSequences == null || stopSequences.isEmpty();
        if(!estVideOuNull) {
            params.stopSequences(stopSequences);
        }

        if (temperature != null) {
            params.temperature(temperature);
        }

        if (tools != null && !tools.isEmpty()) {
            params.tools(tools.stream().map(ToolUnion::ofTool).toList());
        }

        Message messageResponse = client.messages().create(params.build());
        return messageResponse.content().getFirst().asText().text();
    }

    protected static Message chatWithTool(
            List<MessageParam> messages,
            List<Tool> tools) {

        AnthropicClient client = buildClient();

        MessageCreateParams.Builder params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(MAX_TOKENS)
                .messages(messages);

        if (tools != null && !tools.isEmpty()) {
            params.tools(tools.stream().map(ToolUnion::ofTool).toList());
        }

        return client.messages().create(params.build());
    }
}
