package com.formation.claudeapi.prompt.evaluation.pipeline;

import com.formation.claudeapi.prompt.evaluation.grader.ModelGrade;
import com.formation.claudeapi.prompt.evaluation.grader.ModelGrader;
import com.formation.claudeapi.prompt.evaluation.grader.SyntaxGrader;

/**
 * Résultat de l'évaluation d'un cas de test.
 * @param output      la réponse complète de Claude
 * @param testCase    le cas de test original qui a été traité
 * @param syntaxScore score structurel donné par {@link SyntaxGrader} (0 ou 10)
 * @param modelGrade  verdict complet du LLM-as-judge ({@link ModelGrader}) — score, raisonnement,
 *                    forces et faiblesses
 * @param score       moyenne de {@code syntaxScore} et de {@code modelGrade.score()}
 */
public record EvalResult(String output, TestCase testCase, int syntaxScore, ModelGrade modelGrade, double score) {
}
