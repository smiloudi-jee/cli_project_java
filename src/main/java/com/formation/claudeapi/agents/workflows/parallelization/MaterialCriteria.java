package com.formation.claudeapi.agents.workflows.parallelization;

/**
 * Une liste de possibilités et leurs critères d'évaluation spécialisés, utilisés par
 * {@link ParallelizationDemo} pour lancer une requête Claude dédiée par possibilité.
 */
public enum MaterialCriteria {

    METAL("Métal",
            "Résistance mécanique élevée, bonne conductivité thermique/électrique, tenue à la "
                    + "fatigue, coût et disponibilité, résistance à la corrosion si usage extérieur."),

    POLYMER("Polymère",
            "Légèreté, facilité de moulage pour des formes complexes, coût de production en grande "
                    + "série, résistance chimique, tenue aux UV et à la température."),

    CERAMIC("Céramique",
            "Dureté et résistance à l'usure, tenue à très haute température, isolation électrique, "
                    + "fragilité aux chocs, coût d'usinage."),

    COMPOSITE("Composite",
            "Rapport résistance/poids, anisotropie des propriétés selon la direction des fibres, "
                    + "coût et complexité de fabrication, tenue à la fatigue."),

    ELASTOMER("Élastomère",
            "Élasticité et capacité d'amortissement, résistance à la déformation permanente, tenue "
                    + "chimique et thermique, usage en étanchéité."),

    WOOD("Bois",
            "Rapport résistance/poids, facilité d'usinage, sensibilité à l'humidité et aux insectes, "
                    + "aspect esthétique, coût.");

    private final String displayName;
    private final String criteria;

    MaterialCriteria(String displayName, String criteria) {
        this.displayName = displayName;
        this.criteria = criteria;
    }

    public String displayName() {
        return displayName;
    }

    public String criteria() {
        return criteria;
    }
}
