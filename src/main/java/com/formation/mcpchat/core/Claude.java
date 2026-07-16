package com.formation.mcpchat.core;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Wrapper client pour le SDK Java Anthropic (equivalent de claude.py).
 * <p>
 * Centralise la construction des messages (user/assistant) et l'appel à
 * l'API Claude : ajout des tours de parole utilisateur (texte ou blocs de contenu,
 * ex. résultats de tools), ajout des tours de parole assistant, extraction du texte
 * d'une réponse, et envoi de la conversation au modèle.
 */
public class Claude {

    private final AnthropicClient client;
    private final String model;

    public Claude(String model) {
        this.client = AnthropicOkHttpClient.fromEnv();
        this.model = model;
    }

    /** Ajoute un tour de parole utilisateur construit à partir de texte brut. */
    public void addUserMessage(List<MessageParam> messages, String text) {
        messages.add(MessageParam.builder()
                .role(MessageParam.Role.USER)
                .content(MessageParam.Content.ofString(text))
                .build());
    }

    /** Ajoute un tour de parole utilisateur construit à partir de blocs de contenu (e.g. résultats de tools). */
    public void addUserMessage(List<MessageParam> messages, List<ContentBlockParam> content) {
        messages.add(MessageParam.builder()
                .role(MessageParam.Role.USER)
                .content(MessageParam.Content.ofBlockParams(content))
                .build());
    }

    /** Ajoute un tour de parole assistant construit à partir d'une réponse de modèle. */
    public void addAssistantMessage(List<MessageParam> messages, Message message) {
        messages.add(message.toParam());
    }

    /** Concatène tous les blocs de texte d'une réponse en une seule chaine. */
    public String textFromMessage(Message message) {
        return message.content().stream()
                .flatMap(block -> block.text().stream())
                .map(TextBlock::text)
                .collect(Collectors.joining("\n"));
    }

    public Message chat(List<MessageParam> messages, List<Tool> tools) {
        return chat(messages, null, 1.0, List.of(), tools);
    }

    public Message chat(
            List<MessageParam> messages,
            String system,
            double temperature,
            List<String> stopSequences,
            List<Tool> tools
    ) {
        MessageCreateParams.Builder paramsBuilder = MessageCreateParams.builder()
                .model(Model.of(model))
                .maxTokens(8000L)
                .temperature(temperature)
                .messages(messages);

        if (stopSequences != null && !stopSequences.isEmpty()) {
            paramsBuilder.stopSequences(stopSequences);
        }
        if (tools != null && !tools.isEmpty()) {
            paramsBuilder.tools(tools.stream().map(ToolUnion::ofTool).toList());
        }
        if (system != null && !system.isBlank()) {
            paramsBuilder.system(system);
        }
        return client.messages().create(paramsBuilder.build());
    }
}