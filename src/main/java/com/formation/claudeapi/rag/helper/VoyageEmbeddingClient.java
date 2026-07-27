package com.formation.claudeapi.rag.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formation.claudeapi.AbstractClaudeConversation;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Chapitre "RAG and Agentic Search", section "Text embeddings" et section "Implementing the RAG flow".
 * <p>
 * Anthropic ne fournit pas nativement de génération d'embeddings, le cours recommande
 * VoyageAI (<a href="https://www.voyageai.com/">voyageai.com</a>) comme fournisseur de service.
 * <p>
 * Les méthodes sont nommées d'après l'étape du pipeline RAG (section "Implementing the RAG
 * flow") où elles interviennent, plutôt que d'avoir un seul {@code generateEmbedding} générique
 * réutilisé partout — ça évite de devoir deviner, au niveau d'un appel, si on est en train
 * d'indexer un document ou de traiter une question utilisateur :
 * <ul>
 *     <li><b>Étape 2</b> — indexation : {@link #generateChunkEmbedding(String)} /
 *     {@link #generateChunkEmbeddings(List)} embarquent chaque fragment de document ;</li>
 *     <li><b>Étape 4</b> — interrogation : {@link #generateQueryEmbedding(String)} embarque la
 *     question posée par l'utilisateur, pour être comparée aux embeddings déjà stockés.</li>
 * </ul>
 * Les deux appellent en réalité la même API VoyageAI de la même façon ; seule l'intention
 * diffère, d'où le choix de deux noms distincts plutôt que deux méthodes identiques.
 */
public class VoyageEmbeddingClient extends AbstractClaudeConversation {

    private static final String EMBEDDINGS_URL = "https://api.voyageai.com/v1/embeddings";
    private static final String DEFAULT_MODEL = "voyage-3-large";
    private static final String DEFAULT_INPUT_TYPE = "query";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;

    public VoyageEmbeddingClient() {
        this.apiKey = dotenv.get("VOYAGE_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "VOYAGE_API_KEY manquante dans le fichier .env "
                            + "(voir le document annexe VoyageAI_API_Key_Directions.pdf)");
        }
    }

    /** Étape 2 : Embedding d'un seul fragment de document. */
    public List<Double> generateChunkEmbedding(String chunkText) {
        return generateChunkEmbedding(chunkText, DEFAULT_MODEL, DEFAULT_INPUT_TYPE);
    }

    /** Étape 2 : Embedding d'un seul fragment de document, avec modèle/input_type explicites. */
    public List<Double> generateChunkEmbedding(String chunkText, String model, String inputType) {
        List<List<Double>> embeddings = callEmbeddingsApi(List.of(chunkText), model, inputType);
        return embeddings.getFirst();
    }

    /** Étape 2 : Embedding de tous les fragments de document, en un seul appel. */
    public List<List<Double>> generateChunkEmbeddings(List<String> chunks) {
        return generateChunkEmbeddings(chunks, DEFAULT_MODEL, DEFAULT_INPUT_TYPE);
    }

    /** Étape 2 : Embedding de tous les fragments de document, avec modèle/input_type explicites. */
    public List<List<Double>> generateChunkEmbeddings(List<String> chunks, String model, String inputType) {
        return callEmbeddingsApi(chunks, model, inputType);
    }

    /** Étape 4 : Embedding de la question posée par l'utilisateur. */
    public List<Double> generateQueryEmbedding(String userQuestion) {
        return generateQueryEmbedding(userQuestion, DEFAULT_MODEL, DEFAULT_INPUT_TYPE);
    }

    /** Étape 4 : Embedding de la question utilisateur, avec modèle/input_type explicites. */
    public List<Double> generateQueryEmbedding(String userQuestion, String model, String inputType) {
        List<List<Double>> embeddings = callEmbeddingsApi(List.of(userQuestion), model, inputType);
        return embeddings.getFirst();
    }

    /** Appel HTTP POST /v1/embeddings */
    private List<List<Double>> callEmbeddingsApi(List<String> texts, String model, String inputType) {
        try {
            String requestBody = objectMapper.writeValueAsString(
                    new EmbeddingRequest(texts, model, inputType));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(EMBEDDINGS_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IOException(
                        "Appel VoyageAI échoué (HTTP " + response.statusCode() + ") : " + response.body());
            }

            JsonNode dataArray = objectMapper.readTree(response.body()).path("data");

            // L'API renvoie chaque embedding avec son "index" d'origine : on trie dessus pour
            // garantir que l'ordre des embeddings retournés correspond bien à l'ordre des textes
            // envoyés dans la requête, même si l'API ne les renvoyait pas dans le même ordre.
            List<JsonNode> orderedEntries = new ArrayList<>();
            dataArray.forEach(orderedEntries::add);
            orderedEntries.sort(Comparator.comparingInt(a -> a.path("index").asInt()));

            List<List<Double>> embeddings = new ArrayList<>();
            for (JsonNode entry : orderedEntries) {
                List<Double> embedding = new ArrayList<>();
                entry.path("embedding").forEach(value -> embedding.add(value.asDouble()));
                embeddings.add(embedding);
            }
            return embeddings;
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la génération des embeddings VoyageAI", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Appel VoyageAI interrompu", e);
        }
    }

    /**
     * Corps de la requête {@code POST /v1/embeddings}.
     */
    private record EmbeddingRequest(List<String> input, String model, String input_type) {
    }
}
