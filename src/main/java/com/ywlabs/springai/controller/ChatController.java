package com.ywlabs.springai.controller;

import com.ywlabs.springai.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

@Controller
public class ChatController {

    @Autowired
    private ChatService chatService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/history")
    public String history() {
        return "history";
    }

    @GetMapping("/api/chat/stream")
    @ResponseBody
    public SseEmitter streamChat(@RequestParam String message) {
        return chatService.streamChat(message);
    }

    @GetMapping("/api/chat/history")
    @ResponseBody
    public Object getChatHistory() {
        return chatService.getChatHistory();
    }

    @GetMapping("/api/chat/history/paging")
    @ResponseBody
    public Object getChatHistoryWithPaging(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return chatService.getChatHistoryWithPaging(page, pageSize);
    }
} 