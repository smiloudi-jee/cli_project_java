package com.formation.claudeapi.prompt.system.eval.generator;

import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.Model;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formation.claudeapi.AbstractClaudeConversation;
import com.formation.claudeapi.prompt.system.eval.pipeline.TestCase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Génère un dataset d'évaluation via Claude et l'écrit dans src/main/resources/Dataset.json.
 */
public class DatasetGenerator extends AbstractClaudeConversation {

    private static final Path DATASET_PATH = Path.of("src/main/resources/Dataset.json");
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public DatasetGenerator() {
        super();
        model = Model.CLAUDE_HAIKU_4_5;
    }

    public List<TestCase> generateDataset() {
        String prompt = """
                Generate an evaluation dataset for a prompt evaluation library. The dataset will be used to evaluate prompts
                that generate Java, JSON, or Regex specifically for AWS-related tasks. Generate an array of JSON objects,
                each representing a task that requires Java, JSON, or a Regex to complete.

                Example output:
                ```json
                [
                    {
                        "task": "Description of task",
                        "format": "json" or "java" or "regex",
                        "solution_criteria": "Key criteria for evaluating the solution"
                    },
                    ...additional
                ]
                ```

                * Focus on tasks that can be solved by writing a single Java method, a single JSON object, or a regular expression.
                * Focus on tasks that do not require writing much code

                Please generate 3 objects.
                """;

        List<MessageParam> messages = new ArrayList<>();
        addUserMessage(messages, prompt);
        addAssistantMessage(messages, "```json");

        String text = chat(messages, null, List.of("```"));

        try {
            return MAPPER.readValue(text, new TypeReference<List<TestCase>>() {});
        } catch (IOException e) {
            throw new RuntimeException("Dataset généré invalide : " + text, e);
        }
    }

    public static void main(String[] args) throws IOException {
        DatasetGenerator generator = new DatasetGenerator();
        List<TestCase> dataset = generator.generateDataset();

        Files.writeString(DATASET_PATH, MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(dataset));

        System.out.println("Dataset écrit dans " + DATASET_PATH.toAbsolutePath());
    }
}
