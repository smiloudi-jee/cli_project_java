package com.formation.claudeapi.rag;

import com.formation.claudeapi.AbstractClaudeConversation;
import com.formation.claudeapi.rag.bm25.BM25Index;
import com.formation.claudeapi.rag.retriever.SearchResult;
import com.formation.claudeapi.rag.chunk.strategie.TextChunker;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Chapitre RAG and Agentic Search, section BM25 lexical search :
 * Recherche lexicale pure (pas d'embeddings, pas d'appel réseau) : BM25 excelle ici précisément
 * parce que la question contient un identifiant exact ("INC-2023-Q4-011") qu'une recherche
 * sémantique seule pourrait diluer.
 */
public class Bm25Demo extends AbstractClaudeConversation {

    private static final String QUERY = "What happened with INC-2023-Q4-011?";
    private static final int TOP_K = 3;

    public static void main(String[] args) throws IOException {
        String text = readReport();
        List<String> chunks = TextChunker.chunkBySection(text);

        BM25Index store = new BM25Index();
        for (String chunk : chunks) {
            store.addDocument(Map.of("content", (Object) chunk));
        }

        List<SearchResult> results = store.search(QUERY, TOP_K);

        for (SearchResult result : results) {
            String content = (String) result.document().get("content");
            String preview = content.substring(0, Math.min(220, content.length()));
            System.out.println(result.distance() + "\n" + preview + "\n----\n");
        }
    }
}
