package com.formation.claudeapi.tool.existing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implémentation locale du tool intégré "text editor".
 * <p>
 * Particularité de ce tool par rapport aux tools "maison" du reste du chapitre :
 * son schéma est fourni par Claude lui-même, on n'écrit pas de schema.
 * <p>
 * En revanche, toute l'exécution reste à notre charge : Claude ne fait qu'émettre des commandes
 * (view / replace / create / insert), charge à nous de les exécuter réellement sur de vrais fichiers.
 */
public class TextEditorTool {

    private static final Path WORKSPACE_ROOT =
            Path.of("src/main/java/com/formation/claudeapi/tool/existing/greeting").toAbsolutePath().normalize();

    /**
     * Dispatch la commande demandée par Claude vers l'opération correspondante, et
     * renvoie le texte à renvoyer dans le tool_result.
     */
    @SuppressWarnings("unchecked")
    public static String handle(Map<String, Object> input) {
        String command = (String) input.get("command");
        if (command == null) {
            throw new IllegalArgumentException("Missing required field: command");
        }

        return switch (command) {
            case "view" -> view((String) input.get("path"), (List<Object>) input.get("view_range"));
            case "str_replace" -> strReplace(
                    (String) input.get("path"),
                    (String) input.get("old_str"),
                    (String) input.get("new_str"));
            case "create" -> create((String) input.get("path"), (String) input.get("file_text"));
            case "insert" -> insert(
                    (String) input.get("path"),
                    ((Number) input.get("insert_line")).intValue(),
                    (String) input.get("insert_text"));
            default -> throw new IllegalArgumentException("Unknown command: " + command
                    + " (undo_edit n'existe plus dans text_editor_20250429)");
        };
    }

    /** Liste un répertoire, ou renvoie le contenu d'un fichier avec numéros de ligne ("N: ..."). */
    private static String view(String pathStr, List<Object> viewRange) {
        Path path = resolve(pathStr);

        if (Files.isDirectory(path)) {
            try (Stream<Path> entries = Files.list(path)) {
                return entries.map(p -> WORKSPACE_ROOT.relativize(p).toString())
                        .sorted()
                        .collect(Collectors.joining("\n"));
            } catch (IOException e) {
                throw new RuntimeException("Failed to list directory: " + pathStr, e);
            }
        }

        List<String> lines = readLines(path);

        int start = 1;
        int end = lines.size();
        if (viewRange != null && viewRange.size() == 2) {
            start = ((Number) viewRange.get(0)).intValue();
            int rawEnd = ((Number) viewRange.get(1)).intValue();
            end = rawEnd == -1 ? lines.size() : rawEnd;
        }

        StringBuilder result = new StringBuilder();
        for (int i = start; i <= end && i <= lines.size(); i++) {
            result.append(i).append(": ").append(lines.get(i - 1)).append('\n');
        }
        return result.toString();
    }

    /** Remplace old_str par new_str — doit matcher exactement une seule fois (best practice du cours). */
    private static String strReplace(String pathStr, String oldStr, String newStr) {
        if (oldStr == null || oldStr.isEmpty()) {
            throw new IllegalArgumentException("old_str cannot be empty");
        }

        Path path = resolve(pathStr);
        String content = readString(path);

        int firstIndex = content.indexOf(oldStr);
        if (firstIndex == -1) {
            throw new IllegalArgumentException("No match found for replacement in " + pathStr);
        }
        int lastIndex = content.lastIndexOf(oldStr);
        if (firstIndex != lastIndex) {
            throw new IllegalArgumentException(
                    "Multiple matches found for old_str in " + pathStr + " — old_str must be unique");
        }

        String updated = content.substring(0, firstIndex) + newStr + content.substring(firstIndex + oldStr.length());
        writeString(path, updated);

        return "Successfully replaced text at exactly one location.";
    }

    private static String create(String pathStr, String fileText) {
        Path path = resolve(pathStr);
        writeString(path, fileText == null ? "" : fileText);
        return "Successfully created file " + pathStr;
    }

    private static String insert(String pathStr, int insertLine, String insertText) {
        Path path = resolve(pathStr);
        List<String> lines = readLines(path);

        if (insertLine < 0 || insertLine > lines.size()) {
            throw new IllegalArgumentException(
                    "insert_line out of range: " + insertLine + " (file has " + lines.size() + " lines)");
        }

        lines.add(insertLine, insertText);
        writeString(path, String.join("\n", lines));

        return "Successfully inserted text after line " + insertLine;
    }

    // ── Sécurité + I/O ──

    private static Path resolve(String pathStr) {
        if (pathStr == null || pathStr.isBlank()) {
            throw new IllegalArgumentException("path cannot be empty");
        }

        Path resolved = WORKSPACE_ROOT.resolve(pathStr).normalize();
        if (!resolved.startsWith(WORKSPACE_ROOT)) {
            throw new IllegalArgumentException("Path escapes the sandboxed workspace: " + pathStr);
        }
        return resolved;
    }

    private static List<String> readLines(Path path) {
        try {
            return new ArrayList<>(Files.readAllLines(path));
        } catch (IOException e) {
            throw new RuntimeException("File not found or unreadable: " + WORKSPACE_ROOT.relativize(path), e);
        }
    }

    private static String readString(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new RuntimeException("File not found or unreadable: " + WORKSPACE_ROOT.relativize(path), e);
        }
    }

    private static void writeString(Path path, String content) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, content);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write file: " + WORKSPACE_ROOT.relativize(path), e);
        }
    }
}
