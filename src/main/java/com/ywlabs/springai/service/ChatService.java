package com.ywlabs.springai.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.List;
import java.util.Map;
import com.ywlabs.springai.entity.Chat;

public interface ChatService {
    SseEmitter streamChat(String message);
    List<Chat> getChatHistory();
    Map<String, Object> getChatHistoryWithPaging(int page, int pageSize);
} 