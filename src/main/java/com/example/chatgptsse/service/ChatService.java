package com.example.chatgptsse.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface ChatService {
    SseEmitter streamChat(String message);
} 