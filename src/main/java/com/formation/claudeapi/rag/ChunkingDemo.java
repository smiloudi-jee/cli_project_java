package com.formation.claudeapi.rag;

import com.formation.claudeapi.AbstractClaudeConversation;
import com.formation.claudeapi.rag.chunk.strategie.TextChunker;

import java.io.IOException;
import java.util.List;

/**
 * Découpe le rapport d'exemple par taille, par phrase et par section puis affiche chaque fragment obtenu.
 */
public class ChunkingDemo extends AbstractClaudeConversation {

    public static void main(String[] args) throws IOException {
        chunkBySize();
        chunkBySentence();
        chunkBySection();
    }

    private static void chunkBySize() throws IOException {
        String text = readReport();
        List<String> chunks = TextChunker.chunkBySize(text, 0, 0);
        printChunk(chunks, " By Size");
    }

    private static void chunkBySentence() throws IOException {
        String text = readReport();
        List<String> chunks = TextChunker.chunkBySentence(text, 0, 0);
        printChunk(chunks, "By Sentence");
    }

    private static void chunkBySection() throws IOException {
        String text = readReport();
        List<String> chunks = TextChunker.chunkBySection(text);
        printChunk(chunks, "By Section");
    }

    private static void printChunk(List<String> chunks, String type) {
        System.out.println("\n---------------- Begin "+type+" ------------------\n");
        for (String chunk : chunks) {
            System.out.println(chunk + "\n----\n");
        }
        System.out.println("\n---------------- End "+type+" ------------------\n");
    }
}
