package com.formation.claudeapi.prompt.evaluation.grader;

import com.formation.claudeapi.prompt.evaluation.pipeline.TestCase;

import java.util.List;

/**
 * Évaluation produite par {@link ModelGrader} (LLM-as-judge) :
 * Représente une réponse JSON structurée de Claude après lecture du {@link TestCase} et de la sortie à évaluer.
 * @param strengths  1 à 3 points forts identifiés
 * @param weaknesses 1 à 3 points faibles identifiés
 * @param reasoning  explication concise de l'évaluation globale
 * @param score      note entre 1 et 10
 */
public record ModelGrade(List<String> strengths, List<String> weaknesses, String reasoning, double score) {
}
