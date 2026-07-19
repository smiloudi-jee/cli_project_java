package com.formation.mcpchat.core;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.reader.Reference;
import org.jline.reader.UserInterruptException;
import org.jline.reader.Widget;
import org.jline.terminal.TerminalBuilder;

import io.modelcontextprotocol.spec.McpSchema;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Equivalent de Cli.py.
 * <p>
 * Gere l'interface en ligne de commande (REPL) :
 * * Lit les messages tapes par l'utilisateur,
 * * Gere l'autocomplétion Tab pour les "/commandes" et les mentions "@document" (via JLine),
 * * Et transmet chaque message au chat pour obtenir une réponse.
 */
public class Cli {

    private final CliChat agent;
    private final LineReader reader;
    private final UnifiedCompleter completer;

    public Cli(CliChat agent) throws IOException {
        this.agent = agent;
        this.completer = new UnifiedCompleter();

        this.reader = LineReaderBuilder.builder()
                .terminal(TerminalBuilder.builder().system(true).build())
                .completer(completer)
                .option(LineReader.Option.AUTO_LIST, true)
                .option(LineReader.Option.AUTO_MENU, true)
                .build();

        // Affiche automatiquement le menu d'autocomplétion juste apres avoir tapé "/"
        // ou "@", reproduisant les raccourcis clavier prompt_toolkit de la CLI d'origine.
        bindAutoTriggerKey('/');
        bindAutoTriggerKey('@');
    }

    private void bindAutoTriggerKey(char key) {
        String widgetName = "auto-trigger-" + (int) key;
        Widget widget = () -> {
            reader.getBuffer().write(key);
            reader.callWidget(LineReader.REDISPLAY);
            reader.callWidget(LineReader.MENU_COMPLETE);
            return true;
        };
        reader.getWidgets().put(widgetName, widget);
        reader.getKeyMaps().get(LineReader.MAIN).bind(new Reference(widgetName), String.valueOf(key));
    }

    public void initialize() {
        refreshResources();
        refreshPrompts();
    }

    private void refreshResources() {
        try {
            completer.updateResources(agent.listDocIds());
        } catch (Exception e) {
            System.out.println("Error refreshing resources: " + e.getMessage());
        }
    }

    private void refreshPrompts() {
        try {
            completer.updatePrompts(agent.listPrompts());
        } catch (Exception e) {
            System.out.println("Error refreshing prompts: " + e.getMessage());
        }
    }

    public void run() {
        while (true) {
            String userInput;
            try {
                userInput = reader.readLine("> ");
            } catch (UserInterruptException | EndOfFileException e) {
                break;
            }

            if (userInput == null || userInput.isBlank()) {
                continue;
            }

            // Une exception ici (commande /xxx inconnue, erreur MCP, appel API en échec, etc.)
            // ne doit pas faire planter toute la session : on l'affiche et on reprend la boucle.
            try {
                String response = agent.run(userInput);
                System.out.println("\nResponse:\n" + response);
            } catch (Exception e) {
                System.out.println("\nErreur : " + e.getMessage());
            }
        }
    }

    /** Autocompletion pour les noms de "/commande" et les mentions "@document". */
    private static class UnifiedCompleter implements Completer {

        private List<McpSchema.Prompt> prompts = new ArrayList<>();
        private List<String> resources = new ArrayList<>();

        void updatePrompts(List<McpSchema.Prompt> prompts) {
            this.prompts = prompts;
        }

        void updateResources(List<String> resources) {
            this.resources = resources;
        }

        @Override
        public void complete(LineReader lineReader, ParsedLine parsedLine, List<Candidate> candidates) {
            String word = parsedLine.word();

            if (word.startsWith("@")) {
                String prefix = word.substring(1).toLowerCase(Locale.ROOT);
                for (String resourceId : resources) {
                    if (resourceId.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                        candidates.add(new Candidate(
                                "@" + resourceId, resourceId, "Resource", null, null, null, true));
                    }
                }
                return;
            }

            List<String> words = parsedLine.words();
            int wordIndex = parsedLine.wordIndex();

            if (words.isEmpty() || !words.getFirst().startsWith("/")) {
                return;
            }

            if (wordIndex == 0) {
                String cmdPrefix = word.startsWith("/") ? word.substring(1) : word;
                for (McpSchema.Prompt prompt : prompts) {
                    if (prompt.name().startsWith(cmdPrefix)) {
                        candidates.add(new Candidate(
                                "/" + prompt.name(), prompt.name(), null, prompt.description(), null, null, true));
                    }
                }
            } else {
                String docPrefix = word.toLowerCase(Locale.ROOT);
                for (String resourceId : resources) {
                    if (resourceId.toLowerCase(Locale.ROOT).startsWith(docPrefix)) {
                        candidates.add(new Candidate(resourceId, resourceId, null, null, null, null, true));
                    }
                }
            }
        }
    }
}
