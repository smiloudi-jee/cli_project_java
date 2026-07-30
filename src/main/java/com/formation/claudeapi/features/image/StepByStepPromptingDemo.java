package com.formation.claudeapi.features.image;

import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.MessageParam;
import com.formation.claudeapi.AbstractClaudeConversation;

import java.util.ArrayList;
import java.util.List;

/**
 * Demo "Prompting Techniques" / "Step-by-Step Analysis".
 * <p>
 * Comptage de billes : Combien de billes ? donne souvent un faux résultat,
 * Une méthodologie explicite améliore nettement la precision.
 */
public class StepByStepPromptingDemo extends AbstractClaudeConversation {

    private static final String IMAGE_RESOURCE = "/image/prop3.png";

    private static final String NAIVE_PROMPT =
            "Combien d'arbres vois-tu sur cette image ?";

    private static final String STRUCTURED_PROMPT = """
            Analyse cette image satellite et determine le nombre exact d'arbres visibles \
            en suivant cette méthodologie :
            
            1. Identifie chaque arbre un par un, en lui attribuant un numero au fur et \
            a mesure que tu le repères.
            
            2. Vérifie ton résultat avec une seconde methode : repars du coin en bas a \
            gauche de l'image et compte ligne par ligne, de gauche a droite.

            Quel est le nombre exact et vérifie d'arbres sur cette image ?""";

    public static void main(String[] args) {
        System.out.println("=== Prompt naif ===");
        System.out.println(askAboutImage(NAIVE_PROMPT));

        System.out.println();
        System.out.println("=== Prompt avec methodologie pas-a-pas ===");
        System.out.println(askAboutImage(STRUCTURED_PROMPT));
    }

    private static String askAboutImage(String prompt) {
        List<ContentBlockParam> content = List.of(
                ImageBlocks.imageFromClasspath(StepByStepPromptingDemo.class, IMAGE_RESOURCE),
                text(prompt)
        );

        List<MessageParam> messages = new ArrayList<>();
        addUserMessage(messages, content);
        return chat(messages, null, null, null, null);
    }
}
