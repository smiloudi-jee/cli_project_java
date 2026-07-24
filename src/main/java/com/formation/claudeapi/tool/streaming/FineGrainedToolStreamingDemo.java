package com.formation.claudeapi.tool.streaming;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.http.StreamResponse;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.RawMessageStreamEvent;
import com.anthropic.models.messages.ToolUnion;
import com.formation.claudeapi.AbstractClaudeConversation;
import com.formation.claudeapi.tool.existing.ToolSchemasForExistingTools;

import java.util.ArrayList;
import java.util.List;

/**
 * Streaming des arguments JSON d'un tool_use — section "Fine grained tool calling".
 * Port du pattern déjà prouvé dans {@code streaming.response.BasicStreaming}
 * ({@code event.contentBlockDelta()} puis {@code delta().text()}), étendu ici pour
 * aussi observer les deltas de type "input_json" (les morceaux de JSON qui composent
 * progressivement l'objet {@code input} du tool_use, ex. {@code save_article}).
 * <p>
 * Sans activation spéciale, l'API tamponne (buffer) ces chunks et ne les livre que par
 * paire clé/valeur top-level complète et déjà validée — d'où l'effet "pause puis rafale"
 * décrit dans le cours ("How JSON Validation Works"). Le mode fine-grained lève ce
 * tampon : les fragments de JSON arrivent au fil de l'eau, potentiellement invalides
 * tant que l'objet n'est pas complet — à nous de les accumuler et de ne parser qu'à
 * la fin (ou d'utiliser un parseur JSON tolérant au flux).
 * <p>
 * NON VÉRIFIÉ PAR COMPILATION (même réserve que {@link ToolSchemasForExistingTools}) :
 * <ul>
 *   <li>l'accesseur du delta JSON ({@code delta().inputJson()}) est déduit par analogie
 *   avec {@code delta().text()}, prouvé dans {@code BasicStreaming} — le nom exact et le
 *   nom du champ ({@code partialJson()} vs {@code partial_json}) ne sont pas confirmés ;</li>
 *   <li>l'activation du mode fine-grained lui-même n'est pas câblée ci-dessous : côté API
 *   REST c'est un header béta ({@code anthropic-beta: fine-grained-tool-streaming-2025-05-14}),
 *   je n'ai trouvé aucun exemple de header béta ailleurs dans ce projet pour confirmer la
 *   méthode du SDK Java (peut-être {@code .putAdditionalHeader(...)} ou une méthode
 *   {@code .betas(...)} sur le builder). Sans elle, ce code tourne quand même, mais en
 *   streaming standard (avec le tampon de validation par bloc). Regarde l'auto-complétion
 *   sur {@code MessageCreateParams.builder().} si tu veux activer le vrai mode fine-grained.</li>
 * </ul>
 */
public class FineGrainedToolStreamingDemo extends AbstractClaudeConversation {

    public static void main(String[] args) {
        List<MessageParam> messages = new ArrayList<>();
        addUserMessage(messages, "Generate and save an article about computer science");

        AnthropicClient client = buildClient();

        MessageCreateParams params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(1024L)
                .messages(messages)
                .tools(List.of(ToolUnion.ofTool(SaveArticleTool.SAVE_ARTICLE_SCHEMA)))
                .build();

        try (StreamResponse<RawMessageStreamEvent> streamResponse = client.messages().createStreaming(params)) {
            streamResponse.stream()
                    .flatMap(event -> event.contentBlockDelta().stream())
                    .forEach(deltaEvent -> {
                        deltaEvent.delta().text().ifPresent(textDelta ->
                                System.out.print(textDelta.text()));
                        deltaEvent.delta().inputJson().ifPresent(inputJsonDelta ->
                                System.out.println("[input_json] partial_json=\"" + inputJsonDelta.partialJson() + "\""));
                    });
        }

        System.out.println();
    }
}
