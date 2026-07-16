package com.formation.mcpchat;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Équivalent de mcp_server.py : expose un petit ensemble de "documents" en mémoire via le protocole MCP (transport stdio).
 *
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

    public static void main(String[] args) {
        StdioServerTransportProvider transportProvider =
                new StdioServerTransportProvider(McpJsonDefaults.getMapper());

        McpSyncServer server = McpServer.sync(transportProvider)
                .serverInfo(SERVER_NAME, SERVER_VERSION)
                .build();

        // TODO: Écrire une routine pour lire un document
        // TODO: Écrire une routine pour modifier un document
        // TODO: Écrire une routine pour renvoyer tous les identifiants des documents
        // TODO: Écrire une routine pour renvoyer le contenu d'un document spécifique
        // TODO: Écrire un prompt pour réécrire un document au format Markdown
        // TODO: Écrire un prompt pour résumer un document

        // Permet de maintenir le process MCP Serveur et ainsi de communiquer via stdin/stdout jusqu'à ce que
        // le processus client ferme la connexion.
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
    }
}
