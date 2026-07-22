package com.formation.claudeapi.structured.data;

import com.anthropic.models.messages.MessageParam;
import com.formation.claudeapi.AbstractClaudeConversation;

import java.util.ArrayList;
import java.util.List;

public class PrefillStopSequenceExercise  extends AbstractClaudeConversation {

    public static void main(String[] args) {
        List<MessageParam> messages = new ArrayList<>();
        addUserMessage(messages, "Give me three different Sample AWS CLI commands. Each should be very short.");

        addAssistantMessage(messages, "Here are three sample AWS CLI commands:\n" + "```bash");

        String response = chat(messages, null, List.of("```"));

        System.out.println(response);
    }
}
