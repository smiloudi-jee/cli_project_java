package com.formation.claudeapi.prompt.system.eval.grader;

import com.anthropic.models.messages.MessageParam;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formation.claudeapi.AbstractClaudeConversation;
import com.formation.claudeapi.prompt.system.eval.pipeline.TestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * Grader "LLM-as-judge" : demande à Claude d'évaluer le fond de la sortie par
 * rapport aux critères de réussite du cas de test ({@link TestCase#solutionCriteria()}) —
 * là où {@link SyntaxGrader} ne valide que la forme.
 */
public class ModelGrader extends AbstractClaudeConversation {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public ModelGrade gradeByModel(TestCase testCase, String output) {
        String evalPrompt = """
                You are an expert AWS code reviewer. Your task is to evaluate the following AI-generated solution.

                Original Task:
                <task>
                %s
                </task>

                Solution to Evaluate:
                <solution>
                %s
                </solution>

                Criteria you should use to evaluate the solution:
                <criteria>
                %s
                </criteria>

                Output Format
                Provide your evaluation as a structured JSON object with the following fields, in this specific order:
                - "strengths": An array of 1-3 key strengths
                - "weaknesses": An array of 1-3 key areas for improvement
                - "reasoning": A concise explanation of your overall assessment
                - "score": A number between 1-10

                Respond with JSON. Keep your response concise and direct.
                Example response shape:
                {
                    "strengths": string[],
                    "weaknesses": string[],
                    "reasoning": string,
                    "score": number
                }
                """.formatted(testCase.task(), output, testCase.solutionCriteria());

        List<MessageParam> messages = new ArrayList<>();
        addUserMessage(messages, evalPrompt);
        addAssistantMessage(messages, "```json");

        String evalText = chat(messages, null, List.of("```"));

        try {
            return MAPPER.readValue(evalText, ModelGrade.class);
        } catch (Exception e) {
            throw new RuntimeException("Réponse d'évaluation du modèle invalide : " + evalText, e);
        }
    }
}
