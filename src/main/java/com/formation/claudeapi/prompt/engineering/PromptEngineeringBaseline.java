package com.formation.claudeapi.prompt.engineering;

import com.anthropic.models.messages.MessageParam;
import com.formation.claudeapi.AbstractClaudeConversation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Exemple du cours : Générer un plan de repas d'une journée pour un athlète, à
 * partir de sa taille, son poids, son objectif et ses restrictions alimentaires.
 */
public class PromptEngineeringBaseline extends AbstractClaudeConversation {

    private static final String TASK_DESCRIPTION = "Write a compact, concise 1 day meal plan for a single athlete";

    private static final Map<String, String> PROMPT_INPUTS_SPEC = Map.of(
            "height", "Athlete's height in cm",
            "weight", "Athlete's weight in kg",
            "goal", "Goal of the athlete",
            "restrictions", "Dietary restrictions of the athlete"
    );

    private static final String EXTRA_CRITERIA = """
            The output should include:
            - Daily caloric total
            - Macronutrient breakdown
            - Meals with exact foods, portions, and timing
            """;

    /**
     * Prompt basique à améliorer à l'aide de chaque technique de prompt engineering.
     */
    static String runPrompt(Map<String, String> promptInputs) {

        // Being Clear & Direct : "What should this person eat ?" becomes "Generate a one-day meal plan for an athlete that meets their dietary restrictions."
        // Being Specific : Add Guidelines or / add Process Steps to follow.
        // Follow this process to build the meal plan:
        // 1. Estimate the athlete's daily caloric needs from their height, weight, and goal.
        // 2. Derive a macronutrient split (protein, carbs, fat) that matches that goal.
        // 3. Shortlist foods that comply with the dietary restrictions.
        // 4. Assign each food a portion in grams so each meal's totals track the macro split from step 2.
        // 5. Schedule the meals across the day with realistic times.
        // 6. Sum all meals and confirm the daily totals match the target from step 1.
        //
        String prompt = """
                Generate a one-day meal plan for an athlete that meets their dietary restrictions.
                
                <athlete_information>
                - Height: %s
                - Weight: %s
                - Goal: %s
                - Dietary restrictions: %s
                </athlete_information>
               
                Guidelines:
                1. Include accurate daily calorie amount
                2. Show protein, fat, and carb amounts
                3. Specify when to eat each meal
                4. Use only foods that fit restrictions
                5. List all portion sizes in grams
                6. Keep budget-friendly if mentioned
                
                Here is an example with a sample input and an ideal output:
                <sample_input>
                - Height: 188
                - Weight: 105
                - Goal: Gain muscle mass
                - Restrictions: vegan, gluten-free, nut allergy
                </sample_input>
                <ideal_output>
                ## Meal 1: Breakfast (7:00 AM)
                **High-Protein Oatmeal Bowl**
                - Gluten-free oats: 100g
                - Vegan protein powder (pea/rice blend): 40g
                - Banana: 120g
                - Ground flaxseed: 15g
                - Blueberries: 80g
                - Soy milk (fortified): 300ml
                **Meal totals:** 685 kcal | 42g protein | 95g carbs | 16g fat
                
                ---
                
                ## Meal 2: Mid-Morning Snack (10:30 AM)
                **Protein Smoothie**
                - Vegan protein powder: 30g
                - Frozen mango: 100g
                - Spinach: 50g
                - Chia seeds: 15g
                - Oat milk: 300ml
                - Hemp seeds: 15g
                **Meal totals:** 430 kcal | 32g protein | 45g carbs | 15g fat
                
                ---
                
                ## Meal 3: Pre-Workout Lunch (12:30 PM)
                **Quinoa Power Bowl**
                - Cooked quinoa: 200g
                - Black beans (cooked): 150g
                - Sweet potato (roasted): 200g
                - Broccoli (steamed): 150g
                - Avocado: 80g
                - Tahini dressing: 20g
                - Nutritional yeast: 10g
                **Meal totals:** 745 kcal | 32g protein | 105g carbs | 22g fat
                
                ---
                
                ## Meal 4: Post-Workout Shake (4:00 PM)
                **Recovery Shake**
                - Vegan protein powder: 40g
                - Banana: 120g
                - Dates: 40g
                - Soy milk: 400ml
                - Ground flaxseed: 10g
                **Meal totals:** 525 kcal | 45g protein | 72g carbs | 10g fat
                
                ---
                
                ## Meal 5: Dinner (7:00 PM)
                **Tofu Stir-Fry with Rice**
                - Firm tofu (pressed): 250g
                - Brown rice (cooked): 250g
                - Mixed vegetables (bell peppers, bok choy, carrots): 250g
                - Edamame (shelled): 100g
                - Olive oil: 15ml
                - Tamari (gluten-free soy sauce): 20ml
                - Sesame seeds: 10g
                **Meal totals:** 720 kcal | 48g protein | 82g carbs | 24g fat
                
                ---
                
                ## Meal 6: Evening Snack (9:30 PM)
                **Protein-Rich Snack**
                - Rice cakes (gluten-free): 30g
                - Sunflower seed butter: 30g
                - Vegan protein bar (nut-free): 60g
                - Apple: 150g
                **Meal totals:** 395 kcal | 18g protein | 52g carbs | 14g fat
                
                ---
                
                ## Daily Totals
                - **Calories:** 3,500 kcal
                - **Protein:** 217g
                - **Carbohydrates:** 451g
                - **Fat:** 101g
                
                ## Hydration
                - Drink 3-4 liters of water throughout the day
                - Add electrolytes during/after training
                
                ## Supplementation Recommendations
                - Vitamin B12 (essential for vegans)
                - Vitamin D3 (vegan source)
                - Creatine monohydrate: 5g daily
                - Omega-3 from algae oil
                
                ## Notes
                - All protein powders should be verified nut-free
                - Adjust portions based on training intensity
                - This plan
                </ideal_output>
                This solution comprehensively meets all mandatory requirements: it provides daily caloric totals, macronutrient breakdowns, and complete meals with exact foods, portions, and timing. All secondary criteria are satisfied with strict adherence to dietary restrictions and appropriate high-protein content for muscle gain. The meal plan is well-structured, practical, and nutritionally sound for a 105kg athlete.
                """.formatted(
                promptInputs.get("height"),
                promptInputs.get("weight"),
                promptInputs.get("goal"),
                promptInputs.get("restrictions"));

        List<MessageParam> messages = new ArrayList<>();
        addUserMessage(messages, prompt);

        return chat(messages, null, null, null);
    }

    public static void main(String[] args) {
        // Démarre bas (3) pour éviter les erreurs de rate limit ; à augmenter si le quota le permet.
        PromptEvaluator evaluator = new PromptEvaluator(3);

        if(dotenv.get("GENERATE_DATASET").equals("YES")) {
            evaluator.generateDataset(
                    TASK_DESCRIPTION,
                    PROMPT_INPUTS_SPEC,
                    3,
                    "datasetEngineering.json");
            return;
        }

        evaluator.runEvaluation(
                PromptEngineeringBaseline::runPrompt,
                "datasetEngineering.json",
                EXTRA_CRITERIA,
                "output.json",
                "output.html");
    }
}
