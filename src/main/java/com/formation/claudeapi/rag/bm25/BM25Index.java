package com.formation.claudeapi.rag.bm25;

import com.formation.claudeapi.rag.retriever.SearchIndex;
import com.formation.claudeapi.rag.retriever.SearchResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Chapitre RAG and Agentic Search, section BM25 lexical search.
 * <p>
 * Index de recherche lexicale : BM25 pondère chaque terme par sa rareté dans le corpus (IDF)
 * et sature l'effet des répétitions d'un même terme dans un document (paramètre {@code k1}),
 * avec une normalisation par la longueur du document (paramètre {@code b}).
 */
public class BM25Index implements SearchIndex {

    private static final Pattern NON_WORD_CHARS = Pattern.compile("\\W+");
    private static final double EPSILON = 1e-9;
    private static final double DEFAULT_K1 = 1.5;
    private static final double DEFAULT_B = 0.75;
    private static final double DEFAULT_SCORE_NORMALIZATION_FACTOR = 0.1;

    private final List<Map<String, Object>> documents = new ArrayList<>();
    private final List<List<String>> corpusTokens = new ArrayList<>();
    private final List<Integer> docLengths = new ArrayList<>();
    private final Map<String, Integer> docFrequencies = new HashMap<>();
    private double avgDocLength = 0.0;
    private final Map<String, Double> idf = new HashMap<>();
    private boolean indexBuilt = false;

    private final double k1;
    private final double b;
    private final Function<String, List<String>> tokenizer;

    public BM25Index() {
        this(DEFAULT_K1, DEFAULT_B, null);
    }

    public BM25Index(double k1, double b, Function<String, List<String>> tokenizer) {
        this.k1 = k1;
        this.b = b;
        this.tokenizer = tokenizer != null ? tokenizer : BM25Index::defaultTokenizer;
    }

