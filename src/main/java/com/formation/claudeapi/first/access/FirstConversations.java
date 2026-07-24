package com.formation.claudeapi.first.access;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.formation.claudeapi.AbstractClaudeConversation;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class FirstConversations extends AbstractClaudeConversation {

    public static void main(String[] args) {
        System.out.println("Hello, this is the first request to chat with Claude !!");
//        noContextConversation();
//        withContextConversation();
        firstChat();
    }

    private static void noContextConversation(){
        sendMessage(
                buildUserMessage("What is quantum computing? Answer in one sentence")
        );

        sendMessage(
                buildUserMessage("Write another sentence")
        );
    }

    private static void sendMessage(MessageParam messageParam){
        AnthropicClient client = buildClient();

        MessageCreateParams params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(1000)
                .addMessage(messageParam)
                .build();

        Message messageResponse = client.messages().create(params);
        String responseText = messageResponse.content().getFirst().asText().text();
        System.out.println(responseText);
    }

    private static MessageParam buildUserMessage(String message){
        return MessageParam.builder()
                .role(MessageParam.Role.USER)
                .content(message)
                .build();
    }

    private static void withContextConversation(){
        List<MessageParam> messages = new ArrayList<>();

        addUserMessage(messages, "Define quantum computing in one sentence");
        String firstResponse = chat(messages, null, null, null, null);
        System.out.println(firstResponse);

        addAssistantMessage(messages, firstResponse);
        addUserMessage(messages, "Write another sentence");

        String secondResponse = chat(messages, null, null, null, null);
        System.out.println(secondResponse);

    }

    private static void firstChat(){
        List<MessageParam> messages = new ArrayList<>();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("You: ");
            String userInput = scanner.nextLine();

            if (userInput.equalsIgnoreCase("quit") || userInput.equalsIgnoreCase("exit")) {
                break;
            }

            // 1. Add it to the list of messages
            addUserMessage(messages, userInput);

            // 2. Call the API
            String responseText = chat(messages, null, null, null, null);

            // 3. Add generated text to the list of messages
            addAssistantMessage(messages, responseText);

            // 4. Print the generated text
            System.out.println("Claude: " + responseText);
        }

        scanner.close();
    }
}

