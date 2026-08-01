package com.formation.claudeapi.agents.workflows.evaluatoroptimizer;

import com.anthropic.models.messages.MessageParam;
import com.formation.claudeapi.AbstractClaudeConversation;

import java.util.ArrayList;
import java.util.List;

/**
 * Section de cours "The Evaluator-Optimizer Pattern"
 * <p>
 * Généralisation du workflow (produire → rendu → notation → correction) du cours :
 * un "producteur" rédige une accroche produit, un "évaluateur" la note par rapport à des
 * critères précis, et la boucle continue tant que l'évaluateur n'accepte pas (ou jusqu'à
 * un nombre maximum d'itérations).
 */
public class EvaluatorOptimizerDemo extends AbstractClaudeConversation {

    private static final int MAX_ITERATIONS = 3;

    private static final String BRIEF =
            "Une accroche marketing pour une gourde réutilisable en inox, isolée 24h.";

    private static final String CRITERIA = """
            - 12 mots maximum
            - Mentionne explicitement le bénéfice "isolation 24h"
            - Aucun jargon marketing générique ("révolutionnaire", "incontournable", etc.)
            - Ton dynamique, sans point d'exclamation""";

    public static void main(String[] args) {
        String feedback = null;

        for (int iteration = 1; iteration <= MAX_ITERATIONS; iteration++) {
            System.out.println("=== Itération " + iteration + " (producteur) ===");
            String candidate = produce(feedback);
            System.out.println(candidate);

            System.out.println("=== Itération " + iteration + " (évaluateur) ===");
            GradeResult grade = grade(candidate);
            System.out.println(grade);

            if (grade.accepted()) {
                System.out.println("\nAccroche retenue : " + candidate);
                return;
            }
            feedback = grade.feedback();
        }

        System.out.println("\nNombre maximum d'itérations atteint sans accord de l'évaluateur.");
    }

    /** Producteur : rédige (ou corrige, si un feedback précédent existe) l'accroche. */
    private static String produce(String previousFeedback) {
        String prompt = previousFeedback == null
                ? "Rédige : " + BRIEF
                : """
                  Rédige : %s

                  La proposition précédente a été rejetée pour la raison suivante, corrige-la en \
                  conséquence :
                  %s
                  """.formatted(BRIEF, previousFeedback);

        List<MessageParam> messages = new ArrayList<>();
        addUserMessage(messages, prompt);
        return chat(messages, null, null, null, null).trim();
    }

    /** Évaluateur : accepte ou rejette la proposition par rapport aux critères, avec justification. */
    private static GradeResult grade(String candidate) {
        String prompt = """
                Évalue la proposition suivante par rapport à ces critères stricts :
                %s

                Proposition : "%s"

                Si tous les critères sont respectés, réponds exactement par : ACCEPT
                Sinon, réponds par : REJECT: <raison précise et actionnable>
                """.formatted(CRITERIA, candidate);

        List<MessageParam> messages = new ArrayList<>();
        addUserMessage(messages, prompt);
        String response = chat(messages, null, null, null, null).trim();

        if (response.equalsIgnoreCase("ACCEPT")) {
            return new GradeResult(true, null);
        }
        String reason = response.startsWith("REJECT:") ? response.substring("REJECT:".length()).trim() : response;
        return new GradeResult(false, reason);
    }

    private record GradeResult(boolean accepted, String feedback) {
        @Override
        public String toString() {
            return accepted ? "ACCEPT" : "REJECT: " + feedback;
        }
    }
}
