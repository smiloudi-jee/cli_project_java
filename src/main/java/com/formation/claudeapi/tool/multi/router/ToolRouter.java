package com.formation.claudeapi.tool.multi.router;

import com.formation.claudeapi.tool.multi.tools.AddDurationToDateTimeTool;
import com.formation.claudeapi.tool.multi.tools.GetCurrentDateTimeTool;
import com.formation.claudeapi.tool.multi.tools.SetReminderTool;

import java.util.Map;

/**
 * Routage des tools : fait le lien entre le nom d'un tool demandé par Claude et son implémentation Java.
 * Ajouter un tool se limite à ajouter un {@code case} ici.
 */
public class ToolRouter {

    public static String runTool(String toolName, Map<String, Object> toolInput) {
        return switch (toolName) {
            case "get_current_datetime" -> {
                System.out.println("Appel du tool get_current_datetime");
                yield GetCurrentDateTimeTool.getCurrentDateTime(
                    (String) toolInput.getOrDefault("date_format", GetCurrentDateTimeTool.DEFAULT_FORMAT));

            }

            case "add_duration_to_datetime" -> {
                System.out.println("Appel du tool add_duration_to_datetime");
                yield AddDurationToDateTimeTool.addDurationToDateTime((String) toolInput.get("datetime_str"),
                    ((Number) toolInput.get("duration")).longValue(), (String) toolInput.get("unit"));
            }

            case "set_reminder" -> {
                System.out.println("Appel du tool set_reminder");
                yield SetReminderTool.setReminder((String) toolInput.get("content"),
                        (String) toolInput.get("timestamp"));
            }

            default -> throw new IllegalArgumentException("Unknown tool: " + toolName);
        };
    }
}
