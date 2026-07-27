package com.formation.claudeapi.rag.retriever;

import java.util.Map;

/**
 * Résultat renvoyé par un SearchIndex.search(String query, int k).
 */
public record SearchResult(Map<String, Object> document, double distance) {
}
