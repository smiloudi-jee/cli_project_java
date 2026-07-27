package com.formation.claudeapi.rag;

import com.formation.claudeapi.AbstractClaudeConversation;
import com.formation.claudeapi.rag.bm25.BM25Index;
import com.formation.claudeapi.rag.helper.VoyageEmbeddingClient;
import com.formation.claudeapi.rag.retriever.RankedResult;
import com.formation.claudeapi.rag.retriever.Retriever;
import com.formation.claudeapi.rag.chunk.strategie.TextChunker;
import com.formation.claudeapi.rag.vectordb.VectorIndex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Chapitre RAG and Agentic Search, section A Multi-Index RAG pipeline :
 * Combine la recherche sémantique et la recherche lexicale puis fusionne les deux classements
 * par fusion de rang réciproque (RRF).
 */
public class HybridRetrieverDemo extends AbstractClaudeConversation {

    private static final String QUERY = "What happened with INC-2023-Q4-011?";
    private static final int TOP_K = 3;

    public static void main(String[] args) throws IOException {
        String text = readReport();
        List<String> chunks = TextChunker.chunkBySection(text);

        VoyageEmbeddingClient embeddingClient = new VoyageEmbeddingClient();

        VectorIndex vectorIndex = new VectorIndex(
                VectorIndex.DistanceMetric.COSINE,
                embeddingClient::generateChunkEmbedding,
                embeddingClient::generateChunkEmbeddings);
        BM25Index bm25Index = new BM25Index();

        Retriever retriever = new Retriever(bm25Index, vectorIndex);

        List<Map<String, Object>> documents = new ArrayList<>();
        for (String chunk : chunks) {
            documents.add(Map.of("content", (Object) chunk));
        }
        retriever.addDocuments(documents);

        List<RankedResult> results = retriever.search(QUERY, TOP_K);

        for (RankedResult result : results) {
            String content = (String) result.document().get("content");
            String preview = content.substring(0, Math.min(200, content.length()));
            System.out.println(result.score() + "\n" + preview + "\n----\n");
        }
    }
}
