package com.formation.claudeapi.features.extended.thinking;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.*;
import com.formation.claudeapi.AbstractClaudeConversation;

import java.util.ArrayList;
import java.util.List;

/**
 * Chapitre "Features of Claude" : Extended thinking (Afficher la Réflexion)
 * La réponse contient alors deux blocs au lieu d'un seul :
 * <ul>
 *   <li>Un bloc {@code thinking}, le raisonnement visible</li>
 *   <li>Un bloc {@code text} habituel (la réponse finale)</li>
 * </ul>
 * <p>
 * Le bloc thinking est chiffré et signé de à l'aide d'une clé cryptographique qui prouve que
 * ce texte a bien été généré par Claude et n'a pas été modifié. C'est ce qui empêche un
 * développeur de trafiquer le raisonnement pour orienter le modèle vers une direction dangereuse.
 * <p>
 * Contraintes à connaître :
 * <ul>
 *   <li>Incompatible avec le Pre-Filling de message et avec le paramètre {@code temperature} ;</li>
 *   <li>Coût et latence plus élevés (on paie les tokens de thinking) : à activer seulement
 *   si l'évaluation de prompt (voir le chapitre précédent) montre qu'un prompt bien
 *   optimisé ne suffit pas.</li>
 * </ul>
 */
public class ExtendedThinkingDemo extends AbstractClaudeConversation {

    private static final long THINKING_BUDGET = 1024L;
    private static final long MAX_TOKENS = 4000L;
    private static final String USER_MESSAGE = "Write a short guide on recursion";

    public static void main(String[] args) {
        List<MessageParam> messages = new ArrayList<>();
        addUserMessage(messages, USER_MESSAGE);

        AnthropicClient client = buildClient();

        MessageCreateParams params = MessageCreateParams.builder()
            .model(model)
            .maxTokens(MAX_TOKENS)
            .messages(messages)
            .thinking(ThinkingConfigParam.ofEnabled(
                ThinkingConfigEnabled.builder()
                    .budgetTokens(THINKING_BUDGET)
                    .build()))
            .build();

        Message response = client.messages().create(params);

        System.out.println("=== USER MESSAGE ===");
        System.out.println(USER_MESSAGE);

        for (ContentBlock block : response.content()) {
            block.thinking().ifPresent(thinking -> {
                System.out.println("=== THINKING ===");
                System.out.println(thinking.thinking());
                System.out.println("[signature] " + thinking.signature());
            });
            block.text().ifPresent(text -> {
                System.out.println("=== RÉPONSE ===");
                System.out.println(text.text());
            });
        }
    }
}