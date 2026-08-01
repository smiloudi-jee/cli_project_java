package com.formation.claudeapi.agents.workflows.routing;

/**
 * Catégories de contenu prédéfinies utilisées par {@link RoutingDemo},
 * une par pipeline de génération spécialisé.
 */
public enum ContentCategory {

    EDUCATIONAL("Educational",
            "Explications claires et engageantes, qui transforment une information complexe en "
                    + "idées digestes, avec des exemples parlants et des questions qui font réfléchir."),

    ENTERTAINMENT("Entertainment",
            "Contenu énergique, en phase avec les tendances actuelles, langage percutant et "
                    + "immédiatement accrocheur."),

    COMEDY("Comedy",
            "Contenu incisif et inattendu, avec des observations fines et un sens du rythme comique."),

    PERSONAL_VLOG("Personal vlog",
            "Contenu authentique et intime, ton conversationnel, narration personnelle."),

    REVIEWS("Reviews",
            "Contenu tranché, basé sur l'expérience, qui met clairement en avant points forts et "
                    + "points faibles."),

    STORYTELLING("Storytelling",
            "Contenu immersif, riche en détails concrets, qui crée une connexion émotionnelle.");

    private final String label;
    private final String promptGuidance;

    ContentCategory(String label, String promptGuidance) {
        this.label = label;
        this.promptGuidance = promptGuidance;
    }

    public String label() {
        return label;
    }

    public String promptGuidance() {
        return promptGuidance;
    }

    /** Retrouve la catégorie à partir du libellé renvoyé par Claude (insensible à la casse/aux espaces). */
    public static ContentCategory fromLabel(String rawLabel) {
        String normalized = rawLabel.trim();
        for (ContentCategory category : values()) {
            if (category.label.equalsIgnoreCase(normalized)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Catégorie inconnue renvoyée par Claude : " + rawLabel);
    }
}
