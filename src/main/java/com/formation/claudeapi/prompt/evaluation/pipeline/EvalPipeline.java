package com.formation.claudeapi.prompt.evaluation.pipeline;

import com.anthropic.models.messages.*;
import com.formation.claudeapi.AbstractClaudeConversation;
import com.formation.claudeapi.prompt.evaluation.grader.ModelGrade;
import com.formation.claudeapi.prompt.evaluation.grader.ModelGrader;
import com.formation.claudeapi.prompt.evaluation.grader.SyntaxGrader;

import java.util.ArrayList;
import java.util.List;

/**
 * Pipeline central d'évaluation.
 * <p>
 * Workflow à trois étapes :
 * <ul>
 *   <li>{@link #runPrompt} </li>
 *   <li>{@link #runTestCase} </li>
 *   <li>{@link #runEval} </li>
 * </ul>
 * La notation combine deux graders indépendants, moyennés dans {@link #runTestCase} :
 * <ul>
 *   <li>{@link SyntaxGrader} — validateur structurel rapide (JSON / méthode Java / regex)</li>
 *   <li>{@link ModelGrader} — LLM-as-judge, qui juge le fond via {@link TestCase#solutionCriteria()}</li>
 * </ul>
 */
public class EvalPipeline extends AbstractClaudeConversation {

    private final ModelGrader modelGrader = new ModelGrader();

    public EvalPipeline() {
        super();
        model = Model.CLAUDE_HAIKU_4_5;
    }

    /**
     * Fusionne le prompt et l'entrée du cas de test, puis retourne le résultat.
     * Validé par {@link SyntaxGrader}.
     */
    public String runPrompt(TestCase testCase) {
        List<MessageParam> messages = new ArrayList<>();
        String prompt = """
                Please solve the following task:

                %s

                * Respond only with Java, JSON, or a plain Regex
                * Do not add any comments or commentary or explanation
                """.formatted(testCase.task());

        addUserMessage(messages, prompt);
        addAssistantMessage(messages, "```code");

        return chat(messages, null, List.of("```"), null);
    }

    /**
     * Appelle runPrompt, puis note le résultat en faisant la moyenne {@link SyntaxGrader}
     * (forme) et {@link ModelGrader} (fond).
     */
    public EvalResult runTestCase(TestCase testCase) {
        String output = runPrompt(testCase);

        ModelGrade modelGrade = modelGrader.gradeByModel(testCase, output);
        int syntaxScore = SyntaxGrader.gradeSyntax(output, testCase);

        double score = (modelGrade.score() + syntaxScore) / 2.0;

        return new EvalResult(output, testCase, syntaxScore, modelGrade, score);
    }

    /**
     * Charge le dataset et appelle runTestCase pour chaque cas.
     */
    public List<EvalResult> runEval(List<TestCase> dataset) {
        List<EvalResult> results = new ArrayList<>();

        for (int i = 0; i < dataset.size(); i++) {
            TestCase testCase = dataset.get(i);
            System.out.printf("Évaluation %d/%d (%s)...%n", i + 1, dataset.size(), testCase.format());
            results.add(runTestCase(testCase));
        }

        return results;
    }
}
