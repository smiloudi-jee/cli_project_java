package com.formation.claudeapi.tool;

import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.TextBlock;
import com.anthropic.models.messages.Tool;
import com.anthropic.models.messages.ToolResultBlockParam;
import com.anthropic.models.messages.ToolUseBlock;
import com.formation.claudeapi.AbstractClaudeConversation;
import com.formation.claudeapi.tool.multi.tools.AddDurationToDateTimeTool;
import com.formation.claudeapi.tool.multi.tools.GetCurrentDateTimeTool;
import com.formation.claudeapi.tool.multi.tools.SetReminderTool;
import com.formation.claudeapi.tool.multi.router.ToolRouter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Conversation multi tools.
 */
public class MultiToolConversation extends AbstractClaudeConversation {

    private static final List<Tool> TOOLS = List.of(
            GetCurrentDateTimeTool.GET_CURRENT_DATETIME_SCHEMA,
            AddDurationToDateTimeTool.ADD_DURATION_TO_DATETIME_SCHEMA,
            SetReminderTool.SET_REMINDER_SCHEMA
    );

    /**
     * Boucle tant que Claude demande des tools.
     */
    public static List<MessageParam> runConversation(List<MessageParam> messages) {
        while (true) {
            Message response = chatWithTool(messages, TOOLS);
            addAssistantMessage(messages, response);
            printText(response);

            boolean askedForTool = response.content().stream()
                    .anyMatch(block -> block.toolUse().isPresent());

            if (!askedForTool) {
                break;
            }

            List<ContentBlockParam> toolResults = runTools(response);
            addUserMessage(messages, toolResults);
        }

        return messages;
    }

    /** Exécute chaque tool_use de la réponse issue de l'appel précédent et construit les tool_result correspondants. */
    private static List<ContentBlockParam> runTools(Message message) {
        List<ToolUseBlock> toolRequests = message.content().stream()
                .flatMap(block -> block.toolUse().stream())
                .toList();

        List<ContentBlockParam> toolResults = new ArrayList<>();
        for (ToolUseBlock toolRequest : toolRequests) {
            toolResults.add(runSingleTool(toolRequest));
        }
        return toolResults;
    }

    /**
     * Exécution unitaire des tool_uses avec gestion des erreurs possibles.
     * Claude peut lire l'erreur et ajuster son prochain appel.
     */
    @SuppressWarnings("unchecked")
    private static ContentBlockParam runSingleTool(ToolUseBlock toolRequest) {
        Map<String, Object> input = toolRequest._input().convert(Map.class);

        String content;
        boolean isError;
        try {
            content = ToolRouter.runTool(toolRequest.name(), input);
            isError = false;
        } catch (Exception e) {
            content = "Error: " + e.getMessage();
            isError = true;
        }

        System.out.println((isError ? "[erreur] " : "") + toolRequest.name() + " -> " + content);

        return ContentBlockParam.ofToolResult(ToolResultBlockParam.builder()
                .toolUseId(toolRequest.id())
                .content(ToolResultBlockParam.Content.ofString(content))
                .isError(isError)
                .build());
    }

    private static void printText(Message message) {
        message.content().stream()
                .flatMap(block -> block.text().stream())
                .map(TextBlock::text)
                .forEach(System.out::println);
    }

    public static void main(String[] args) {
        List<MessageParam> messages = new ArrayList<>();

        // 2 tools utilisés : add_duration_to_datetime & set_reminder
        String userPromptDeux = "Set a reminder for my doctors appointment. Its 177 days after Jan 1st, 2050.";

        // 3 tools utilisés : get_current_datetime & add_duration_to_datetime & set_reminder
        String userPromptTrois = "Set a reminder for my doctors appointment. Its 177 days from today.";

        addUserMessage(messages, userPromptTrois);

        runConversation(messages);
    }
}
