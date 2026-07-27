package com.formation.claudeapi.rag.vectordb;

import com.formation.claudeapi.rag.retriever.SearchIndex;
import com.formation.claudeapi.rag.retriever.SearchResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Chapitre "RAG and Agentic Search", sections "The full RAG flow", "Implementing the RAG flow"
 * et "A Multi-Index RAG pipeline".
 * <p>
 * Simulation d'une Base de données vectorielle "en mémoire" :
 * <ol>
 *     <li>Une simple liste de vecteurs et de documents associés ;</li>
 *     <li>Une fonction de distance (cosinus ou euclidienne) ;</li>
 *     <li>Une recherche par force brute (on compare la requête à chaque vecteur stocké).</li>
 * </ol>
 */
public class VectorIndex implements SearchIndex {

    public enum DistanceMetric {
        COSINE,
        EUCLIDEAN
    }

    private final List<List<Double>> vectors = new ArrayList<>();
    private final List<Map<String, Object>> documents = new ArrayList<>();
    private Integer vectorDim;
    private final DistanceMetric distanceMetric;
    private final Function<String, List<Double>> embeddingFn;
    private final Function<List<String>, List<List<Double>>> batchEmbeddingFn;

    public VectorIndex() {
        this(DistanceMetric.COSINE, null, null);
    }

    /** Équivalent de VectorIndex(distance_metric, embedding_fn). Pas de fonction batch dédiée : {@link #addDocuments} retombe sur un embedding fragment par fragment. */
    public VectorIndex(DistanceMetric distanceMetric, Function<String, List<Double>> embeddingFn) {
        this(distanceMetric, embeddingFn, null);
    }

    /**
     * Variante avec une fonction d'embedding "batch" dédiée, utilisée par {@link #addDocuments}
     * pour embarquer tous les fragments en un seul appel réseau plutôt qu'un par fragment — c'est
     * ce qui évite le rate limiting VoyageAI évoqué dans le notebook "005_hybrid.ipynb"
     * (ex. {@code new VectorIndex(DistanceMetric.COSINE, client::generateChunkEmbedding, client::generateChunkEmbeddings)}).
     */
    public VectorIndex(DistanceMetric distanceMetric,
                        Function<String, List<Double>> embeddingFn,
                        Function<List<String>, List<List<Double>>> batchEmbeddingFn) {
        this.distanceMetric = distanceMetric != null ? distanceMetric : DistanceMetric.COSINE;
        this.embeddingFn = embeddingFn;
        this.batchEmbeddingFn = batchEmbeddingFn;
    }

    /**
     * Équivalent de add_document(document) : calcule l'embedding du document via la fonction
     * d'embedding fournie au constructeur, puis délègue à addVector.
     */
    @Override
    public void addDocument(Map<String, Object> document) {
        if (embeddingFn == null) {
            throw new IllegalStateException("Embedding function not provided during initialization.");
        }
        String contentText = requireContent(document, -1);

        List<Double> vector = embeddingFn.apply(contentText);
        addVector(vector, document);
    }

    /**
     * Équivalent de add_documents(documents) : embarque tous les fragments en un seul appel via
     * {@code batchEmbeddingFn} si fourni (sinon un appel par fragment via {@code embeddingFn}),
     * puis ajoute chaque paire (embedding, document) au store.
     */
    @Override
    public void addDocuments(List<Map<String, Object>> newDocuments) {
        if (embeddingFn == null) {
            throw new IllegalStateException("Embedding function not provided during initialization.");
        }
        if (newDocuments == null) {
            throw new IllegalArgumentException("Documents must be a list of maps.");
        }
        if (newDocuments.isEmpty()) {
            return;
        }

        List<String> contents = new ArrayList<>();
        for (int i = 0; i < newDocuments.size(); i++) {
            contents.add(requireContent(newDocuments.get(i), i));
        }

        List<List<Double>> vectors = (batchEmbeddingFn != null)
                ? batchEmbeddingFn.apply(contents)
                : contents.stream().map(embeddingFn).toList();

        for (int i = 0; i < vectors.size(); i++) {
            addVector(vectors.get(i), newDocuments.get(i));
        }
    }