    private static List<String> defaultTokenizer(String text) {
        String[] rawTokens = NON_WORD_CHARS.split(text.toLowerCase());
        List<String> tokens = new ArrayList<>();
        for (String token : rawTokens) {
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    @Override
    public void addDocument(Map<String, Object> document) {
        String content = requireContent(document, -1);
        List<String> docTokens = tokenizer.apply(content);

        documents.add(document);
        corpusTokens.add(docTokens);
        updateStatsOnAdd(docTokens);
    }

    @Override
    public void addDocuments(List<Map<String, Object>> newDocuments) {
        if (newDocuments == null) {
            throw new IllegalArgumentException("Documents must be a list of maps.");
        }
        if (newDocuments.isEmpty()) {
            return;
        }

        for (int i = 0; i < newDocuments.size(); i++) {
            String content = requireContent(newDocuments.get(i), i);
            List<String> docTokens = tokenizer.apply(content);

            documents.add(newDocuments.get(i));
            corpusTokens.add(docTokens);
            updateStatsOnAdd(docTokens);
        }

        indexBuilt = false;
    }

    private String requireContent(Map<String, Object> document, int index) {
        String suffix = index >= 0 ? " at index " + index : "";
        if (document == null) {
            throw new IllegalArgumentException("Document" + suffix + " must be a map not null.");
        }
        if (!document.containsKey("content")) {
            throw new IllegalArgumentException("Document" + suffix + " must contain a 'content' key.");
        }
        Object content = document.get("content");
        if (!(content instanceof String contentText)) {
            throw new IllegalArgumentException("Document 'content'" + suffix + " must be a string.");
        }
        return contentText;
    }

    /** Normalisation : longueur du document + fréquence de document (une occurrence max par doc et par terme). */
    private void updateStatsOnAdd(List<String> docTokens) {
        docLengths.add(docTokens.size());

        Set<String> seenInDoc = new HashSet<>();
        for (String token : docTokens) {
            if (seenInDoc.add(token)) {
                docFrequencies.merge(token, 1, Integer::sum);
            }
        }

        indexBuilt = false;
    }

    /** Idf(terme) = log(((N - freq + 0.5) / (freq + 0.5)) + 1). */
    private void calculateIdf() {
        int documentCount = documents.size();
        idf.clear();
        for (Map.Entry<String, Integer> entry : docFrequencies.entrySet()) {
            int freq = entry.getValue();
            double idfScore = Math.log(((documentCount - freq + 0.5) / (freq + 0.5)) + 1);
            idf.put(entry.getKey(), idfScore);
        }
    }

    /** Reconstruction des index (uniquement au moment d'une recherche), pas à chaque ajout. */
    private void buildIndex() {
        if (documents.isEmpty()) {
            avgDocLength = 0.0;
            idf.clear();
            indexBuilt = true;
            return;
        }

        int totalLength = 0;
        for (int length : docLengths) {
            totalLength += length;
        }
        avgDocLength = (double) totalLength / documents.size();
        calculateIdf();
        indexBuilt = true;
    }

    /** Formule BM25 classique pour le couple (requête tokenisée, document). */
    private double computeBm25Score(List<String> queryTokens, int docIndex) {
        double score = 0.0;

        Map<String, Integer> docTermCounts = new HashMap<>();
        for (String token : corpusTokens.get(docIndex)) {
            docTermCounts.merge(token, 1, Integer::sum);
        }
        int docLength = docLengths.get(docIndex);

        for (String token : queryTokens) {
            Double termIdf = idf.get(token);
            if (termIdf == null) {
                continue;
            }

            int termFreq = docTermCounts.getOrDefault(token, 0);

            double numerator = termIdf * termFreq * (k1 + 1);
            double denominator = termFreq + k1 * (1 - b + b * (docLength / avgDocLength));
            score += numerator / (denominator + EPSILON);
        }

        return score;
    }

    /** Search(query_text, k) avec le facteur de normalisation par défaut (0.1). */
    @Override
    public List<SearchResult> search(String queryText, int k) {
        return search(queryText, k, DEFAULT_SCORE_NORMALIZATION_FACTOR);
    }

    /**
     * Calcule le score BM25 brut de chaque document (plus haut = plus pertinent),
     * On conserve les {@code k} meilleurs, puis normalise chaque score brut en pseudo-distance
     * via {@code exp(-factor * score_brut)}.
     */
    public List<SearchResult> search(String queryText, int k, double scoreNormalizationFactor) {
        if (documents.isEmpty()) {
            return List.of();
        }
        if (queryText == null) {
            throw new IllegalArgumentException("Query text must be a string.");
        }
        if (k <= 0) {
            throw new IllegalArgumentException("k must be a positive integer.");
        }

        if (!indexBuilt) {
            buildIndex();
        }
        if (avgDocLength == 0) {
            return List.of();
        }

        List<String> queryTokens = tokenizer.apply(queryText);
        if (queryTokens.isEmpty()) {
            return List.of();
        }

        List<RawScore> rawScores = new ArrayList<>();
        for (int i = 0; i < documents.size(); i++) {
            double rawScore = computeBm25Score(queryTokens, i);
            if (rawScore > EPSILON) {
                rawScores.add(new RawScore(documents.get(i), rawScore));
            }
        }
        rawScores.sort(Comparator.comparingDouble(RawScore::score).reversed());

        List<RawScore> topK = rawScores.subList(0, Math.min(k, rawScores.size()));

        List<SearchResult> normalizedResults = new ArrayList<>();
        for (RawScore raw : topK) {
            double normalizedScore = Math.exp(-scoreNormalizationFactor * raw.score());
            normalizedResults.add(new SearchResult(raw.document(), normalizedScore));
        }
        normalizedResults.sort(Comparator.comparingDouble(SearchResult::distance));

        return normalizedResults;
    }

    public int size() {
        return documents.size();
    }

    @Override
    public String toString() {
        return "BM25Index(count=" + size() + ", k1=" + k1 + ", b=" + b + ", indexBuilt=" + indexBuilt + ")";
    }

    private record RawScore(Map<String, Object> document, double score) {
    }
}
