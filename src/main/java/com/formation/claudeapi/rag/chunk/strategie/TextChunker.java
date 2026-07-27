package com.formation.claudeapi.rag.chunk.strategie;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Chapitre "RAG and Agentic Search", section "Text chunking strategies".
 * <p>
 * Regroupe les trois stratégies de découpage de texte présentées dans le cours :
 * <ul>
 *     <li>{@link #chunkBySize} — découpage par taille (Size-Based Chunking)</li>
 *     <li>{@link #chunkBySentence} — découpage par phrase (Sentence-Based Chunking)</li>
 *     <li>{@link #chunkBySection} — découpage par structure (Structure-Based Chunking)</li>
 * </ul>
 */
public interface TextChunker {

    int DEFAULT_CHUNK_SIZE = 150;
    int DEFAULT_CHUNK_OVERLAP = 20;
    int DEFAULT_MAX_SENTENCES_PER_CHUNK = 5;
    int DEFAULT_OVERLAP_SENTENCES = 1;

    /**
     * Découpage par taille (Size-Based Chunking).
     * <p>
     * Divise le texte en chaînes de longueur fixe {@code chunkSize}, avec un chevauchement
     * {@code chunkOverlap} entre fragments consécutifs pour conserver du contexte et éviter
     * de couper des mots en plein milieu.
     */
    static List<String> chunkBySize(String text, int chunkSize, int chunkOverlap) {
        if(chunkSize <= 0) {
           chunkSize = DEFAULT_CHUNK_SIZE;
        }

        if(chunkOverlap <= 0) {
              chunkOverlap = DEFAULT_CHUNK_OVERLAP;
        }

        List<String> chunks = new ArrayList<>();
        int startIdx = 0;

        while (startIdx < text.length()) {
            int endIdx = Math.min(startIdx + chunkSize, text.length());

            String chunkText = text.substring(startIdx, endIdx);
            chunks.add(chunkText);

            startIdx = endIdx < text.length() ? endIdx - chunkOverlap : text.length();
        }

        return chunks;
    }

    /**
     * Découpage par phrase (Sentence-Based Chunking).
     * <p>
     * Découpe le texte en phrases individuelles, puis les regroupe par lots de
     * {@code maxSentencesPerChunk}, avec un chevauchement de {@code overlapSentences} phrases
     * entre fragments consécutifs.
     */
    static List<String> chunkBySentence(String text, int maxSentencesPerChunk, int overlapSentences) {
        if(maxSentencesPerChunk <= 0) {
            maxSentencesPerChunk = DEFAULT_MAX_SENTENCES_PER_CHUNK;
        }

        if(overlapSentences < 0) {
            overlapSentences = DEFAULT_OVERLAP_SENTENCES;
        }

        String[] sentences = text.split("(?<=[.!?])\\s+");

        List<String> chunks = new ArrayList<>();
        int startIdx = 0;

        while (startIdx < sentences.length) {
            int endIdx = Math.min(startIdx + maxSentencesPerChunk, sentences.length);

            String currentChunk = String.join(" ", Arrays.asList(sentences).subList(startIdx, endIdx));
            chunks.add(currentChunk);

            startIdx += maxSentencesPerChunk - overlapSentences;

            if (startIdx < 0) {
                startIdx = 0;
            }
        }

        return chunks;
    }


    /**
     * Découpage par structure (Structure-Based Chunking).
     * <p>
     * Découpe le document Markdown sur les marqueurs de titre de niveau 2 ({@code "\n## "}).
     * Chaque fragment correspond à une section complète du document.
     */
    static List<String> chunkBySection(String documentText) {
        return Arrays.asList(documentText.split("\\n## "));
    }
}
