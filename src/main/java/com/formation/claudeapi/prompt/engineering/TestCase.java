package com.formation.claudeapi.prompt.engineering;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Un cas de test généré par {@link PromptEvaluator} pour évaluer un prompt.
 *
 * @param promptInputs      les entrées du prompt à évaluer (ex. height, weight, goal...)
 * @param solutionCriteria  1 à 4 critères concis pour juger la solution
 * @param taskDescription   la description de la tâche évaluée, dupliquée dans chaque cas
 *                          pour que {@link PromptEvaluator#gradeOutput} n'ait pas besoin
 *                          d'un contexte externe
 * @param scenario          l'idée/scénario à l'origine de ce cas de test
 */
public record TestCase(
        @JsonProperty("prompt_inputs") Map<String, String> promptInputs,
        @JsonProperty("solution_criteria") List<String> solutionCriteria,
        @JsonProperty("task_description") String taskDescription,
        String scenario) {
}
