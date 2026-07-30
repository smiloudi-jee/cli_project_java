package com.formation.claudeapi.features.image;

import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.MessageParam;
import com.formation.claudeapi.AbstractClaudeConversation;

import java.util.ArrayList;
import java.util.List;

/**
 * Demo "Image support"
 * <p>
 * Un message utilisateur peut contenir un bloc "image" et un bloc "text" côte
 * à côte (l'image en premier, par convention). Claude répond avec un bloc
 * "text" contenant son analyse : le flux de conversation est identique à une
 * conversation 100% textuelle.
 * <p>
 * Deux façons d'envoyer une image sont illustrées ici :
 * <ul>
 *   <li>Encodée en base64 (fichier local)</li>
 *   <li>Via une URL publique</li>
 * </ul>
 */
public class ImageBasicsDemo extends AbstractClaudeConversation {

    private static final String IMAGE_RESOURCE = "/image/prop1.png";

    private static final String PUBLIC_IMAGE_URL =
            "https://fr.wikipedia.org/wiki/Moteur_M_103_Mercedes-Benz#/media/Fichier:M103_R107.jpg";

    public static void main(String[] args) {
        System.out.println("=== Image locale encodée en base64 ===");
        System.out.println(describeLocalImage());

        System.out.println();
        System.out.println("=== Image distante via une URL ===");
        System.out.println(describeImageFromUrl());
    }

    private static String describeLocalImage() {
        List<ContentBlockParam> content = List.of(
                ImageBlocks.imageFromClasspath(ImageBasicsDemo.class, IMAGE_RESOURCE),
                text("Que vois-tu sur cette image ? Decris la propriete en 3 phrases maximum.")
        );

        return askWithContent(content);
    }

    private static String describeImageFromUrl() {
        List<ContentBlockParam> content = List.of(
                ImageBlocks.imageFromUrl(PUBLIC_IMAGE_URL),
                text("Que vois-tu sur cette image ? Reponds en une phrase.")
        );

        return askWithContent(content);
    }

    private static String askWithContent(List<ContentBlockParam> content) {
        List<MessageParam> messages = new ArrayList<>();
        addUserMessage(messages, content);
        return chat(messages, null, null, null, null);
    }
}
