package com.formation.claudeapi.prompt.engineering;

/**
 * Résultat de l'évaluation d'un {@link TestCase} par {@link PromptEvaluator#runTestCase}.
 * @param output    la réponse brute produite par le prompt testé
 * @param testCase  le cas de test évalué
 * @param score     note du modèle-juge (1 à 10)
 * @param reasoning raisonnement du modèle-juge
 */
public record EvaluationResult(String output, TestCase testCase, double score, String reasoning) {
}
