package com.formation.claudeapi.prompt.engineering;

import java.util.List;

/**
 * Construit le rapport HTML d'une évaluation.
 * <p>
 * Un tableau, une ligne par {@link EvaluationResult} : entrées du prompt, critères de
 * réussite, sortie brute, score (coloré) et raisonnement du modèle-juge.
 */
public class PromptEvaluationReport {

    public static String generate(List<EvaluationResult> results) {
        int totalTests = results.size();
        double avgScore = results.stream().mapToDouble(EvaluationResult::score).average().orElse(0.0);
        int maxPossibleScore = 10;
        double passRate = totalTests == 0
                ? 0.0
                : 100.0 * results.stream().filter(r -> r.score() >= 7).count() / totalTests;

        StringBuilder html = new StringBuilder();
        html.append("""
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Prompt Evaluation Report</title>
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            line-height: 1.6;
                            margin: 0;
                            padding: 20px;
                            color: #333;
                        }
                        .header {
                            background-color: #f0f0f0;
                            padding: 20px;
                            border-radius: 5px;
                            margin-bottom: 20px;
                        }
                        .summary-stats {
                            display: flex;
                            justify-content: space-between;
                            flex-wrap: wrap;
                            gap: 10px;
                        }
                        .stat-box {
                            background-color: #fff;
                            border-radius: 5px;
                            padding: 15px;
                            box-shadow: 0 2px 5px rgba(0,0,0,0.1);
                            flex-basis: 30%;
                            min-width: 200px;
                        }
                        .stat-value {
                            font-size: 24px;
                            font-weight: bold;
                            margin-top: 5px;
                        }
                        table {
                            width: 100%;
                            border-collapse: collapse;
                            margin-top: 20px;
                        }
                        th {
                            background-color: #4a4a4a;
                            color: white;
                            text-align: left;
                            padding: 12px;
                        }
                        td {
                            padding: 10px;
                            border-bottom: 1px solid #ddd;
                            vertical-align: top;
                        }
                        tr:nth-child(even) {
                            background-color: #f9f9f9;
                        }
                        .output-cell {
                            white-space: pre-wrap;
                        }
                        .score {
                            font-weight: bold;
                            padding: 5px 10px;
                            border-radius: 3px;
                            display: inline-block;
                        }
                        .score-high {
                            background-color: #c8e6c9;
                            color: #2e7d32;
                        }
                        .score-medium {
                            background-color: #fff9c4;
                            color: #f57f17;
                        }
                        .score-low {
                            background-color: #ffcdd2;
                            color: #c62828;
                        }
                        .output {
                            overflow: auto;
                            white-space: pre-wrap;
                        }
                        .output pre {
                            background-color: #f5f5f5;
                            border: 1px solid #ddd;
                            border-radius: 4px;
                            padding: 10px;
                            margin: 0;
                            font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
                            font-size: 14px;
                            line-height: 1.4;
                            color: #333;
                            box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.1);
                            overflow-x: auto;
                            white-space: pre-wrap;
                            word-wrap: break-word;
                        }
                        td {
                            width: 20%;
                        }
                        .score-col {
                            width: 80px;
                        }
                    </style>
                </head>
                <body>
                """);

        html.append("<div class=\"header\">\n<h1>Prompt Evaluation Report</h1>\n<div class=\"summary-stats\">\n");
        html.append("<div class=\"stat-box\"><div>Total Test Cases</div><div class=\"stat-value\">")
                .append(totalTests).append("</div></div>\n");
        html.append("<div class=\"stat-box\"><div>Average Score</div><div class=\"stat-value\">")
                .append("%.1f".formatted(avgScore)).append(" / ").append(maxPossibleScore).append("</div></div>\n");
        html.append("<div class=\"stat-box\"><div>Pass Rate (≥7)</div><div class=\"stat-value\">")
                .append("%.1f".formatted(passRate)).append("%</div></div>\n");
        html.append("</div>\n</div>\n");

        html.append("""
                <table>
                    <thead>
                        <tr>
                            <th>Scenario</th>
                            <th>Prompt Inputs</th>
                            <th>Solution Criteria</th>
                            <th>Output</th>
                            <th>Score</th>
                            <th>Reasoning</th>
                        </tr>
                    </thead>
                    <tbody>
                """);

        for (EvaluationResult result : results) {
            appendRow(html, result);
        }

        html.append("""
                    </tbody>
                </table>
                </body>
                </html>
                """);

        return html.toString();
    }

    private static void appendRow(StringBuilder html, EvaluationResult result) {
        TestCase testCase = result.testCase();

        String promptInputsHtml = testCase.promptInputs().entrySet().stream()
                .map(e -> "<strong>%s:</strong> %s".formatted(e.getKey(), e.getValue()))
                .reduce((a, b) -> a + "<br>" + b)
                .orElse("");

        String criteriaString = String.join("<br>• ", testCase.solutionCriteria());

        double score = result.score();
        String scoreClass;
        if (score >= 8) {
            scoreClass = "score-high";
        } else if (score <= 5) {
            scoreClass = "score-low";
        } else {
            scoreClass = "score-medium";
        }

        html.append("<tr>\n");
        html.append("<td>").append(testCase.scenario()).append("</td>\n");
        html.append("<td class=\"prompt-inputs\">").append(promptInputsHtml).append("</td>\n");
        html.append("<td class=\"criteria\">• ").append(criteriaString).append("</td>\n");
        html.append("<td class=\"output\"><pre>").append(result.output()).append("</pre></td>\n");
        html.append("<td class=\"score-col\"><span class=\"score ").append(scoreClass).append("\">")
                .append("%.1f".formatted(score)).append("</span></td>\n");
        html.append("<td class=\"reasoning\">").append(result.reasoning()).append("</td>\n");
        html.append("</tr>\n");
    }
}