    private String requireContent(Map<String, Object> document, int index) {
        String suffix = index >= 0 ? " at index " + index : "";
        if (document == null) {
            throw new IllegalArgumentException("Document" + suffix + " must be a map.");
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

    /**
     * Ajoute une paire (embedding déjà calculé, document) au store.
     * Vérifie que la dimension du vecteur est cohérente avec celle des vecteurs déjà stockés.
     */
    public void addVector(List<Double> vector, Map<String, Object> document) {
        if (vector == null) {
            throw new IllegalArgumentException("Vector must be a list of numbers.");
        }
        if (document == null) {
            throw new IllegalArgumentException("Document must be a map.");
        }
        if (!document.containsKey("content")) {
            throw new IllegalArgumentException("Document map must contain a 'content' key.");
        }

        if (vectors.isEmpty()) {
            vectorDim = vector.size();
        } else if (vector.size() != vectorDim) {
            throw new IllegalArgumentException(
                    "Inconsistent vector dimension. Expected " + vectorDim + ", got " + vector.size());
        }

        vectors.add(new ArrayList<>(vector));
        documents.add(document);
    }

    /**
     * Équivalent de search(query, k) lorsque query est une chaîne de texte : la requête est
     * d'abord transformée en vecteur via la fonction d'embedding fournie au constructeur, puis on
     * délègue à {@link #search(List, int)}.
     */
    @Override
    public List<SearchResult> search(String query, int k) {
        if (embeddingFn == null) {
            throw new IllegalStateException("Embedding function not provided for string query.");
        }
        return search(embeddingFn.apply(query), k);
    }

    /**
     * Équivalent de search(query, k) lorsque query est déjà un vecteur numérique. Retourne les
     * documents dont le vecteur associé est le plus proche de la requête, triés par distance
     * croissante (donc du plus pertinent au moins pertinent).
     */
    public List<SearchResult> search(List<Double> queryVector, int k) {
        if (vectors.isEmpty() || vectorDim == null) {
            return List.of();
        }
        if (queryVector.size() != vectorDim) {
            throw new IllegalArgumentException(
                    "Query vector dimension mismatch. Expected " + vectorDim + ", got " + queryVector.size());
        }
        if (k <= 0) {
            throw new IllegalArgumentException("k must be a positive integer.");
        }

        List<SearchResult> distances = new ArrayList<>();
        for (int i = 0; i < vectors.size(); i++) {
            double distance = (distanceMetric == DistanceMetric.COSINE)
                    ? cosineDistance(queryVector, vectors.get(i))
                    : euclideanDistance(queryVector, vectors.get(i));
            distances.add(new SearchResult(documents.get(i), distance));
        }

        distances.sort(Comparator.comparingDouble(SearchResult::distance));

        return distances.subList(0, Math.min(k, distances.size()));
    }

    private double euclideanDistance(List<Double> vec1, List<Double> vec2) {
        requireSameDimension(vec1, vec2);

        double sumOfSquares = 0.0;
        for (int i = 0; i < vec1.size(); i++) {
            double diff = vec1.get(i) - vec2.get(i);
            sumOfSquares += diff * diff;
        }
        return Math.sqrt(sumOfSquares);
    }

    private double dotProduct(List<Double> vec1, List<Double> vec2) {
        requireSameDimension(vec1, vec2);

        double sum = 0.0;
        for (int i = 0; i < vec1.size(); i++) {
            sum += vec1.get(i) * vec2.get(i);
        }
        return sum;
    }

    private double magnitude(List<Double> vec) {
        double sumOfSquares = 0.0;
        for (double x : vec) {
            sumOfSquares += x * x;
        }
        return Math.sqrt(sumOfSquares);
    }

    private double cosineDistance(List<Double> vec1, List<Double> vec2) {
        requireSameDimension(vec1, vec2);

        double mag1 = magnitude(vec1);
        double mag2 = magnitude(vec2);

        if (mag1 == 0.0 && mag2 == 0.0) {
            return 0.0;
        } else if (mag1 == 0.0 || mag2 == 0.0) {
            return 1.0;
        }

        double dotProd = dotProduct(vec1, vec2);
        double cosineSimilarity = dotProd / (mag1 * mag2);
        cosineSimilarity = Math.clamp(cosineSimilarity, -1.0, 1.0);

        return 1.0 - cosineSimilarity;
    }

    private void requireSameDimension(List<Double> vec1, List<Double> vec2) {
        if (vec1.size() != vec2.size()) {
            throw new IllegalArgumentException("Vectors must have the same dimension");
        }
    }

    public int size() {
        return vectors.size();
    }

    @Override
    public String toString() {
        String hasEmbedFn = embeddingFn != null ? "Yes" : "No";
        return "VectorIndex(count=" + size()
                + ", dim=" + vectorDim
                + ", metric='" + distanceMetric
                + "', hasEmbeddingFn='" + hasEmbedFn + "')";
    }
}
