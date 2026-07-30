package com.formation.claudeapi.features.caching;

import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.Tool;
import com.anthropic.models.messages.ToolUnion;
import com.formation.claudeapi.AbstractClaudeConversation;
import com.formation.claudeapi.tool.multi.tools.AddDurationToDateTimeTool;
import com.formation.claudeapi.tool.multi.tools.GetCurrentDateTimeTool;
import com.formation.claudeapi.tool.multi.tools.SetReminderTool;

import java.util.ArrayList;
import java.util.List;

/**
 * Demo "Prompt caching" - mise en cache des schemas de tools.
 * <p>
 * Les schemas de tools (description + input_schema) peuvent peser plusieurs
 * milliers de tokens des qu'on en declare plusieurs - ici les 4 schemas du
 * cours totalisent environ 1.7k tokens. cache_control ne se pose pas sur
 * chaque tool individuellement : on le place uniquement sur le DERNIER tool
 * de la liste, ce qui met en cache l'integralite des tools qui le precedent.
 * <p>
 * On reutilise ici 3 tools deja definis ailleurs dans le projet
 * (get_current_datetime, add_duration_to_datetime, set_reminder) plus
 * {@link DbQueryTool}, pour retrouver les 4 schemas du cours.
 */
public class CachedToolsDemo extends AbstractClaudeConversation {

    private static final List<Tool> TOOLS = buildToolsWithCacheOnLast();

    public static void main(String[] args) {
        Message first = askWithCachedTools("What's 1+1?");
        CacheUsagePrinter.print("1er appel  (ecriture du cache)", first);

        Message second = askWithCachedTools("What's 2+2?");
        CacheUsagePrinter.print("2e appel   (lecture du cache)", second);
    }

    private static List<Tool> buildToolsWithCacheOnLast() {
        List<Tool> tools = new ArrayList<>(List.of(
                GetCurrentDateTimeTool.GET_CURRENT_DATETIME_SCHEMA,
                AddDurationToDateTimeTool.ADD_DURATION_TO_DATETIME_SCHEMA,
                SetReminderTool.SET_REMINDER_SCHEMA,
                DbQueryTool.DB_QUERY_SCHEMA
        ));

        int lastIndex = tools.size() - 1;
        tools.set(lastIndex, CacheControls.cached(tools.get(lastIndex)));
        return tools;
    }

    private static Message askWithCachedTools(String question) {
        List<MessageParam> messages = new ArrayList<>();
        addUserMessage(messages, question);

        MessageCreateParams params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(1024L)
                .tools(TOOLS.stream().map(ToolUnion::ofTool).toList())
                .messages(messages)
                .build();

        return buildClient().messages().create(params);
    }
}
