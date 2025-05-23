package com.ywlabs.springai.service;

import com.ywlabs.springai.entity.Chat;
import com.ywlabs.springai.mapper.ChatMapper;
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
import java.util.List;
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
        AtomicReference<StringBuilder> responseBuilder = new AtomicReference<>(new StringBuilder());

        new Thread(() -> {
            Chat chat = Chat.getInstance();
            try {
                chat.reset();
                chat.setUserMessage(message);
                chat.setRequestTime(LocalDateTime.now());
                chat.setStatus("REQUESTED");
                chatMapper.insert(chat);

                SystemMessage systemMessage = new SystemMessage("You are a helpful assistant.");
                UserMessage userMessage = new UserMessage(message);
                Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

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