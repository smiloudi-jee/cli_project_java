package com.formation.claudeapi.features.caching;

import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.Usage;

/**
 * Affiche les compteurs de tokens d'une reponse, en particulier
 * {@code cache_creation_input_tokens} et {@code cache_read_input_tokens} :
 * c'est la seule facon de verifier concretement qu'un cache a bien ete ecrit
 * puis relu d'un appel a l'autre (le texte de la reponse, lui, ne change pas).
 */
public final class CacheUsagePrinter {

    private CacheUsagePrinter() {
        // classe utilitaire, non instanciable
    }

    public static void print(String label, Message message) {
        Usage usage = message.usage();
        System.out.printf(
                "%s -> input=%d, output=%d, cache_creation=%d, cache_read=%d%n",
                label,
                usage.inputTokens(),
                usage.outputTokens(),
                usage.cacheCreationInputTokens(),
                usage.cacheReadInputTokens()
        );
    }
}
