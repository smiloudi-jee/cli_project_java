package com.formation.claudeapi.agents.workflows.chaining;

import com.anthropic.models.messages.MessageParam;
import com.formation.claudeapi.AbstractClaudeConversation;

import java.util.ArrayList;
import java.util.List;

/**
 * Section de cours "Chaining workflows".
 * <p>
 * Un unique prompt avec plusieurs contraintes ne garantit pas que Claude respecte toutes
 * les règles du premier coup.
 * <p>
 * Le chaînage consiste à séparer le prompt en plusieurs étapes, ici "rédiger" et "corriger".
 * À chaque appel, Claude se concentre sur une seule tâche à la fois plutôt que de jongler
 * avec plusieurs exigences en même temps.
 */
public class ChainingDemo extends AbstractClaudeConversation {

    private static final String TOPIC = "Les bases du prompt caching avec l'API Claude";

    public static void main(String[] args) {
        System.out.println("=== Étape 1 : premier jet ===");
        String draft = writeArticle(TOPIC);
        System.out.println(draft);

        System.out.println("\n=== Étape 2 : révision ciblée ===");
        String revised = reviseArticle(draft);
        System.out.println(revised);
    }

    /** Étape 1 de la chaîne : rédaction initiale, sans garantie que toutes les contraintes soient respectées. */
    private static String writeArticle(String topic) {
        String prompt = """
                Écris un court article technique (environ 150 mots) sur le sujet suivant : %s

                Contraintes :
                - Ne jamais mentionner que le texte est écrit par une IA
                - Aucun emoji
                - Évite le langage cliché ou trop familier
                - Ton professionnel et technique
                """.formatted(topic);

        List<MessageParam> messages = new ArrayList<>();
        addUserMessage(messages, prompt);
        return chat(messages, null, null, null, null);
    }

    /** Étape 2 de la chaîne : Claude se concentre uniquement sur la correction, pas sur le fond. */
    private static String reviseArticle(String article) {
        String prompt = """
                Révise l'article ci-dessous en suivant ces étapes :
                1. Repère tout passage qui identifie le texte comme écrit par une IA et supprime-le
                2. Trouve et supprime tous les emojis
                3. Repère les formulations "cringe" ou trop familières et remplace-les par un texte \
                digne d'un rédacteur technique

                Article :
                %s
                """.formatted(article);

        List<MessageParam> messages = new ArrayList<>();
        addUserMessage(messages, prompt);
        return chat(messages, null, null, null, null);
    }
}
