package com.formation.claudeapi.tool.streaming;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.Tool;

import java.util.List;
import java.util.Map;

/**
 * Tool factice utilisé uniquement pour observer le streaming des arguments JSON
 * (section "Fine grained tool calling"). Reprend l'exemple du cours.
 * Le but de la démo est d'observer les événements de streaming, pas le résultat du tool.
 */
public class SaveArticleTool {

    public static final Tool SAVE_ARTICLE_SCHEMA = Tool.builder()
            .name("save_article")
            .description("""
                    Saves an article with its abstract and metadata. Use this once you have \
                    written a complete abstract and computed its metadata. Returns a confirmation \
                    once the article has been saved.""")
            .inputSchema(Tool.InputSchema.builder()
                    .properties(Tool.InputSchema.Properties.builder()
                            .putAdditionalProperty("abstract", JsonValue.from(Map.of(
                                    "type", "string",
                                    "description", "The abstract of the article."
                            )))
                            .putAdditionalProperty("meta", JsonValue.from(Map.of(
                                    "type", "object",
                                    "description", "Metadata about the article.",
                                    "properties", Map.of(
                                            "word_count", Map.of(
                                                    "type", "integer",
                                                    "description", "Word count of the abstract."),
                                            "review", Map.of(
                                                    "type", "string",
                                                    "description", "A short review of the article.")
                                    ),
                                    "required", List.of("word_count", "review")
                            )))
                            .build())
                    .required(List.of("abstract", "meta"))
                    .build())
            .build();
}
