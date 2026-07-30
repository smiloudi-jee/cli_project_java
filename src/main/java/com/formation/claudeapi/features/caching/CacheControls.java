package com.formation.claudeapi.features.caching;

import com.anthropic.models.messages.CacheControlEphemeral;
import com.anthropic.models.messages.TextBlockParam;
import com.anthropic.models.messages.Tool;

/**
 * Fonctions utilitaires pour activer le "prompt caching" (section de cours
 * "Prompt caching") : mise en cache cote serveur de portions de la requete
 * (system prompt, tools, documents...) qui ne changent pas d'un appel a
 * l'autre, pour eviter de les retraiter (et de les repayer au plein tarif) a
 * chaque fois.
 * <p>
 * Concretement, on ajoute un marqueur {@code cache_control: {type: "ephemeral"}}
 * sur le dernier bloc de contenu (ou le dernier tool) que l'on souhaite
 * mettre en cache : tout ce qui precede ce marqueur dans la requete est mis
 * en cache jusqu'a ce point.
 */
public final class CacheControls {

    private CacheControls() {
        // classe utilitaire, non instanciable
    }

    /**
     * Marqueur de cache "ephemeral" avec la duree de vie par defaut (5 minutes).
     */
    public static CacheControlEphemeral ephemeral() {
        return CacheControlEphemeral.builder().build();
    }

    /**
     * Marqueur de cache "ephemeral" avec une duree de vie etendue (1 heure),
     * utile quand le delai entre deux appels depasse les 5 minutes par defaut.
     */
    public static CacheControlEphemeral ephemeralOneHour() {
        return CacheControlEphemeral.builder()
                .ttl(CacheControlEphemeral.Ttl.TTL_1H)
                .build();
    }

    /**
     * Construit un bloc "text" avec cache_control. Utile pour un system
     * prompt volumineux : c'est le seul moyen d'y attacher un cache_control,
     * puisqu'il faut alors le passer sous forme de liste de blocs (via
     * {@code systemOfTextBlockParams}) plutot que comme une simple chaine.
     */
    public static TextBlockParam cachedSystemBlock(String text) {
        return TextBlockParam.builder()
                .text(text)
                .cacheControl(ephemeral())
                .build();
    }

    /**
     * Marque un tool comme point de cache. Attention : cache_control ne se
     * pose pas sur chaque tool individuellement, mais uniquement sur le
     * DERNIER tool de la liste que l'on veut mettre en cache - Claude met
     * alors en cache l'integralite des schemas de tools qui le precedent
     * (lui inclus).
     */
    public static Tool cached(Tool tool) {
        return tool.toBuilder()
                .cacheControl(ephemeral())
                .build();
    }
}
