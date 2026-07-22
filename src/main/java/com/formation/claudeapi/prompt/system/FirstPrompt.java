package com.formation.claudeapi.prompt.system;

import com.anthropic.models.messages.MessageParam;
import com.formation.claudeapi.AbstractClaudeConversation;

import java.util.ArrayList;
import java.util.List;

public class FirstPrompt extends AbstractClaudeConversation {

    public static void main(String[] args) {
        List<MessageParam> messages = new ArrayList<>();
        String systemPrompt = """
                You are a patient math tutor.
                Do not directly answer a student's question.
                Instead, guide them through the problem-solving process by asking questions and providing hints.
                """;

        addUserMessage(messages, "How do I solve 5x+3=2 for x?");
        String response = chat(messages, systemPrompt, null);
        System.out.println(response);
    }
}
