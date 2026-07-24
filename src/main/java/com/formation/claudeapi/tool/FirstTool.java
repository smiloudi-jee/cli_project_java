package com.formation.claudeapi.tool;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formation.claudeapi.AbstractClaudeConversation;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Premier tool : Donner l'heure exacte à Claude.
 * Un des trois problèmes identifiés dans "Project overview".
 * <p>
 * "Tool functions" & "Tool schemas" : {@code function_name} / {@code FUNCTION_NAME_SCHEMA}.
 * <p>
 */
public class FirstTool extends AbstractClaudeConversation {

    public static final String DEFAULT_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * Tool : Exécutée quand Claude a besoin de connaître la date/heure actuelle avec précision.
     * Lève une erreur explicite en cas de format invalide.
     * Claude peut lire cette erreur et retenter avec un format corrigé.
     */
    public static String getCurrentDateTime(String dateFormat) {
        if (dateFormat == null || dateFormat.isBlank()) {
            throw new IllegalArgumentException("dateFormat cannot be empty");
        }

        try {
            return LocalDateTime.now().format(DateTimeFormatter.ofPattern(dateFormat));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid dateFormat pattern: " + dateFormat, e);
        }
    }

    /**
     * Schéma JSON du tool :
     * Ce que Claude lit pour savoir quand et comment l'appeler
     * (name / description / input_schema, comme décrit dans "Tool schemas").
     */
    public static final Tool GET_CURRENT_DATETIME_SCHEMA = Tool.builder()
            .name("get_current_datetime")
            .description("""
                    Returns the current date and time formatted according to the specified format. \
                    Use this whenever you need to know the exact current date or time to answer the \
                    user's request, for example to compute a future date/time for a reminder. Returns \
                    a single string containing the formatted date and time.""")
            .inputSchema(Tool.InputSchema.builder()
                    .properties(Tool.InputSchema.Properties.builder()
                            .putAdditionalProperty("date_format", JsonValue.from(Map.of(
                                    "type", "string",
                                    "description", "A Java DateTimeFormatter pattern (e.g. \"yyyy-MM-dd HH:mm:ss\", \"HH:mm\") describing how to format the returned datetime.",
                                    "default", DEFAULT_FORMAT
                            )))
                            .build())
                    .required(List.of())
                    .build())
            .build();

    public static void main(String[] args) throws Exception {
        // 0) Without call to Claude API
        System.out.println("Format par défaut : " + getCurrentDateTime(DEFAULT_FORMAT));
        System.out.println("Heure:minute seulement : " + getCurrentDateTime("HH:mm"));

        System.out.println();
        System.out.println("Schéma envoyé à Claude :");
        System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter()
                .writeValueAsString(GET_CURRENT_DATETIME_SCHEMA));

        // 1) Send user message with tool schema to Claude
        System.out.println();
        System.out.println("Claude choisit-il bien d'appeler ce tool ?");
        List<MessageParam> messages = new ArrayList<>();
        addUserMessage(messages, "What time is it right now ? I need the exact current time.");
        Message response = chatWithTool(messages, List.of(GET_CURRENT_DATETIME_SCHEMA));

        // 2) Receive assistant message with text block and tool use block
        List<ToolUseBlock> toolCalls = response.content().stream()
            .flatMap(block -> block.toolUse().stream())
            .toList();

        // 3) Extract tool information and execute the actual function
        List<ContentBlockParam> toolResults = new ArrayList<>();
        if (toolCalls.isEmpty()) {
            // Pas de tool_use : la réponse de Claude est purement textuelle.
            System.out.println("Claude n'a pas appelé le tool. Réponse :");
            response.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(TextBlock::text)
                    .forEach(System.out::println);
            return;
        }
        for (ToolUseBlock call : toolCalls) {
            System.out.println("Claude demande à appeler : " + call.name());
            Map<String, Object> input = call._input().convert(Map.class);
            System.out.println("Avec les arguments : " + input);

            String dateFormat = (String) input.getOrDefault("date_format", DEFAULT_FORMAT);

            String result;
            boolean isError = false;
            try {
                result = getCurrentDateTime(dateFormat);
            } catch (RuntimeException e) {
                result = e.getMessage();
                isError = true;
            }
            System.out.println("Résultat du tool : " + result);

            toolResults.add(ContentBlockParam.ofToolResult(ToolResultBlockParam.builder()
                .toolUseId(call.id())
                .content(ToolResultBlockParam.Content.ofString(result))
                .isError(isError)
                .build()));
        }

        // 4) Add Tool Result and call Claude again
        addAssistantMessage(messages, response);
        addUserMessage(messages, toolResults);
        Message finalResponse = chatWithTool(messages, List.of(GET_CURRENT_DATETIME_SCHEMA));

        // 5) Receive final response from Claude
        System.out.println();
        System.out.println("Réponse finale de Claude :");
        finalResponse.content().stream()
                .flatMap(block -> block.text().stream())
                .map(TextBlock::text)
                .forEach(System.out::println);
    }
}
