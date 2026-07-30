package com.formation.claudeapi.features.image;

import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.MessageParam;
import com.formation.claudeapi.AbstractClaudeConversation;

import java.util.ArrayList;
import java.util.List;

/**
 * Demo "Real-World Example: Fire Risk Assessment".
 * <p>
 * Exemple concret du cours : Plutôt que d'envoyer un inspecteur sur chaque propriété,
 * un assureur habitation décide de faire analyser des images satellites par Claude
 * pour estimer un risque incendie.
 * </p>
 * Un prompt naif, du style "donne-moi un score de risque incendie", le prompt decoupe
 * l'analyse en 5 etapes precises :
 * <ul>
 *     <li>Identification de la residence</li>
 *     <li>Analyse du surplomb des arbres</li>
 *     <li>Evaluation du risque incendie</li>
 *     <li>Identification de l'espace defendable</li>
 *     <li>Note finale de 1 (faible) a 4 (severe)</li>
 * </ul>
 * <p>
 * Les 7 images de propriété fournies avec le cours (prop1.png a prop7.png)
 * sont analysées une par une avec exactement le meme prompt.
 */
public class FireRiskAssessmentDemo extends AbstractClaudeConversation {

    private static final String FIRE_RISK_PROMPT = """
            Analyze the attached satellite image of a property with these specific steps:

            1. Residence identification: Locate the primary residence on the property by looking for:
               - The largest roofed structure
               - Typical residential features (driveway connection, regular geometry)
               - Distinction from other structures (garages, sheds, pools)
               Describe the residence's location relative to property boundaries and other features.

            2. Tree overhang analysis: Examine all trees near the primary residence:
               - Identify any trees whose canopy extends directly over any portion of the roof
               - Estimate the percentage of roof covered by overhanging branches (0-25%, 25-50%, 50-75%, 75-100%)
               - Note particularly dense areas of overhang

            3. Fire risk assessment: For any overhanging trees, evaluate:
               - Potential wildfire vulnerability (ember catch points, continuous fuel paths to structure)
               - Proximity to chimneys, vents, or other roof openings if visible
               - Areas where branches create a "bridge" between wildland vegetation and the structure

            4. Defensible space identification: Assess the property's overall vegetative structure:
               - Identify if trees connect to form a continuous canopy over or near the home
               - Note any obvious fuel ladders (vegetation that can carry fire from ground to tree to roof)

            5. Fire risk rating: Based on your analysis, assign a Fire Risk Rating from 1-4:
               - Rating 1 (Low Risk): No tree branches overhanging the roof, good defensible space around the structure
               - Rating 2 (Moderate Risk): Minimal overhang (<25% of roof), some separation between tree canopies
               - Rating 3 (High Risk): Significant overhang (25-50% of roof), connected tree canopies, multiple points of vulnerability
               - Rating 4 (Severe Risk): Extensive overhang (>50% of roof), dense vegetation against structure, numerous ember catch points, limited defensible space

            For each item above (1-5), write one sentence summarizing your findings, with your final response being the numeric Fire Risk Rating (1-4) with a brief justification.
            """;

    private static final List<String> PROPERTY_IMAGES = List.of(
            "/image/prop1.png",
            "/image/prop2.png",
            "/image/prop3.png",
            "/image/prop4.png",
            "/image/prop5.png",
            "/image/prop6.png",
            "/image/prop7.png"
    );

    public static void main(String[] args) {
        for (String imageResource : PROPERTY_IMAGES) {
            System.out.println("=== " + imageResource + " ===");
            System.out.println(assessProperty(imageResource));
            System.out.println();
        }
    }

    private static String assessProperty(String imageResource) {
        List<ContentBlockParam> content = List.of(
                ImageBlocks.imageFromClasspath(FireRiskAssessmentDemo.class, imageResource),
                text(FIRE_RISK_PROMPT)
        );

        List<MessageParam> messages = new ArrayList<>();
        addUserMessage(messages, content);
        return chat(messages, null, null, null, null);
    }
}
