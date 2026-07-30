package com.formation.claudeapi.features.codeexecution;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.formation.claudeapi.AbstractClaudeConversation;

/**
 * Client Anthropic pour les fonctionnalités beta utilisées dans la section
 * de cours : "Code Execution" et "Files API".
 */
public final class BetaClient extends AbstractClaudeConversation {

    private static final String CODE_EXECUTION_BETA = "code-execution-2025-08-25";
    private static final String FILES_API_BETA = "files-api-2025-04-14";

    private BetaClient() {
        // classe utilitaire, non instanciable
    }

    public static AnthropicClient build() {
        return AnthropicOkHttpClient.builder()
                .apiKey(dotenv.get("ANTHROPIC_API_KEY"))
                .putHeader("anthropic-beta", CODE_EXECUTION_BETA + "," + FILES_API_BETA)
                .build();
    }
}
