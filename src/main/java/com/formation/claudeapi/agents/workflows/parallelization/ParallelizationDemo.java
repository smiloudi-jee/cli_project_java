package com.formation.claudeapi.agents.workflows.parallelization;

import com.anthropic.models.messages.MessageParam;
import com.formation.claudeapi.AbstractClaudeConversation;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Section de cours "Parallelization workflows".
 * <p>
 * Plutôt qu'un unique prompt géant demandant à Claude de choisir entre plusieurs possibilités
 * en une seule fois (ce qui l'oblige à jongler avec des critères hétérogènes en même temps),
 * on envoie une requête indépendante par possibilité, en parallèle, chacune avec ses propres
 * critères spécialisés, puis on agrège les analyses obtenues dans un dernier appel de synthèse.
 */
public class ParallelizationDemo extends AbstractClaudeConversation {

    private static final String PART_DESCRIPTION = """
            Pièce : support en L de 120 x 80 x 5 mm, destiné à fixer un panneau solaire sur un \
            toit incliné, exposé en extérieur (pluie, UV, écarts de température -10°C à 45°C). \
            Doit supporter une charge statique de 25 kg et résister aux vibrations dues au vent. \
            Production en série de 5000 unités/an, budget serré.""";

    public static void main(String[] args) {
        Map<MaterialCriteria, String> analyses = evaluateAllMaterialsInParallel(PART_DESCRIPTION);

        analyses.forEach((material, analysis) -> {
            System.out.println("=== " + material.displayName() + " ===");
            System.out.println(analysis);
            System.out.println();
        });

        System.out.println("=== Recommandation finale (agrégation) ===");
        System.out.println(aggregate(PART_DESCRIPTION, analyses));
    }

    /** Lance une requête Claude par matériau, en parallèle, et attend que toutes se terminent. */
    private static Map<MaterialCriteria, String> evaluateAllMaterialsInParallel(String partDescription) {
        ExecutorService executor = Executors.newFixedThreadPool(MaterialCriteria.values().length);
        try {
            Map<MaterialCriteria, CompletableFuture<String>> futures = new EnumMap<>(MaterialCriteria.class);
            for (MaterialCriteria material : MaterialCriteria.values()) {
                futures.put(material, CompletableFuture.supplyAsync(
                        () -> evaluateMaterial(partDescription, material), executor));
            }

            Map<MaterialCriteria, String> results = new EnumMap<>(MaterialCriteria.class);
            futures.forEach((material, future) -> results.put(material, future.join()));
            return results;
        } finally {
            executor.shutdown();
        }
    }

    /** Sous-tâche spécialisée : un seul matériau, ses propres critères, indépendante des autres. */
    private static String evaluateMaterial(String partDescription, MaterialCriteria material) {
        String prompt = """
                Évalue si le matériau "%s" convient pour la pièce décrite ci-dessous, en te basant \
                uniquement sur les critères suivants : %s

                Pièce :
                %s

                Réponds en 3-4 phrases maximum : avantages, inconvénients, note de 1 à 10.
                """.formatted(material.displayName(), material.criteria(), partDescription);

        List<MessageParam> messages = new ArrayList<>();
        addUserMessage(messages, prompt);
        return chat(messages, null, null, null, null);
    }

    /** Étape finale : compare les analyses indépendantes et tranche. */
    private static String aggregate(String partDescription, Map<MaterialCriteria, String> analyses) {
        String analysesText = analyses.entrySet().stream()
                .map(entry -> "- " + entry.getKey().displayName() + " : " + entry.getValue())
                .collect(Collectors.joining("\n"));

        String prompt = """
                Voici les analyses indépendantes de plusieurs matériaux candidats pour cette pièce :

                Pièce :
                %s

                Analyses par matériau :
                %s

                Compare ces analyses et recommande le matériau le plus adapté, en justifiant \
                brièvement ton choix.
                """.formatted(partDescription, analysesText);

        List<MessageParam> messages = new ArrayList<>();
        addUserMessage(messages, prompt);
        return chat(messages, null, null, null, null);
    }
}
