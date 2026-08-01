package com.formation.claudeapi.agents.workflows.agent;

import com.anthropic.models.messages.MessageParam;
import com.formation.claudeapi.AbstractClaudeConversation;
import com.formation.claudeapi.tool.MultiToolConversation;

import java.util.ArrayList;
import java.util.List;

/**
 * Sections de cours "Agents and tools" & "Workflows vs agents". Réutilise volontairement les 3 tools
 * "datetime" déjà écrits pour le chapitre Tool use, {@code com.formation.claudeapi.tool.multi.tools} :
 * <ul>
 *   <li>{@code get_current_datetime}</li>
 *   <li>{@code add_duration_to_datetime}</li>
 *   <li>{@code set_reminder}</li>
 * </ul>
 * Ainsi que la boucle {@link MultiToolConversation#runConversation}.
 */
public class AgentToolsDemo extends AbstractClaudeConversation {

    public static void main(String[] args) {
        // 1 tool utilisé : get_current_datetime
        ask("Quelle heure est-il ?");

        // 2 tools enchaînés : get_current_datetime -> add_duration_to_datetime
        ask("Quel jour de la semaine est-ce dans 11 jours?");

        // 3 tools enchaînés : get_current_datetime -> add_duration_to_datetime -> set_reminder
        ask("Programmez un rappel pour la salle de sport mercredi prochain.");

        // Claude détecte qu'il lui manque une information et la demande avant d'agir
        ask("Quand ma garantie de 90 jours expire-t-elle ? J'ai acheté l'article le 3 mars 2026.");
    }

    private static void ask(String userPrompt) {
        System.out.println("=== " + userPrompt + " ===");
        List<MessageParam> messages = new ArrayList<>();
        addUserMessage(messages, userPrompt);
        MultiToolConversation.runConversation(messages);
        System.out.println();
    }
}
