package com.formation.claudeapi.agents.workflows.routing;

import com.anthropic.models.messages.MessageParam;
import com.formation.claudeapi.AbstractClaudeConversation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Section de cours "Routing workflows".
 * <p>
 * Le routage se fait en deux étapes :
 * <ul>
 *     <li>Étape 1 : Catégoriser le sujet fourni par l'utilisateur parmi les catégories prédéfinies,</li>
 *     <li>Étape 2 : Sélection du prompt spécialisé utilisé pour générer la réponse finale.</li>
 * </ul>
 * Le sujet ne passe jamais que par UN seul pipeline spécialisé,
 * contrairement à la parallélisation, qui interroge tous les pipelines.
 */
public class RoutingDemo extends AbstractClaudeConversation {

    public static void main(String[] args) {
        route("Python functions");
        route("surfing");
    }

    private static void route(String topic) {
        System.out.println("=== Sujet : " + topic + " ===");

        ContentCategory category = categorize(topic);
        System.out.println("Catégorie détectée : " + category.label());

        String script = generateScript(topic, category);
        System.out.println("Script :\n" + script);
        System.out.println();
    }

    /** Étape 1 (routeur) : classification parmi les catégories prédéfinies. */
    private static ContentCategory categorize(String topic) {
        String categoriesList = Arrays.stream(ContentCategory.values())
                .map(ContentCategory::label)
                .collect(Collectors.joining("\n- ", "- ", ""));

        String prompt = """
                Categorize the topic of a video into one of the listed categories:

                <topic>%s</topic>

                <categories>
                %s
                </categories>

                Réponds uniquement avec le nom exact de la catégorie, sans rien ajouter d'autre.
                """.formatted(topic, categoriesList);

        List<MessageParam> messages = new ArrayList<>();
        addUserMessage(messages, prompt);
        String response = chat(messages, null, null, null, null);
        return ContentCategory.fromLabel(response);
    }

    /** Étape 2 (pipeline spécialisé) : utilise le template propre à la catégorie détectée. */
    private static String generateScript(String topic, ContentCategory category) {
        String prompt = """
                Écris un script de vidéo courte (environ 100 mots) sur le sujet "%s".

                Style attendu pour la catégorie "%s" : %s
                """.formatted(topic, category.label(), category.promptGuidance());

        List<MessageParam> messages = new ArrayList<>();
        addUserMessage(messages, prompt);
        return chat(messages, null, null, null, null);
    }
}
