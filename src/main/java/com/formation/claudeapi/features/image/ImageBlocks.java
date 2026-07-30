package com.formation.claudeapi.features.image;

import com.anthropic.models.messages.Base64ImageSource;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.ImageBlockParam;
import com.anthropic.models.messages.TextBlockParam;
import com.anthropic.models.messages.UrlImageSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Locale;

import static com.formation.claudeapi.AbstractClaudeConversation.readResourceBytes;

/**
 * Fonctions utilitaires pour construire les blocs de contenu "image" et "text"
 * <p>
 * Rappels des limites à garder en tete :
 * - jusqu'à 100 images au total dans une meme requête
 * - 5 Mo maximum par image
 * - 8000 px de large/haut max quand on envoie une seule image, 2000 px si plusieurs images dans le meme message
 * - le cout en tokens d'une image = (largeur px x hauteur px) / 750
 */
public final class ImageBlocks {

    private ImageBlocks() {
        // classe utilitaire, non instanciable
    }

    /**
     * Construit un bloc "image" encode en base64 à partir d'une ressource du
     * classpath, par exemple "/image/prop1.png".
     */
    public static ContentBlockParam imageFromClasspath(Class<?> anchor, String resourcePath) {
        byte[] bytes = readResourceBytes(anchor, resourcePath);
        String base64Data = Base64.getEncoder().encodeToString(bytes);
        Base64ImageSource.MediaType mediaType = mediaTypeFromFileName(resourcePath);

        return ContentBlockParam.ofImage(
            ImageBlockParam.builder()
                .source(Base64ImageSource.builder()
                    .mediaType(mediaType)
                    .data(base64Data)
                    .build())
                .build()
        );
    }

    /**
     * Construit un bloc "image" à partir d'une URL publique. Contrairement au
     * base64, Claude va lui-meme télécharger l'image : rien à lire/encoder
     * localement.
     */
    public static ContentBlockParam imageFromUrl(String url) {
        return ContentBlockParam.ofImage(
            ImageBlockParam.builder()
                .source(UrlImageSource.builder()
                    .url(url)
                    .build())
                .build()
        );
    }

    private static Base64ImageSource.MediaType mediaTypeFromFileName(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return Base64ImageSource.MediaType.IMAGE_PNG;
        } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return Base64ImageSource.MediaType.IMAGE_JPEG;
        } else if (lower.endsWith(".gif")) {
            return Base64ImageSource.MediaType.IMAGE_GIF;
        } else if (lower.endsWith(".webp")) {
            return Base64ImageSource.MediaType.IMAGE_WEBP;
        }
        throw new IllegalArgumentException("Extension d'image non geree : " + path);
    }
}
