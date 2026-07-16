package com.formation.mcpchat;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.Prompt;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;

import java.util.Collections;
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
        // TODO: Return a list of tools defined by the MCP server
        return Collections.emptyList();
    }

    public CallToolResult callTool(String toolName, Map<String, Object> toolInput) {
        // TODO: Call a particular tool and return the result
        return null;
    }

    public List<Prompt> listPrompts() {
        // TODO: Return a list of prompts defined by the MCP server
        return Collections.emptyList();
    }

    public GetPromptResult getPrompt(String promptName, Map<String, String> args) {
        // TODO: Get a particular prompt defined by the MCP server
        return null;
    }

    public ReadResourceResult readResource(String uri) {
        // TODO: Read a resource, parse the contents and return it
        return ReadResourceResult.builder(Collections.emptyList()).build();
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

    // For testing
    public static void main(String[] args) {
        try (MCPClient testClient = buildMCPClient()) {
            testClient.connect();
        }
    }

    private static MCPClient buildMCPClient(){
        return new MCPClient(JAVA_HOME, List.of("-cp", CLASS_PATH, MCP_SERVER));
    }
}
