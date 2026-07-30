package com.formation.claudeapi.features.caching;

import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.formation.claudeapi.AbstractClaudeConversation;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Demo "Prompt caching" - mise en cache du system prompt.
 * <p>
 * Le system prompt utilise ici (~6000 tokens, fourni avec le cours) decrit un
 * generateur de code Javascript pour un builder de flux d'analyse de
 * documents. Sans cache, ce prompt entier est retraite - et repaye au plein
 * tarif - a chaque appel, meme pour une question anodine comme "1+1".
 * <p>
 * Avec {@code cache_control}, seul le premier appel paie le cout complet
 * (visible dans {@code cache_creation_input_tokens}) ; les appels suivants
 * relisent le cache ({@code cache_read_input_tokens}), nettement moins cher.
 * Pour attacher un cache_control au system prompt, il faut le fournir sous
 * forme de liste de blocs "text" (via {@code systemOfTextBlockParams})
 * plutot que comme une simple chaine de caracteres.
 */
public class CachedSystemPromptDemo extends AbstractClaudeConversation {

    private static final String SYSTEM_PROMPT_RESOURCE = "/caching/code-generation-system-prompt.txt";

    public static void main(String[] args) {
        String codePrompt = new String(
                readResourceBytes(CachedSystemPromptDemo.class, SYSTEM_PROMPT_RESOURCE),
                StandardCharsets.UTF_8
        );

        Message first = askWithCachedSystemPrompt(codePrompt, "What's 1+1?");
        CacheUsagePrinter.print("1er appel  (ecriture du cache)", first);

        Message second = askWithCachedSystemPrompt(codePrompt, "What's 2+2?");
        CacheUsagePrinter.print("2e appel   (lecture du cache)", second);
    }

    private static Message askWithCachedSystemPrompt(String codePrompt, String question) {
        List<MessageParam> messages = new ArrayList<>();
        addUserMessage(messages, question);

        MessageCreateParams params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(1024L)
                .systemOfTextBlockParams(List.of(CacheControls.cachedSystemBlock(codePrompt)))
                .messages(messages)
                .build();

        return buildClient().messages().create(params);
    }
}
