package com.example.chatgptsse.controller;

import com.example.chatgptsse.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @GetMapping("/stream")
    public SseEmitter streamChat(@RequestParam String message) {
        return chatService.streamChat(message);
    }
} 