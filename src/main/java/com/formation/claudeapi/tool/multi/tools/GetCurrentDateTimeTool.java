package com.formation.claudeapi.tool.multi.tools;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.Tool;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Premier tool écrit à partir de FirstTool.java.
 */
public class GetCurrentDateTimeTool {

    public static final String DEFAULT_FORMAT = "yyyy-MM-dd HH:mm:ss";

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

    public static final Tool GET_CURRENT_DATETIME_SCHEMA = Tool.builder()
            .name("get_current_datetime")
            .description("""
                    Returns the current date and time formatted according to the specified format. \
                    Use this whenever you need to know the exact current date or time to answer the \
                    user's request, for example as the starting point for a date calculation. Returns \
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
}
