package com.formation.claudeapi.features.codeexecution;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.MultipartField;
import com.anthropic.core.http.HttpResponse;
import com.anthropic.models.beta.files.FileMetadata;
import com.anthropic.models.beta.files.FileUploadParams;
import com.anthropic.models.messages.CodeExecutionOutputBlock;
import com.anthropic.models.messages.CodeExecutionTool20250825;
import com.anthropic.models.messages.CodeExecutionToolResultBlock;
import com.anthropic.models.messages.ContainerUploadBlockParam;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.ToolUnion;
import com.formation.claudeapi.AbstractClaudeConversation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Demo "Code execution and the Files API".
 * <p>
 * Reprend l'avancement du cours :
 * <ul>
 *     <li>1. Upload d'un CSV via "Files API", renvoie un identifiant de fichier réutilisable dans une conversation.</li>
 *     <li>2. Ce fichier est attaché au message utilisateur via un bloc "container_upload".</li>
 *     <li>3. Le tool "code_execution" permet à Claude d'écrire et d'exécuter du code dans la sandbox pour l'analyser</li>
 *     <li>4. Chaque execution de code repart d'un environnement vierge : pas de variable ni d'import conservé d'un appel à l'autre.</li>
 *     <li>5. Les fichiers générés par le code sont eux-mêmes identifiés par un file_id, téléchargeables.</li>
 * </ul>
 * <p>
 */
public class CodeExecutionWithFilesDemo extends AbstractClaudeConversation {

    private static final String CSV_RESOURCE = "/codeexecution/streaming.csv";
    private static final String CSV_FILENAME = "streaming.csv";

    private static final String ANALYSIS_PROMPT = """
            Run a detailed analysis to determine major drivers of churn.
            Your final output should include at least one detailed plot summarizing your findings.

            Critical note: Every time you execute code, you're starting with a completely clean slate. \
            No variables or library imports from previous executions exist. You need to redeclare/reimport \
            all variables/libraries.""";

    private static final Path OUTPUT_DIR = Paths.get("codeexecution-output");

    public static void main(String[] args) throws IOException {
        AnthropicClient client = BetaClient.build();

        FileMetadata uploaded = uploadCsv(client);
        System.out.println("Fichier uploade : " + uploaded.filename() + " (id=" + uploaded.id() + ")");

        Message response = analyzeChurn(client, uploaded.id());
        printTextBlocks(response);
        downloadGeneratedFiles(client, response);
    }

    private static FileMetadata uploadCsv(AnthropicClient client) throws IOException {
        byte[] csvBytes = readResourceBytes(CodeExecutionWithFilesDemo.class, CSV_RESOURCE);

        try (InputStream csvStream = new ByteArrayInputStream(csvBytes)) {
            MultipartField field = MultipartField.builder()
                    .value(csvStream)
                    .filename(CSV_FILENAME)
                    .contentType("text/csv")
                    .build();

            return client.beta().files().upload(FileUploadParams.builder()
                    .file(field)
                    .build());
        }
    }

    private static Message analyzeChurn(AnthropicClient client, String fileId) {
        List<ContentBlockParam> content = List.of(
                text(ANALYSIS_PROMPT),
                ContentBlockParam.ofContainerUpload(ContainerUploadBlockParam.builder()
                        .fileId(fileId)
                        .build())
        );

        List<MessageParam> messages = new ArrayList<>();
        addUserMessage(messages, content);

        MessageCreateParams params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(4096L)
                .messages(messages)
                .tools(List.of(ToolUnion.ofCodeExecutionTool20250825(
                        CodeExecutionTool20250825.builder().build())))
                .build();

        return client.messages().create(params);
    }

    private static void printTextBlocks(Message response) {
        response.content().stream()
            .flatMap(block -> block.text().stream())
            .forEach(textBlock -> System.out.println(textBlock.text()));
    }

    /**
     * Parcourt la réponse à la recherche de blocs "code_execution_tool_result"
     * et télécharge chaque fichier génere par le code execute par Claude.
     */
    private static void downloadGeneratedFiles(AnthropicClient client, Message response) throws IOException {
        List<String> fileIds = response.content().stream()
            .flatMap(block -> block.codeExecutionToolResult().stream())
            .map(CodeExecutionToolResultBlock::content)
            .flatMap(resultContent -> resultContent.resultBlock().stream())
            .flatMap(resultBlock -> resultBlock.content().stream())
            .map(CodeExecutionOutputBlock::fileId)
            .toList();

        if (fileIds.isEmpty()) {
            System.out.println("Aucun fichier genere par le code execution tool.");
            return;
        }

        Files.createDirectories(OUTPUT_DIR);

        for (String fileId : fileIds) {
            FileMetadata metadata = client.beta().files().retrieveMetadata(fileId);
            Path destination = OUTPUT_DIR.resolve(metadata.filename());

            try (HttpResponse httpResponse = client.beta().files().download(fileId)) {
                Files.copy(httpResponse.body(), destination, StandardCopyOption.REPLACE_EXISTING);
            }

            System.out.println("Fichier telecharge : " + destination);
        }
    }
}
