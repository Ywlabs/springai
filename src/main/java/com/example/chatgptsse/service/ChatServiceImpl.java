package com.example.chatgptsse.service;

import com.example.chatgptsse.entity.Chat;
import com.example.chatgptsse.mapper.ChatMapper;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ChatServiceImpl implements ChatService {

    @Autowired
    private OpenAiChatClient chatClient;

    @Autowired
    private ChatMapper chatMapper;

    @Override
    public SseEmitter streamChat(String message) {
        SseEmitter emitter = new SseEmitter();
        
        // Save initial request
        final Chat chat = new Chat();
        chat.setUserMessage(message);
        chat.setStatus("REQUESTED");
        chat.setRequestTime(LocalDateTime.now());
        chatMapper.insert(chat);
        
        new Thread(() -> {
            try {
                AtomicReference<StringBuilder> responseBuilder = new AtomicReference<>(new StringBuilder());

                Prompt prompt = new Prompt(
                    java.util.List.of(
                        new SystemMessage("모든 답변은 자연스러운 한국어 문장으로, 띄어쓰기를 정확히 지켜서 작성해 주세요."),
                        new UserMessage(message)
                    )
                );

                chatClient.stream(prompt)
                        .doOnError(error -> {
                            chat.setStatus("ERROR");
                            chat.setErrorMessage(error.getMessage());
                            chatMapper.update(chat);
                            emitter.completeWithError(error);
                        })
                        .doOnComplete(() -> {
                            chat.setStatus("COMPLETED");
                            chat.setAssistantMessage(responseBuilder.get().toString());
                            chat.setResponseTime(LocalDateTime.now());
                            chatMapper.update(chat);
                            emitter.complete();
                        })
                        .subscribe(response -> {
                            try {
                                String content = response.getResult().getOutput().getContent();
                                System.out.println("[OpenAI 응답 content] " + content);
                                if (content != null && !content.isEmpty()) {
                                    responseBuilder.get().append(content);
                                    emitter.send(content, MediaType.TEXT_EVENT_STREAM);
                                }
                            } catch (IOException e) {
                                chat.setStatus("ERROR");
                                chat.setErrorMessage(e.getMessage());
                                chatMapper.update(chat);
                                emitter.completeWithError(e);
                            }
                        });
            } catch (Exception e) {
                chat.setStatus("ERROR");
                chat.setErrorMessage(e.getMessage());
                chatMapper.update(chat);
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }
} 