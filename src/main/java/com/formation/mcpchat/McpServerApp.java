package com.formation.mcpchat;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncPromptSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceTemplateSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.Prompt;
import io.modelcontextprotocol.spec.McpSchema.PromptArgument;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.Resource;
import io.modelcontextprotocol.spec.McpSchema.ResourceTemplate;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import io.modelcontextprotocol.spec.McpSchema.Tool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Équivalent de mcp_server.py : expose un petit ensemble de "documents" en mémoire via le protocole MCP (transport stdio).
 * <p>
 * Les outils, ressources et prompts sont laissés en TODO, à implémenter par l'étudiant.
 */
public class McpServerApp {

    private static final String SERVER_NAME = "DocumentMCP";
    private static final String SERVER_VERSION = "1.0.0";

    static final Map<String, String> DOCS = new LinkedHashMap<>();

    static {
        DOCS.put("deposition.md", "Cette déposition couvre le témoignage d'Angela Smith, P.E.");
        DOCS.put("report.pdf", "Le rapport détaille l'état d'une tour de condensation de 20m.");
        DOCS.put("financials.docx", "Ces états financiers décrivent le budget et les dépenses du projet.");
        DOCS.put("outlook.pdf", "Ce document présente les performances futures projetées du système.");
        DOCS.put("plan.md", "Le plan décrit les étapes de la mise en œuvre du projet.");
        DOCS.put("spec.txt", "Ces spécifications définissent les exigences techniques de l'équipement.");
    }

    // Point d'entrée du serveur MCP (transport stdio).
    // Peut aussi être lancé via MCP Inspector pour tester tools/resources/prompts
    // isolément : npx @modelcontextprotocol/inspector java -jar target/mcp-chat-server.jar
    public static void main(String[] args) {
        StdioServerTransportProvider transportProvider =
            new StdioServerTransportProvider(McpJsonDefaults.getMapper());

        McpSyncServer server = McpServer.sync(transportProvider)
            .serverInfo(SERVER_NAME, SERVER_VERSION)
            .capabilities(
                McpSchema.ServerCapabilities.builder()
                    .tools(true)
                    .resources(false, true)
                    .prompts(true)
                    .build())
            .build();

        // Tool pour lire un document
        buildToolReadDocument(server);
        // Tool pour modifier un document
        buildToolModifyDocument(server);

        // Resource : renvoie tous les identifiants de documents disponibles
        buildResourceListDocIds(server);
        // Resource (template) : renvoie le contenu d'un document spécifique
        buildResourceReadDocContent(server);

        // Prompt : réécrit un document au format Markdown
        buildPromptFormat(server);
        // Prompt : résume un document
        buildPromptSummarize(server);

        // Permet de maintenir le process MCP Serveur et ainsi de communiquer via stdin/stdout jusqu'à ce que
        // le processus client ferme la connexion.
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
    }

