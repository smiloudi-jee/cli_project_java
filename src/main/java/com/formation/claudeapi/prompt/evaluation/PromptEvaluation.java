package com.formation.claudeapi.prompt.evaluation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formation.claudeapi.AbstractClaudeConversation;
import com.formation.claudeapi.prompt.evaluation.pipeline.EvalPipeline;
import com.formation.claudeapi.prompt.evaluation.pipeline.EvalResult;
import com.formation.claudeapi.prompt.evaluation.pipeline.TestCase;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class PromptEvaluation extends AbstractClaudeConversation {

    public static void main(String[] args) throws IOException {
        EvalPipeline pipeline = new EvalPipeline();
        ObjectMapper mapper = new ObjectMapper();

        InputStream file = PromptEvaluation.class.getResourceAsStream("/Dataset.json");
        List<TestCase> dataset = mapper.readValue(
                file,
                new TypeReference<List<TestCase>>() {});

        List<EvalResult> results = pipeline.runEval(dataset);

        EvalReportPrinter.print(results);
    }

}
