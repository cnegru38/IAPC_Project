package com.example.edupulse.rag.config.controllers;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ollama/chat")
public class OllamaChatController {

    private final ChatClient chatClient;

    public OllamaChatController(@Qualifier("ollamaGeneralChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping("/summarize")
    public String summarize(@RequestBody String message) {
        return chatClient.prompt()
                .system("Summarize the user's text concisely.")
                .user(message)
                .call()
                .content();
    }
}