package com.formation.claudeapi.rag.retriever;

import java.util.Map;

/**
 * Résultat renvoyé par {@link Retriever#search(String, int)} après fusion de rang réciproque
 * (RRF — "Reciprocal Rank Fusion").
 */
public record RankedResult(Map<String, Object> document, double score) {
}
