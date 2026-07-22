package com.formation.claudeapi.structured.data;

import com.anthropic.models.messages.MessageParam;
import com.formation.claudeapi.AbstractClaudeConversation;

import java.util.ArrayList;
import java.util.List;

public class FirstStructuredJsonData extends AbstractClaudeConversation {

    public static void main(String[] args) {
        List<MessageParam> messages = new ArrayList<>();

        String jsonStructure = """
                {
                  "name": "string",
                  "age": "integer",
                  "email": "string",
                  "address": {
                    "street": "string",
                    "city": "string",
                    "zipcode": "string"
                  },
                  "phoneNumbers": [
                    {
                      "type": "string",
                      "number": "string"
                    }
                  ]
                }""";
        String prompt = "Please provide a JSON object that matches the following structure:\n" + jsonStructure;

        addUserMessage(messages, prompt);
        addAssistantMessage(messages, "```json");

        String response = chat(messages, null, List.of("```"));

        System.out.println(response);
    }

}
