package com.formation.claudeapi.features.extended.thinking;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.*;
import com.formation.claudeapi.AbstractClaudeConversation;

import java.util.ArrayList;
import java.util.List;

/**
 * Chapitre "Features of Claude" : Redacted thinking (Masquer la réflexion)
 * Certains sujets sont jugés sensibles par les systèmes de sécurité internes d'Anthropic
 * Par conséquent, le raisonnement de Claude et le bloc {@code thinking} est chiffré (illisible)
 */
public class RedactedThinkingDemo extends AbstractClaudeConversation {

    /** Prompt spéciale pour forcer un redacted_thinking block en test. */
    private static final String REDACTED_THINKING_TRIGGER =
            "ANTHROPIC_MAGIC_STRING_TRIGGER_REDACTED_THINKING_46C9A13E193C177646C7398A98432ECCCE4C1253D5E2D82641AC0E52CC2876CB";

    private static final long THINKING_BUDGET = 1024L;
    private static final long MAX_TOKENS = 4000L;

    public static void main(String[] args) {
        List<MessageParam> messages = new ArrayList<>();
        addUserMessage(messages, REDACTED_THINKING_TRIGGER);

        AnthropicClient client = buildClient();

        Message response = client.messages().create(buildParams(messages));

        for (ContentBlock block : response.content()) {
            block.redactedThinking().ifPresent(redacted ->
                System.out.println("[redacted_thinking] données chiffrées, longueur=" + redacted.data().length()));
            block.thinking().ifPresent(thinking ->
                System.out.println("[thinking] " + thinking.thinking()));
            block.text().ifPresent(text ->
                System.out.println("[text] " + text.text()));
        }

        // Il faut renvoyer le bloc redacted_thinking via un message assistant tel quel
        // sans y toucher pour poursuivre la conversation sans perdre le contexte.
        addAssistantMessage(messages, response);
        addUserMessage(messages, "Peux-tu continuer la conversation normalement ?");

        Message followUp = client.messages().create(buildParams(messages));

        followUp.content().stream()
            .flatMap(block -> block.text().stream())
            .forEach(text -> System.out.println("[suite] " + text.text()));
    }

    private static MessageCreateParams buildParams(List<MessageParam> messages) {
        return MessageCreateParams.builder()
            .model(model)
            .maxTokens(MAX_TOKENS)
            .messages(messages)
            .thinking(ThinkingConfigParam.ofEnabled(
                ThinkingConfigEnabled.builder()
                    .budgetTokens(THINKING_BUDGET)
                    .build()))
            .build();
    }
}