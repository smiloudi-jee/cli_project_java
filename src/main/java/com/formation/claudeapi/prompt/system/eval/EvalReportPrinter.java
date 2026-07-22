package com.formation.claudeapi.prompt.system.eval;

import com.formation.claudeapi.prompt.system.eval.grader.ModelGrade;
import com.formation.claudeapi.prompt.system.eval.pipeline.EvalResult;
import com.formation.claudeapi.prompt.system.eval.pipeline.TestCase;

import java.util.List;

/**
 * Affiche les résultats d'une évaluation dans la console, en clair.
 * <p>
 * {@code PromptEvaluation} imprimait jusqu'ici les résultats via un dump JSON
 * pretty-printé : lisible pour la {@link TestCase} (courte), illisible pour le
 * champ {@code output} (code/JSON/regex multi-lignes noyé dans des "\n" et des
 * guillemets échappés). Ici on imprime chaque champ tel quel, avec de vrais
 * retours à la ligne — un bloc par cas de test, plus un récapitulatif global.
 */
public class EvalReportPrinter {

    private static final String SEPARATOR = "=".repeat(80);
    private static final String SUB_SEPARATOR = "-".repeat(80);

    public static void print(List<EvalResult> results) {
        for (int i = 0; i < results.size(); i++) {
            printResult(i + 1, results.size(), results.get(i));
        }
        printSummary(results);
    }

    private static void printResult(int index, int total, EvalResult result) {
        TestCase testCase = result.testCase();
        ModelGrade modelGrade = result.modelGrade();

        System.out.println(SEPARATOR);
        System.out.printf("Cas de test %d/%d — format : %s%n", index, total, testCase.format());
        System.out.println(SUB_SEPARATOR);

        System.out.println("Tâche :");
        System.out.println(testCase.task());
        System.out.println();

        System.out.println("Critères de réussite :");
        System.out.println(testCase.solutionCriteria());
        System.out.println();

        System.out.println("Sortie de Claude :");
        System.out.println(SUB_SEPARATOR);
        System.out.println(result.output());
        System.out.println(SUB_SEPARATOR);

        System.out.printf("Score : %.1f/10  (syntaxe : %d/10, modèle : %.1f/10)%n",
                result.score(), result.syntaxScore(), modelGrade.score());
        System.out.println("Raisonnement du modèle : " + modelGrade.reasoning());

        if (modelGrade.strengths() != null && !modelGrade.strengths().isEmpty()) {
            System.out.println("Points forts : " + String.join(" ; ", modelGrade.strengths()));
        }
        if (modelGrade.weaknesses() != null && !modelGrade.weaknesses().isEmpty()) {
            System.out.println("Points faibles : " + String.join(" ; ", modelGrade.weaknesses()));
        }
        System.out.println();
    }

    private static void printSummary(List<EvalResult> results) {
        double averageScore = results.stream()
                .mapToDouble(EvalResult::score)
                .average()
                .orElse(0.0);

        System.out.println(SEPARATOR);
        System.out.printf("Score moyen sur %d cas : %.2f/10%n", results.size(), averageScore);
        System.out.println(SEPARATOR);
    }
}