    private static Map<String, Object> buildDocSchemaParameter() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "doc_id", Map.of(
                    "type", "string",
                    "description", "Id du document a modifier"),
                "new_content", Map.of(
                    "type", "string",
                    "description", "Nouveau contenu du document")),
            "required", List.of("doc_id"));
    }

    //---------------------------------------------------------------------
    //      Tools : contrôlés par le modèle ("model controlled") :
    //      C'est Claude qui décide, pendant son raisonnement,
    //      s'il a besoin d'appeler tel tool.
    //---------------------------------------------------------------------

    // Tool : lit le contenu d'un document à partir de son identifiant.
    private static void buildToolReadDocument(McpSyncServer server){
        Map<String, Object> readDocSchema = buildDocSchemaParameter();

        SyncToolSpecification readDocTool = SyncToolSpecification.builder()
            .tool(Tool.builder("read_doc_contents", readDocSchema)
                .description("Lit le contenu d'un document et le renvoie sous forme de chaine de caractères.")
                .build())
            .callHandler((exchange, request) -> {
                String docId = (String) request.arguments().get("doc_id");
                if (!DOCS.containsKey(docId)) {
                    return CallToolResult.builder()
                        .content(List.of(TextContent.builder("Document introuvable : " + docId).build()))
                        .isError(true)
                        .build();
                }
                return CallToolResult.builder()
                        .content(List.of(TextContent.builder(DOCS.get(docId)).build()))
                        .build();
            })
            .build();
        server.addTool(readDocTool);
    }
    // Tool : modifie le contenu d'un document à partir de son identifiant.
    private static void buildToolModifyDocument(McpSyncServer server){
        Map<String, Object> modifyDocSchema = buildDocSchemaParameter();

        SyncToolSpecification modifyDocTool = SyncToolSpecification.builder()
            .tool(Tool.builder("modify_doc_contents", modifyDocSchema)
                .description("Modifie le contenu d'un document et renvoie un message de confirmation.")
                .build())
            .callHandler((exchange, request) -> {
                String docId = (String) request.arguments().get("doc_id");
                String newContent = (String) request.arguments().get("new_content");
                if (!DOCS.containsKey(docId)) {
                    return CallToolResult.builder()
                        .content(List.of(TextContent.builder("Document introuvable : " + docId).build()))
                        .isError(true)
                        .build();
                }
                DOCS.put(docId, newContent);
                return CallToolResult.builder()
                    .content(List.of(TextContent.builder("Document modifié avec succès : " + docId).build()))
                    .build();
            }).build();
        server.addTool(modifyDocTool);
    }

    //---------------------------------------------------------------------
    //      Resources = contrôlées par l'application cliente ("application controlled") :
    //      C'est le code du client qui décide quand et comment aller chercher une resource
    //      Ici, CliChat la résout avant même d'envoyer le message à Claude.
    //      Dans notre exercice, le client implémente l'utilisation de @
    //---------------------------------------------------------------------

    // Resource : renvoie la liste de tous les identifiants de documents disponibles (URI fixe docs://documents).
    // Une resource concrete a un URI fixe, connue à l'avance et enumerable via resources/list.
    private static void buildResourceListDocIds(McpSyncServer server) {
        Resource resource = Resource.builder("docs://documents", "Liste des documents")
            .description("Renvoie la liste de tous les identifiants de documents disponibles, au format JSON.")
            .mimeType("application/json")
            .build();

        SyncResourceSpecification listDocIdsResource =
            new SyncResourceSpecification(resource,
                (exchange, request) -> {
                    String json;
                    try {
                        json = McpJsonDefaults.getMapper().writeValueAsString(new ArrayList<>(DOCS.keySet()));
                    } catch (IOException e) {
                        throw new RuntimeException("Erreur de sérialisation JSON des identifiants de documents", e);
                    }
                    return ReadResourceResult.builder(
                        List.of(
                            TextResourceContents.builder("docs://documents", json)
                                .mimeType("application/json")
                                .build()
                        )
                    ).build();
                }
            );
        server.addResource(listDocIdsResource);
    }
    // Resource template : renvoie le contenu d'un document spécifique (URI paramétré docs://documents/{doc_id}).
    // Un resource template sert un autre cas : une famille d'URIs dont on ne peut pas lister toutes les instances à l'avance.
    // Le serveur expose juste le patron, et c'est au client de construire l'URI éxacte avec l'id qu'il connait deja.
    private static void buildResourceReadDocContent(McpSyncServer server) {
        ResourceTemplate template =
            ResourceTemplate.builder("docs://documents/{doc_id}", "Contenu d'un document")
                .description("Renvoie le contenu d'un document spécifique, identifié par son doc_id.")
                .mimeType("text/plain")
                .build();

        SyncResourceTemplateSpecification readDocContentResource =
            new SyncResourceTemplateSpecification(template,
                (exchange, request) -> {
                    String uri = request.uri();
                    String prefix = "docs://documents/";
                    String docId = uri.startsWith(prefix) ? uri.substring(prefix.length()) : uri;

                    if (!DOCS.containsKey(docId)) {
                        throw new McpError(
                            new McpSchema.JSONRPCResponse.JSONRPCError(
                                McpSchema.ErrorCodes.RESOURCE_NOT_FOUND, "Document introuvable : " + docId));
                    }

                    return ReadResourceResult.builder(
                        List.of(
                            TextResourceContents.builder(uri, DOCS.get(docId))
                                .mimeType("text/plain")
                                .build()
                        )
                    ).build();
                }
            );
        server.addResourceTemplate(readDocContentResource);
    }

    //---------------------------------------------------------------------
    //       Prompts = contrôles par l'utilisateur directement via CLI ("user controlled"),
    //       explicitement déclenchés par l'utilisateur (nos commandes /format et /summarize),
    //       ni par le modèle ni par le code client.
    //---------------------------------------------------------------------

    // Prompt : demande à Claude de réécrire un document au format Markdown.
    private static void buildPromptFormat(McpSyncServer server) {
        Prompt prompt = Prompt.builder("format")
            .description("Réécrit le contenu d'un document au format Markdown.")
            .arguments(
                List.of(
                    PromptArgument.builder("doc_id")
                        .description("Id du document a reformater")
                        .required(true)
                        .build()
                )
            ).build();

        SyncPromptSpecification formatPrompt = new SyncPromptSpecification(prompt,
            (exchange, request) -> {
                String docId = request.arguments().get("doc_id").toString();

                String text = """
                    Ta tâche est de reformater un document pour qu'il soit écrit au format Markdown.

                    L'id du document à reformater est :
                    <document_id>
                    %s
                    </document_id>

                    Ajoute des titres, des listes à puces, des tableaux, etc. si nécessaire pour rendre
                    le document plus lisible.

                    Ajoute les détails que tu juges pertinents pour un lecteur de ce document.

                    Utilise le tool 'read_doc_contents' pour récupérer le contenu du document avant de le
                    réécrire.

                    Une fois le document reformaté, utilise le tool 'modify_doc_contents' pour remplacer
                    l'ancien contenu par le nouveau contenu Markdown.
                    """.formatted(docId);

                PromptMessage message = new PromptMessage(Role.USER, TextContent.builder(text).build());
                return GetPromptResult.builder(List.of(message))
                    .description("Prompt de reformatage Markdown pour " + docId)
                    .build();
            });
        server.addPrompt(formatPrompt);
    }
    // Prompt : demande a Claude de resumer un document.
    private static void buildPromptSummarize(McpSyncServer server) {
        Prompt prompt = Prompt.builder("summarize")
            .description("Résume le contenu d'un document.")
            .arguments(
                List.of(
                    PromptArgument.builder("doc_id")
                        .description("Id du document a résumer")
                        .required(true)
                        .build()
                )).build();

        SyncPromptSpecification summarizePrompt = new SyncPromptSpecification(prompt,
            (exchange, request) -> {
                String docId = request.arguments().get("doc_id").toString();

                String text = """
                    Ta tâche est de résumer le contenu du document dont l'id est : %s

                    Pour cela, tu dois d'abord lire le contenu du document avec le tool 'read_doc_contents'.

                    Ensuite, génère un résumé du contenu du document.

                    Le résumé doit comporter un court paragraphe reprenant les points clés.
                    """.formatted(docId);

                PromptMessage message = new PromptMessage(Role.USER, TextContent.builder(text).build());
                return GetPromptResult.builder(List.of(message))
                    .description("Prompt de résumé pour " + docId)
                    .build();
            });
        server.addPrompt(summarizePrompt);
    }

}
