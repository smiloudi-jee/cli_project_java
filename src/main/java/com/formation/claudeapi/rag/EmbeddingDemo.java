package com.formation.claudeapi.rag;

import com.formation.claudeapi.AbstractClaudeConversation;
import com.formation.claudeapi.rag.helper.VoyageEmbeddingClient;
import com.formation.claudeapi.rag.chunk.strategie.TextChunker;

import java.io.IOException;
import java.util.List;

/**
 * Découpe le rapport d'exemple par taille, par phrase et par section puis affiche l'embedding VoyageAI chaque fragment.
 */
public class EmbeddingDemo extends AbstractClaudeConversation {

    public static void main(String[] args) throws IOException {
        chunkBySize();
        chunkBySentence();
        chunkBySection();
    }

    private static void chunkBySize() throws IOException {
        String text = readReport();
        List<String> chunks = TextChunker.chunkBySize(text, 0, 0);
        List<Double> embedding = generateEmbedding(chunks);
        printEmbedding(embedding, "By Section");
    }

    private static void chunkBySentence() throws IOException {
        String text = readReport();
        List<String> chunks = TextChunker.chunkBySentence(text, 0, 0);
        List<Double> embedding = generateEmbedding(chunks);
        printEmbedding(embedding, "By Sentence");
    }

    private static void chunkBySection() throws IOException {
        String text = readReport();
        List<String> chunks = TextChunker.chunkBySection(text);
        List<Double> embedding = generateEmbedding(chunks);
        printEmbedding(embedding, "By Section");
    }

    /** Uniquement pour le premier fragment */
    private static List<Double> generateEmbedding(List<String> chunks) {
        VoyageEmbeddingClient client = new VoyageEmbeddingClient();
        return client.generateChunkEmbedding(chunks.getFirst());
    }

    private static void printEmbedding(List<Double> embedding, String type) {
        System.out.println("\n---------------- Begin "+type+" ------------------\n");
        System.out.println(embedding);
        System.out.println("\n---------------- End "+type+" ------------------\n");
    }
}
