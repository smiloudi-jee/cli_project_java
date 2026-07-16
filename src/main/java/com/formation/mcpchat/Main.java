package com.formation.mcpchat;

import com.formation.mcpchat.core.Cli;
import com.formation.mcpchat.core.CliChat;
import com.formation.mcpchat.core.Claude;

import io.github.cdimascio.dotenv.Dotenv;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * L'équivalent du fichier principal (main.py), il permet de démarrer :
 * * Le client Anthropic (l'IA Claude),
 * * Le serveur MCP qui est le service de documents intégré,
 * * Et l'interface en ligne de commande interactive (CLI).
 */
public class Main {

    public static void main(String[] args) throws Exception {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        String claudeModel = env(dotenv, "CLAUDE_MODEL");
        String anthropicApiKey = env(dotenv, "ANTHROPIC_API_KEY");

        if (claudeModel.isBlank()) {
            throw new IllegalStateException("Error: CLAUDE_MODEL cannot be empty. Update .env");
        }
        if (anthropicApiKey.isBlank()) {
            throw new IllegalStateException("Error: ANTHROPIC_API_KEY cannot be empty. Update .env");
        }

        if (System.getenv("ANTHROPIC_API_KEY") == null) {
            System.setProperty("anthropic.apiKey", anthropicApiKey);
        }

        Claude claudeService = new Claude(claudeModel);

        // Relance la JVM/le classpath actuel(le) pour démarrer le serveur de documents
        String javaBin = System.getProperty("java.home") + "/bin/java";
        String classpath = subprocessClasspath();

        Map<String, MCPClient> clients = new LinkedHashMap<>();

        /*
         * Démarre le serveur MCP principal (service documents) en sous-processus, avec la possibilité d'ajouter des
         * serveurs MCP additionnels passés en argument (mvn exec:java -Dexec.args="...").
         *
         * Tous les clients sont stockés dans `clients` et branchés à CliChat, qui pilote la boucle de chat interactive (Cli).
         *
         * Le `finally` garantit la fermeture propre de tous les sous-processus MCP à la sortie, meme en cas d'erreur.
         */
        try {
            MCPClient docClient = new MCPClient(javaBin, List.of("-cp", classpath, "com.formation.mcpchat.McpServerApp"));
            docClient.connect();
            clients.put("doc_client", docClient);

            for (int i = 0; i < args.length; i++) {
                String[] parts = args[i].trim().split("\\s+");
                String command = parts[0];
                List<String> extraArgs = Arrays.asList(parts).subList(1, parts.length);

                MCPClient client = new MCPClient(command, extraArgs);
                client.connect();
                clients.put("client_" + i + "_" + args[i], client);
            }

            CliChat chat = new CliChat(docClient, clients, claudeService);

            Cli cli = new Cli(chat);
            cli.initialize();
            cli.run();
        } finally {
            for (MCPClient client : clients.values()) {
                client.cleanup();
            }
        }
    }

    /*
     * Construit le classpath pour le sous-processus du serveur MCP.
     *
     * * Avec "mvn exec:java", la propriété "java.class.path" ne contient pas les dépendances
     * * du projet (MCP/Anthropic SDK), car Maven les isole dans un ClassLoader à part.
     * * Lancer le serveur avec cette valeur provoquerait un échec silencieux (timeout).
     *
     * * On crée le fichier "target/classpath.txt" (généré à la compilation par le plugin
     * * Maven configuré dans le pom.xml) et on y ajoute "target/classes".
     *
     * * En cas de lancement classique via "java -cp ...", on réutilise simplement la
     * * propriété "java.class.path" standard.
     */
    private static String subprocessClasspath() {
        Path classpathFile = Path.of("target", "classpath.txt");
        Path classesDir = Path.of("target", "classes");
        if (Files.isRegularFile(classpathFile)) {
            try {
                String deps = Files.readString(classpathFile).strip();
                return classesDir.toAbsolutePath() + File.pathSeparator + deps;
            } catch (IOException e) {
                // Fall through to the system property below.
            }
        }
        return System.getProperty("java.class.path");
    }

    private static String env(Dotenv dotenv, String key) {
        String fromEnv = System.getenv(key);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        String fromDotenv = dotenv.get(key);
        return (fromDotenv != null && !fromDotenv.isBlank()) ? fromDotenv : "";
    }
}