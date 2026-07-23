package com.formation.claudeapi.prompt.evaluation.pipeline;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.formation.claudeapi.prompt.evaluation.grader.ModelGrader;
import com.formation.claudeapi.prompt.evaluation.grader.SyntaxGrader;

/**
 * Un cas de test du dataset d'évaluation.
 * <p>
 * @param task             l'énoncé de la tâche à soumettre à Claude
 * @param format           le format de sortie attendu ("json", "java" ou "regex"),
 *                         utilisé par {@link SyntaxGrader} pour choisir la validation à appliquer
 * @param solutionCriteria les critères de réussite attendus pour cette tâche,
 *                         utilisés par {@link ModelGrader} (LLM-as-judge)
 */
public record TestCase(
        String task,
        String format,
        @JsonProperty("solution_criteria") String solutionCriteria) {
}
