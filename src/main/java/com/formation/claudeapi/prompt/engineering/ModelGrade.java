package com.formation.claudeapi.prompt.engineering;

import java.util.List;

/**
 * Verdict JSON structuré renvoyé par le modèle-juge dans {@link PromptEvaluator#gradeOutput}.
 * @param strengths  1 à 3 points forts
 * @param weaknesses 1 à 3 points faibles
 * @param reasoning  explication concise de l'évaluation
 * @param score      note entre 1 et 10
 */
public record ModelGrade(List<String> strengths, List<String> weaknesses, String reasoning, double score) {
}
