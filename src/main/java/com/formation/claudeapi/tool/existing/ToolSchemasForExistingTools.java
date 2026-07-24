package com.formation.claudeapi.tool.existing;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.ToolTextEditor20250728;
import com.anthropic.models.messages.ToolUnion;
import com.anthropic.models.messages.WebSearchTool20250305;

import java.util.List;

/**
 * Déclarations des deux tools intégrés à Claude.
 * Pas de schema à écrire juste un "stub" côté requête, le vrai schéma est déjà connu de Claude.
 */
public class ToolSchemasForExistingTools {

    /**
     * Le vrai schéma (les paramètres de chaque commande) est déjà connu de Claude,
     * on ne fournit que le type de version + le nom attendu par ce type.
     * <p>
     * {@code text_editor_20250429} (utilisé initialement) n'est pas supporté par
     * {@code claude-sonnet-4-5-20250929} — erreur 400 confirmée à l'exécution. La doc à jour
     * indique {@code text_editor_20250728} comme version courante pour les modèles Claude 4,
     * avec le même nom d'outil {@code str_replace_based_edit_tool} (type et name vont ensemble).
     */
    public static final ToolUnion TEXT_EDITOR_TOOL = ToolUnion.ofTextEditor20250728(
            ToolTextEditor20250728.builder()
                    .name(JsonValue.from("str_replace_based_edit_tool"))
                    .build());

    /**
     * Web search tool : Entièrement géré côté Anthropic (recherche + lecture + des résultats).
     * On fixe le nombre max de recherches {@code maxUses}.
     * On restreint aux sources fiables {@code allowedDomains}.
     */
    public static final ToolUnion WEB_SEARCH_TOOL = ToolUnion.ofWebSearchTool20250305(
            WebSearchTool20250305.builder()
                    .name(JsonValue.from("web_search"))
                    .maxUses(5)
                    .allowedDomains(List.of("nih.gov"))
                    .build());
}
