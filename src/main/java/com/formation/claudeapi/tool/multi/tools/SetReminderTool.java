package com.formation.claudeapi.tool.multi.tools;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.Tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Troisième tool de l'exercice : Poser le rappel
 */
public class SetReminderTool {

    private static final List<String> REMINDERS = new ArrayList<>();

    public static String setReminder(String content, String timestamp) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content cannot be empty");
        }
        if (timestamp == null || timestamp.isBlank()) {
            throw new IllegalArgumentException("timestamp cannot be empty");
        }

        System.out.println("Setting the following reminder for " + timestamp + ":");
        System.out.println(content);

        REMINDERS.add(content + " @ " + timestamp);

        return "Reminder set for " + timestamp + ": " + content;
    }

    /** Utile pour inspecter/tester ce qui a été posé pendant une conversation. */
    public static List<String> getReminders() {
        return List.copyOf(REMINDERS);
    }

    public static final Tool SET_REMINDER_SCHEMA = Tool.builder()
            .name("set_reminder")
            .description("""
                    Sets a reminder with the given content at the given timestamp. Use this once you \
                    know both what to remind the user about and the exact date/time to remind them at \
                    — typically after computing that date/time with add_duration_to_datetime. Returns \
                    a confirmation string once the reminder has been recorded.""")
            .inputSchema(Tool.InputSchema.builder()
                    .properties(Tool.InputSchema.Properties.builder()
                            .putAdditionalProperty("content", JsonValue.from(Map.of(
                                    "type", "string",
                                    "description", "What the reminder is about, e.g. \"Doctor's appointment\"."
                            )))
                            .putAdditionalProperty("timestamp", JsonValue.from(Map.of(
                                    "type", "string",
                                    "description", "The date/time at which to remind the user, in ISO 8601 format (e.g. \"2050-06-27T00:00:00\")."
                            )))
                            .build())
                    .required(List.of("content", "timestamp"))
                    .build())
            .build();
}
