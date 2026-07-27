package com.formation.claudeapi.rag.retriever;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Chapitre RAG and Agentic Search, section A Multi-Index RAG pipeline.
 * <p>
 * Orchestrateur des algorithms de recherches (Recherche sémantique et Recherche lexicale),
 * Puis fusionne leurs classements via la fusion de rang réciproque (RRF — "reciprocal rank fusion")
 * pour produire un classement unique qui combine le meilleur des deux approches.
 */
public class Retriever {

    private static final int DEFAULT_K_RRF = 60;
    private static final int CANDIDATE_MULTIPLIER = 5;

    private final List<SearchIndex> indexes;

    public Retriever(SearchIndex... indexes) {
        if (indexes == null || indexes.length == 0) {
            throw new IllegalArgumentException("At least one index must be provided");
        }
        this.indexes = Arrays.asList(indexes);
    }

    public void addDocument(Map<String, Object> document) {
        for (SearchIndex index : indexes) {
            index.addDocument(document);
        }
    }

    public void addDocuments(List<Map<String, Object>> documents) {
        for (SearchIndex index : indexes) {
            index.addDocuments(documents);
        }
    }

    public List<RankedResult> search(String queryText, int k) {
        return search(queryText, k, DEFAULT_K_RRF);
    }

    /**
     * Interroge chaque index séparément avec une marge (k * 5) candidats par index,
     * fusionne les classements par RRF, et retourne les {@code k} meilleurs documents.
     */
    public List<RankedResult> search(String queryText, int k, int kRrf) {
        if (queryText == null) {
            throw new IllegalArgumentException("Query text must be a string.");
        }
        if (k <= 0) {
            throw new IllegalArgumentException("k must be a positive integer.");
        }
        if (kRrf < 0) {
            throw new IllegalArgumentException("k_rrf must be non-negative.");
        }

        List<List<SearchResult>> allResults = new ArrayList<>();
        for (SearchIndex index : indexes) {
            allResults.add(index.search(queryText, k * CANDIDATE_MULTIPLIER));
        }

        // IdentityHashMap = Regrouper les entrées qui référencent EXACTEMENT le même objet document
        // (ajouté une seule fois via addDocument/addDocuments puis partagé par référence entre les index),
        Map<Map<String, Object>, double[]> ranksByDocument = new IdentityHashMap<>();

        for (int indexPosition = 0; indexPosition < allResults.size(); indexPosition++) {
            List<SearchResult> results = allResults.get(indexPosition);
            for (int rank = 0; rank < results.size(); rank++) {
                Map<String, Object> document = results.get(rank).document();
                double[] ranks = ranksByDocument.computeIfAbsent(document, ignored -> {
                    double[] initial = new double[indexes.size()];
                    Arrays.fill(initial, Double.POSITIVE_INFINITY);
                    return initial;
                });
                ranks[indexPosition] = rank + 1;
            }
        }

        List<RankedResult> scoredDocuments = new ArrayList<>();
        for (Map.Entry<Map<String, Object>, double[]> entry : ranksByDocument.entrySet()) {
            double score = reciprocalRankFusionScore(entry.getValue(), kRrf);
            if (score > 0) {
                scoredDocuments.add(new RankedResult(entry.getKey(), score));
            }
        }

        scoredDocuments.sort(Comparator.comparingDouble(RankedResult::score).reversed());

        return scoredDocuments.subList(0, Math.min(k, scoredDocuments.size()));
    }

    /** RRF_score(d) = Σ(1 / (k_rrf + rank_i(d))) */
    private double reciprocalRankFusionScore(double[] ranks, int kRrf) {
        double sum = 0.0;
        for (double rank : ranks) {
            // Ignore les index où le document n'apparaît pas
            if (!Double.isInfinite(rank)) {
                sum += 1.0 / (kRrf + rank);
            }
        }
        return sum;
    }
}
