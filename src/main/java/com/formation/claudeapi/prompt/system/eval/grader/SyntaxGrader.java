package com.formation.claudeapi.prompt.system.eval.grader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formation.claudeapi.prompt.system.eval.pipeline.TestCase;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Grader "syntaxique" : note un output uniquement sur sa validité structurelle (validation du format uniquement),
 * <p>
 * <ul>
 *   <li>{@link ObjectMapper#readTree}</li>
 *   <li>{@link StaticJavaParser#parseMethodDeclaration}</li>
 *   <li>{@link Pattern#compile}</li>
 * </ul>
 * Le format "java" attend une méthode complète (signature incluse), pas un
 * fragment de statements : c'est ce que {@link StaticJavaParser#parseMethodDeclaration} sait valider.
 */
public class SyntaxGrader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Dispatch vers le validateur du format attendu par le test case. */
    public static int gradeSyntax(String response, TestCase testCase) {
        String format = testCase.format();

        if ("json".equals(format)) {
            return validateJson(response);
        } else if ("java".equals(format)) {
            return validateJava(response);
        } else {
            return validateRegex(response);
        }
    }

    static int validateJson(String text) {
        try {
            MAPPER.readTree(text.strip());
            return 10;
        } catch (JsonProcessingException e) {
            return 0;
        }
    }

    static int validateJava(String text) {
        try {
            StaticJavaParser.parseMethodDeclaration(text.strip());
            return 10;
        } catch (ParseProblemException e) {
            return 0;
        }
    }

    static int validateRegex(String text) {
        try {
            Pattern.compile(text.strip());
            return 10;
        } catch (PatternSyntaxException e) {
            return 0;
        }
    }
}
