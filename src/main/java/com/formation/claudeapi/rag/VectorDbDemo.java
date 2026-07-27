package com.formation.claudeapi.rag;

import com.formation.claudeapi.AbstractClaudeConversation;
import com.formation.claudeapi.rag.helper.VoyageEmbeddingClient;
import com.formation.claudeapi.rag.retriever.SearchResult;
import com.formation.claudeapi.rag.chunk.strategie.TextChunker;
import com.formation.claudeapi.rag.vectordb.VectorIndex;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Chapitre "RAG and Agentic Search", section "Implementing the RAG flow" :
 * <ol>
 *     <li>Découper le texte par section (chunk_by_section) ;</li>
 *     <li>Générer les embeddings de tous les fragments, en un seul appel ;</li>
 *     <li>Créer un vector store et y ajouter chaque embedding avec son texte associé ;</li>
 *     <li>Générer l'embedding de la question de l'utilisateur ;</li>
 *     <li>Interroger le store pour trouver les fragments les plus pertinents.</li>
 * </ol>
 */
public class VectorDbDemo extends AbstractClaudeConversation {

    private static final String USER_QUESTION = "What did the software engineering dept do last year?";
    private static final int TOP_K = 2;

    public static void main(String[] args) throws IOException {
        // Étape 1 : Découper le texte par section (chunk_by_section)
        String text = readReport();
        List<String> chunks = TextChunker.chunkBySection(text);
        System.out.println("chunks[2] (table des matières) :\n" + chunks.get(2) + "\n");

        VoyageEmbeddingClient embeddingClient = new VoyageEmbeddingClient();

        // Étape 2 : Générer les embeddings de tous les fragments, en un seul appel batch
        List<List<Double>> embeddings = embeddingClient.generateChunkEmbeddings(chunks);

        // Étape 3 : Créer un vector store et y ajouter chaque embedding avec son texte associé
        VectorIndex store = new VectorIndex();
        for (int i = 0; i < embeddings.size(); i++) {
            store.addVector(embeddings.get(i), Map.of("content", (Object) chunks.get(i)));
        }
        System.out.println("Store : ");
        System.out.println(store);

        // Étape 4 : Générer l'embedding de la question de l'utilisateur
        System.out.println("User question : " + USER_QUESTION);
        List<Double> userEmbedding = embeddingClient.generateQueryEmbedding(USER_QUESTION);

        // Étape 5 : Interroger le store pour trouver les fragments les plus pertinents.
        List<SearchResult> results = store.search(userEmbedding, TOP_K);

        for (SearchResult result : results) {
            String content = (String) result.document().get("content");
            String preview = content.substring(0, Math.min(200, content.length()));
            System.out.println("Distance : " + result.distance() + "\n" + preview + "\n");
        }
    }
}
