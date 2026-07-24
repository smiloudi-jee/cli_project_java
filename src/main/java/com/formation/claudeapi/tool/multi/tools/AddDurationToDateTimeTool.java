package com.formation.claudeapi.tool.multi.tools;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.Tool;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Deuxième tool de l'exercice : ajouter une durée à une date/heure.
 */
public class AddDurationToDateTimeTool {

    public static String addDurationToDateTime(String datetimeStr, long duration, String unit) {
        if (datetimeStr == null || datetimeStr.isBlank()) {
            throw new IllegalArgumentException("datetimeStr cannot be empty");
        }
        if (unit == null || unit.isBlank()) {
            throw new IllegalArgumentException("unit cannot be empty");
        }

        LocalDateTime base = parseDateTime(datetimeStr);
        LocalDateTime result = applyDuration(base, duration, unit);

        return result.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy h:mm:ss a", Locale.ENGLISH));
    }

    /** Essaie plusieurs formats courants avant d'abandonner avec une erreur explicite. */
    private static LocalDateTime parseDateTime(String datetimeStr) {
        try {
            return LocalDateTime.parse(datetimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException ignored) {
            // essai suivant
        }

        try {
            return LocalDateTime.parse(datetimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignored) {
            // essai suivant
        }

        try {
            return LocalDate.parse(datetimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Unrecognized datetime_str format: " + datetimeStr, e);
        }
    }

    private static LocalDateTime applyDuration(LocalDateTime base, long duration, String unit) {
        return switch (unit.toLowerCase(Locale.ROOT)) {
            case "second", "seconds" -> base.plusSeconds(duration);
            case "minute", "minutes" -> base.plusMinutes(duration);
            case "hour", "hours" -> base.plusHours(duration);
            case "day", "days" -> base.plusDays(duration);
            case "week", "weeks" -> base.plusWeeks(duration);
            case "month", "months" -> base.plusMonths(duration);
            case "year", "years" -> base.plusYears(duration);
            default -> throw new IllegalArgumentException(
                    "Unsupported unit: " + unit + " (expected one of: seconds, minutes, hours, days, weeks, months, years)");
        };
    }

    public static final Tool ADD_DURATION_TO_DATETIME_SCHEMA = Tool.builder()
            .name("add_duration_to_datetime")
            .description("""
                    Adds a duration to a given date/time and returns the resulting date/time as a \
                    human-readable string. Use this whenever you need to calculate a future (or past, \
                    with a negative duration) date/time from a known starting date/time — for example \
                    to figure out what date is N days/weeks/months/years from a given date. Returns a \
                    single string such as "Monday, June 27, 2050 12:00:00 AM".""")
            .inputSchema(Tool.InputSchema.builder()
                    .properties(Tool.InputSchema.Properties.builder()
                            .putAdditionalProperty("datetime_str", JsonValue.from(Map.of(
                                    "type", "string",
                                    "description", "The starting date/time, e.g. \"2050-01-01\" or \"2050-01-01 00:00:00\"."
                            )))
                            .putAdditionalProperty("duration", JsonValue.from(Map.of(
                                    "type", "integer",
                                    "description", "The amount to add to the starting date/time. Use a negative number to go back in time."
                            )))
                            .putAdditionalProperty("unit", JsonValue.from(Map.of(
                                    "type", "string",
                                    "description", "The unit the duration is expressed in.",
                                    "enum", List.of("seconds", "minutes", "hours", "days", "weeks", "months", "years")
                            )))
                            .build())
                    .required(List.of("datetime_str", "duration", "unit"))
                    .build())
            .build();
}
