package com.formation.mcpchat;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.Prompt;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;

import java.util.List;
import java.util.Map;

/**
 * Wrapper pour le client SDK Java MCP, équivalent à la classe Python MCPClient.
 * Les opérations MCP réelles (outils, prompts, ressources) restent à implémenter
 * (TODO), comme dans la version Python.
 */
public class MCPClient implements AutoCloseable {
    private static final String JAVA_HOME = System.getProperty("java.home") + "/bin/java";
    private static final String CLASS_PATH = System.getProperty("java.class.path") ;
    private static final String MCP_SERVER = "com.formation.mcpchat.McpServerApp";

    private final String command;
    private final List<String> args;
    private final Map<String, String> env;
    private McpSyncClient client;

    public MCPClient(String command, List<String> args, Map<String, String> env) {
        this.command = command;
        this.args = args;
        this.env = env;
    }

    public MCPClient(String command, List<String> args) {
        this(command, args, null);
    }

    public void connect() {
        ServerParameters.Builder paramsBuilder = ServerParameters.builder(command).args(args);
        if (env != null) {
            paramsBuilder.env(env);
        }
        ServerParameters serverParameters = paramsBuilder.build();

        StdioClientTransport transport =
                new StdioClientTransport(serverParameters, McpJsonDefaults.getMapper());

        this.client = McpClient.sync(transport).build();
        this.client.initialize();
    }

    public McpSyncClient session() {
        if (client == null) {
            throw new IllegalStateException(
                    "Client session not initialized. Call connect() first.");
        }
        return client;
    }

    public List<Tool> listTools() {
        return session().listTools().tools();
    }

    public CallToolResult callTool(String toolName, Map<String, Object> toolInput) {
        return session().callTool(
                McpSchema.CallToolRequest.builder(toolName).arguments(toolInput).build());
    }

    public List<Prompt> listPrompts() {
        return session().listPrompts().prompts();
    }

    public GetPromptResult getPrompt(String promptName, Map<String, Object> args) {
        return session().getPrompt(
                McpSchema.GetPromptRequest.builder(promptName).arguments(args).build());
    }

    public List<McpSchema.Resource> listResources() {
        return session().listResources().resources();
    }

    public List<McpSchema.ResourceTemplate> listResourceTemplates() {
        return session().listResourceTemplates().resourceTemplates();
    }

    public ReadResourceResult readResource(String uri) {
        return session().readResource(McpSchema.ReadResourceRequest.builder(uri).build());
    }

    public void cleanup() {
        if (client != null) {
            client.closeGracefully();
            client = null;
        }
    }

    @Override
    public void close() {
        cleanup();
    }

    // Pour tester manuellement la connexion et les opérations MCP (tools, resources, prompts)
    // sans passer par l'application complète, nous pouvons lancer ce 'main' directement depuis l'IDE.
    public static void main(String[] args) {
        try (MCPClient testClient = buildMCPClient()) {
            testClient.connect();

            List<Tool> tools = testClient.listTools();
            System.out.println("Tools exposés par le serveur (" + tools.size() + ") :");
            for (Tool tool : tools) {
                System.out.println(" - " + tool.name() + " : " + tool.description());
            }

            List<McpSchema.Resource> resources = testClient.listResources();
            System.out.println("Resource concrète : " + resources.size());
            for (McpSchema.Resource content : resources) {
                System.out.println(" - " + content);
            }

            List<McpSchema.ResourceTemplate> resourceTemplates = testClient.listResourceTemplates();
            System.out.println("Resource templates : " + resourceTemplates.size());
            for (McpSchema.ResourceTemplate template : resourceTemplates) {
                System.out.println(" - " + template);
            }

            List<Prompt> prompts = testClient.listPrompts();
            System.out.println("Prompts exposés par le serveur (" + prompts.size() + ") :");
            for (Prompt prompt : prompts) {
                System.out.println(" - " + prompt.name() + " : " + prompt.description());
            }
        }
    }

    private static MCPClient buildMCPClient(){
        return new MCPClient(JAVA_HOME, List.of("-cp", CLASS_PATH, MCP_SERVER));
    }
}
