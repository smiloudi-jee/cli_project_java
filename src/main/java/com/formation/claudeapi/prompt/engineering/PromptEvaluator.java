package com.formation.claudeapi.prompt.engineering;

import com.anthropic.models.messages.MessageParam;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formation.claudeapi.AbstractClaudeConversation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pipeline générique et réutilisable d'évaluation de prompts.
 * <p>
 * Trois Étapes :
 * <ul>
 *   <li>{@link #generateDataset} : Générer un dataset de cas de test à partir de la description de tâche</li>
 *   <li>{@link #runEvaluation} : Exécute le prompt à tester sur chaque cas puis fait noter chaque sortie par un
 *       modèle-juge ({@link #gradeOutput})</li>
 *   <li>Écrit un rapport JSON et un rapport HTML ({@link PromptEvaluationReport})</li>
 * </ul>
 */
public class PromptEvaluator extends AbstractClaudeConversation {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^{}]+)}");

    private final int maxConcurrentTasks;

    public PromptEvaluator() {
        this(3);
    }

    /** @param maxConcurrentTasks commence bas (2-3) pour éviter les erreurs de rate limit. */
    public PromptEvaluator(int maxConcurrentTasks) {
        this.maxConcurrentTasks = maxConcurrentTasks;
    }

    /**
     * Mini-moteur de templating maison : remplace chaque {@code {placeholder}} présent dans {@code variables}.
     */
    private static String render(String templateString, Map<String, Object> variables) {
        List<String> placeholders = new ArrayList<>();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(templateString);
        while (matcher.find()) {
            placeholders.add(matcher.group(1));
        }

        String result = templateString;
        for (String placeholder : placeholders) {
            if (variables.containsKey(placeholder)) {
                result = result.replace("{" + placeholder + "}", String.valueOf(variables.get(placeholder)));
            }
        }

        return result.replace("{{", "{").replace("}}", "}");
    }

    /**
     * Demande à Claude {@code numCases} idées de scénarios distincts pour tester le prompt décrit
     * par {@code taskDescription}.
     */
    public List<String> generateUniqueIdeas(String taskDescription, Map<String, String> promptInputsSpec, int numCases) {
        String promptTemplate = """
                Generate {num_cases} unique, diverse ideas for testing a prompt that accomplishes this task:

                <task_description>
                {task_description}
                </task_description>

                The prompt will receive the following inputs
                <prompt_inputs>
                {prompt_inputs_spec}
                </prompt_inputs>

                Each idea should represent a distinct scenario or example that tests different aspects of the task.

                Output Format:
                Provide your response as a structured JSON array where each item is a brief description of the idea.

                Example:
                ```json
                [
                    "Testing with technical computer science terminology",
                    "Testing with medical research findings",
                    "Testing with complex mathematical concepts",
                    ...
                ]
                ```

                Ensure each idea is:
                - Clearly distinct from the others
                - Relevant to the task description
                - Specific enough to guide generation of a full test case
                - Quick to solve without requiring extensive computation or multi-step processing
                - Solvable with no more than 400 tokens of output

                Remember, only generate {num_cases} unique ideas
                """;

        String systemPrompt = "You are a test scenario designer specialized in creating diverse, unique testing scenarios.";

        StringBuilder examplePromptInputs = new StringBuilder();
        for (Map.Entry<String, String> entry : promptInputsSpec.entrySet()) {
            String val = entry.getValue().replace("\n", "\\n");
            examplePromptInputs.append('"').append(entry.getKey()).append("\": str # ").append(val).append(',');
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("task_description", taskDescription);
        variables.put("num_cases", numCases);
        variables.put("prompt_inputs_spec", examplePromptInputs.toString());

        String renderedPrompt = render(promptTemplate, variables);

        List<MessageParam> messages = new ArrayList<>();
        addUserMessage(messages, renderedPrompt);
        addAssistantMessage(messages, "```json");

        String text = chat(messages, systemPrompt, List.of("```"), 1.0);

        try {
            return MAPPER.readValue(text, new TypeReference<List<String>>() {});
        } catch (IOException e) {
            throw new RuntimeException("Réponse invalide pour la génération d'idées : " + text, e);
        }
    }

    /**
     * Génère un {@link TestCase} détaillé (entrées + critères de réussite) à partir d'une idée
     * de scénario produite par {@link #generateUniqueIdeas}.
     */
    public TestCase generateTestCase(String taskDescription, String idea, Map<String, String> promptInputsSpec) {
        StringBuilder examplePromptInputs = new StringBuilder();
        for (Map.Entry<String, String> entry : promptInputsSpec.entrySet()) {
            String val = entry.getValue().replace("\n", "\\n");
            examplePromptInputs.append('"').append(entry.getKey()).append("\": \"EXAMPLE_VALUE\", // ").append(val).append('\n');
        }

        String allowedKeys = String.join(", ", promptInputsSpec.keySet().stream().map(k -> "\"" + k + "\"").toList());

        String promptTemplate = """
                Generate a single detailed test case for a prompt evaluation based on:

                <task_description>
                {task_description}
                </task_description>

                <specific_idea>
                {idea}
                </specific_idea>

                <allowed_input_keys>
                {allowed_keys}
                </allowed_input_keys>

                Output Format:
                ```json
                {{
                    "prompt_inputs": {{
                    {example_prompt_inputs}
                    }},
                    "solution_criteria": ["criterion 1", "criterion 2", ...] // Concise list of criteria for evaluating the solution, 1 to 4 items
                }}
                ```

                IMPORTANT REQUIREMENTS:
                - You MUST ONLY use these exact input keys in your prompt_inputs: {allowed_keys}
                - Do NOT add any additional keys to prompt_inputs
                - All keys listed in allowed_input_keys must be included in your response
                - Make the test case realistic and practically useful
                - Include measurable, concise solution criteria
                - The solution criteria should ONLY address the direct requirements of the task description and the generated prompt_inputs
                - Avoid over-specifying criteria with requirements that go beyond the core task
                - Keep solution criteria simple, focused, and directly tied to the fundamental task
                - The test case should be tailored to the specific idea provided
                - Quick to solve without requiring extensive computation or multi-step processing
                - Solvable with no more than 400 tokens of output
                - DO NOT include any fields beyond those specified in the output format

                Here's an example of a sample input with an ideal output:
                <sample_input>
                <sample_task_description>
                Extract topics out of a passage of text
                </sample_task_description>
                <sample_specific_idea>
                Testing with a text that contains multiple nested topics and subtopics (e.g., a passage about renewable energy that covers solar power economics, wind turbine technology, and policy implications simultaneously)
                </sample_specific_idea>

                <sample_allowed_input_keys>
                "content"
                </sample_allowed_input_keys>
                </sample_input>
                <ideal_output>
                ```json
                {
                    "prompt_inputs": {
                        "content": "The transition to renewable energy encompasses numerous interdependent dimensions. Solar photovoltaic technology has seen dramatic cost reductions, with panel efficiency improving 24% since 2010 while manufacturing costs declined by 89%, making it economically competitive with fossil fuels in many markets. Concurrently, wind energy has evolved through innovative turbine designs featuring carbon-fiber composite blades and advanced control systems that increase energy capture by 35% in low-wind conditions."
                    },
                    "solution_criteria": [
                        "Includes all topics mentioned"
                    ]
                }
                ```
                </ideal_output>
                This is ideal output because the solution criteria is concise and doesn't ask for anything outside of the scope of the task description.
                """;

        String systemPrompt = "You are a test case creator specializing in designing evaluation scenarios.";

        Map<String, Object> variables = new HashMap<>();
        variables.put("allowed_keys", allowedKeys);
        variables.put("task_description", taskDescription);
        variables.put("idea", idea);
        variables.put("example_prompt_inputs", examplePromptInputs.toString());

        String renderedPrompt = render(promptTemplate, variables);

        List<MessageParam> messages = new ArrayList<>();
        addUserMessage(messages, renderedPrompt);
        addAssistantMessage(messages, "```json");

        String text = chat(messages, systemPrompt, List.of("```"), 0.7);

        try {
            TestCase parsed = MAPPER.readValue(text, TestCase.class);
            return new TestCase(parsed.promptInputs(), parsed.solutionCriteria(), taskDescription, idea);
        } catch (IOException e) {
            throw new RuntimeException("Réponse invalide pour la génération d'un cas de test : " + text, e);
        }
    }

    /**
     * Génère un dataset complet : {@code numCases} idées, puis un {@link TestCase} détaillé par
     * idée (en parallèle, borné par {@code maxConcurrentTasks}), écrit dans {@code outputFile}.
     */
    public List<TestCase> generateDataset(String taskDescription, Map<String, String> promptInputsSpec, int numCases, String outputFile) {
        List<String> ideas = generateUniqueIdeas(taskDescription, promptInputsSpec, numCases);

        List<TestCase> dataset = new ArrayList<>();
        int total = ideas.size();
        AtomicInteger completed = new AtomicInteger(0);
        AtomicInteger lastReportedPercentage = new AtomicInteger(0);

        try (ExecutorService executor = Executors.newFixedThreadPool(maxConcurrentTasks)) {
            ExecutorCompletionService<TestCase> completionService = new ExecutorCompletionService<>(executor);
            for (String idea : ideas) {
                completionService.submit(() -> generateTestCase(taskDescription, idea, promptInputsSpec));
            }

            for (int i = 0; i < total; i++) {
                try {
                    Future<TestCase> future = completionService.take();
                    dataset.add(future.get());
                    reportProgress("Generated", completed.incrementAndGet(), total, lastReportedPercentage);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                } catch (ExecutionException e) {
                    System.out.println("Error generating test case: " + e.getCause());
                }
            }
        }

        try {
            Files.writeString(Path.of(outputFile), MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(dataset));
        } catch (IOException e) {
            throw new RuntimeException("Impossible d'écrire le dataset dans " + outputFile, e);
        }

        return dataset;
    }

    /**
     * Fait juger {@code output} par un modèle, avec une rigueur volontairement extrême, sur la
     * base des {@code solutionCriteria} du cas de test et d'éventuels {@code extraCriteria}
     * (obligatoires — toute violation entraîne un score ≤ 3).
     */
    public ModelGrade gradeOutput(TestCase testCase, String output, String extraCriteria) {
        StringBuilder promptInputs = new StringBuilder();
        for (Map.Entry<String, String> entry : testCase.promptInputs().entrySet()) {
            String val = entry.getValue().replace("\n", "\\n");
            promptInputs.append('"').append(entry.getKey()).append("\":\"").append(val).append("\",\n");
        }

        String extraCriteriaSection = "";
        if (extraCriteria != null && !extraCriteria.isBlank()) {
            String extraCriteriaTemplate = """
                    Mandatory Requirements - ANY VIOLATION MEANS AUTOMATIC FAILURE (score of 3 or lower):
                    <extra_important_criteria>
                    {extra_criteria}
                    </extra_important_criteria>
                    """;
            extraCriteriaSection = render(extraCriteriaTemplate, Map.of("extra_criteria", extraCriteria));
        }

        String evalTemplate = """
                Your task is to evaluate the following AI-generated solution with EXTREME RIGOR.

                Original task description:
                <task_description>
                {task_description}
                </task_description>

                Original task inputs:
                <task_inputs>
                {{ {prompt_inputs} }}
                </task_inputs>

                Solution to Evaluate:
                <solution>
                {output}
                </solution>

                Criteria you should use to evaluate the solution:
                <criteria>
                {solution_criteria}
                </criteria>

                {extra_criteria_section}

                Scoring Guidelines:
                * Score 1-3: Solution fails to meet one or more MANDATORY requirements
                * Score 4-6: Solution meets all mandatory requirements but has significant deficiencies in secondary criteria
                * Score 7-8: Solution meets all mandatory requirements and most secondary criteria, with minor issues
                * Score 9-10: Solution meets all mandatory and secondary criteria

                IMPORTANT SCORING INSTRUCTIONS:
                * Grade the output based ONLY on the listed criteria. Do not add your own extra requirements.
                * If a solution meets all of the mandatory and secondary criteria give it a 10
                * Don't complain that the solution "only" meets the mandatory and secondary criteria. Solutions shouldn't go above and beyond - they should meet the exact listed criteria.
                * ANY violation of a mandatory requirement MUST result in a score of 3 or lower
                * The full 1-10 scale should be utilized - don't hesitate to give low scores when warranted

                Output Format
                Provide your evaluation as a structured JSON object with the following fields, in this specific order:
                - "strengths": An array of 1-3 key strengths
                - "weaknesses": An array of 1-3 key areas for improvement
                - "reasoning": A concise explanation of your overall assessment
                - "score": A number between 1-10

                Respond with JSON. Keep your response concise and direct.
                Example response shape:
                {{
                    "strengths": string[],
                    "weaknesses": string[],
                    "reasoning": string,
                    "score": number
                }}
                """;

        Map<String, Object> variables = new HashMap<>();
        variables.put("task_description", testCase.taskDescription());
        variables.put("prompt_inputs", promptInputs.toString());
        variables.put("output", output);
        variables.put("solution_criteria", String.join("\n", testCase.solutionCriteria()));
        variables.put("extra_criteria_section", extraCriteriaSection);

        String evalPrompt = render(evalTemplate, variables);

        List<MessageParam> messages = new ArrayList<>();
        addUserMessage(messages, evalPrompt);
        addAssistantMessage(messages, "```json");

        String evalText = chat(messages, null, List.of("```"), 0.0);

        try {
            return MAPPER.readValue(evalText, ModelGrade.class);
        } catch (IOException e) {
            throw new RuntimeException("Réponse d'évaluation invalide : " + evalText, e);
        }
    }

    /** Exécute le prompt à tester sur un cas, puis fait noter la sortie. */
    public EvaluationResult runTestCase(TestCase testCase, Function<Map<String, String>, String> runPromptFunction, String extraCriteria) {
        String output = runPromptFunction.apply(testCase.promptInputs());

        ModelGrade modelGrade = gradeOutput(testCase, output, extraCriteria);

        return new EvaluationResult(output, testCase, modelGrade.score(), modelGrade.reasoning());
    }

    /**
     * Charge {@code datasetFile}, exécute {@code runPromptFunction} sur chaque cas (en parallèle,
     * borné par {@code maxConcurrentTasks}), note chaque sortie, puis écrit un rapport JSON et un
     * rapport HTML ({@link PromptEvaluationReport}).
     */
    public List<EvaluationResult> runEvaluation(
            Function<Map<String, String>, String> runPromptFunction,
            String datasetFile,
            String extraCriteria,
            String jsonOutputFile,
            String htmlOutputFile) {

        List<TestCase> dataset;
        try {
            dataset = MAPPER.readValue(Path.of(datasetFile).toFile(), new TypeReference<List<TestCase>>() {});
        } catch (IOException e) {
            throw new RuntimeException("Impossible de lire le dataset " + datasetFile, e);
        }

        List<EvaluationResult> results = new ArrayList<>();
        int total = dataset.size();
        AtomicInteger completed = new AtomicInteger(0);
        AtomicInteger lastReportedPercentage = new AtomicInteger(0);

        try (ExecutorService executor = Executors.newFixedThreadPool(maxConcurrentTasks)) {
            ExecutorCompletionService<EvaluationResult> completionService = new ExecutorCompletionService<>(executor);
            for (TestCase testCase : dataset) {
                completionService.submit(() -> runTestCase(testCase, runPromptFunction, extraCriteria));
            }

            for (int i = 0; i < total; i++) {
                try {
                    results.add(completionService.take().get());
                    reportProgress("Graded", completed.incrementAndGet(), total, lastReportedPercentage);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                } catch (ExecutionException e) {
                    throw new RuntimeException("Erreur lors de la notation d'un cas de test", e.getCause());
                }
            }
        }

        double averageScore = results.stream().mapToDouble(EvaluationResult::score).average().orElse(0.0);
        System.out.println("Average score: " + averageScore);

        try {
            Files.writeString(Path.of(jsonOutputFile), MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(results));
            Files.writeString(Path.of(htmlOutputFile), PromptEvaluationReport.generate(results));
        } catch (IOException e) {
            throw new RuntimeException("Impossible d'écrire les fichiers de résultats", e);
        }

        return results;
    }

    private static void reportProgress(String verb, int completed, int total, AtomicInteger lastReportedPercentage) {
        int currentPercentage = (int) ((completed / (double) total) * 100);
        int milestonePercentage = (currentPercentage / 20) * 20;
        if (milestonePercentage > lastReportedPercentage.get()) {
            System.out.printf("%s %d/%d test cases%n", verb, completed, total);
            lastReportedPercentage.set(milestonePercentage);
        }
    }
}
