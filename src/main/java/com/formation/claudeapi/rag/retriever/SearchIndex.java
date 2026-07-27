package com.formation.claudeapi.rag.retriever;

import java.util.List;
import java.util.Map;

/**
 * Chapitre RAG and Agentic Search, section A Multi-Index RAG pipeline.
 * Contrat minimal commun à toute implémentation d'index de recherche (sémantique & lexicale),
 */
public interface SearchIndex {

    /** Ajoute un seul document à l'index (clé {@code "content"} obligatoire). */
    void addDocument(Map<String, Object> document);

    /**
     * Ajoute plusieurs documents en une seule opération.
     */
    void addDocuments(List<Map<String, Object>> documents);

    /**
     * Recherche les {@code k} documents les plus pertinents pour {@code query}.
     */
    List<SearchResult> search(String query, int k);
}
