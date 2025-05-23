package com.ywlabs.springai.service;

import com.ywlabs.springai.entity.Employee;
import com.ywlabs.springai.entity.Chat;
import com.ywlabs.springai.mapper.EmployeeMapper;
import com.ywlabs.springai.mapper.ChatMapper;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class EmployeeTrainingService {
    
    private static final String ERROR_MESSAGE = "현재 직원정보 조회가 원활하지 않습니다. 잠시후 다시 시도해 주세요";
    
    private final EmployeeMapper employeeMapper;
    private final ChatClient chatClient;
    private final OpenAiChatClient openAiChatClient;
    private final ChatMapper chatMapper;
    private Message systemMessage;
    
    @Value("${spring.ai.openai.model}")
    private String openaiModel;
    
    public EmployeeTrainingService(EmployeeMapper employeeMapper, ChatClient chatClient, 
                                 OpenAiChatClient openAiChatClient, ChatMapper chatMapper) {
        this.employeeMapper = employeeMapper;
        this.chatClient = chatClient;
        this.openAiChatClient = openAiChatClient;
        this.chatMapper = chatMapper;
        initializeSystemPrompt();
    }
    
    private void initializeSystemPrompt() {
        try {
            List<Employee> employees = employeeMapper.findAll();
            
            // 직원 정보를 학습 데이터 형식으로 변환
            String trainingData = employees.stream()
                .map(emp -> String.format(
                    "직원 정보 #%d\n" +
                    "이름: %s\n" +
                    "직책: %s\n" +
                    "부서: %s\n" +
                    "이메일: %s\n" +
                    "전화번호: %s\n" +
                    "SNS: %s\n" +
                    "-------------------",
                    employees.indexOf(emp) + 1,
                    emp.getName(),
                    emp.getPosition(),
                    emp.getDeptNm(),
                    emp.getEmail(),
                    emp.getPhone(),
                    emp.getSns()
                ))
                .collect(Collectors.joining("\n"));
            
            // 시스템 프롬프트 생성
            String systemPrompt = String.format("""
                당신은 영우랩스의 직원 정보를 제공하는 AI 어시스턴트입니다.
                아래 직원 정보를 참고하여 질문에 답변해주세요.

                직원 정보 목록:
                %s

                응답 규칙:
                1. 위 직원 정보 목록에서 요청된 정보를 정확하게 찾아서 제공하세요.
                2. 정보가 없는 경우 "해당 정보를 찾을 수 없습니다"라고만 답변하세요.
                3. 임의로 정보를 만들어내지 마세요.
                4. 모든 응답은 한국어로 제공하세요.
                5. 부서명이나 직원명이 정확히 일치하지 않더라도, 유사한 정보가 있다면 제공하세요.
                6. 여러 명의 정보를 요청받은 경우, 해당하는 모든 직원의 정보를 나열하세요.
                7. 응답 형식은 다음과 같이 깔끔하게 정리해주세요:
                   - 이름: [이름]
                   - 직책: [직책]
                   - 부서: [부서]
                   - 이메일: [이메일]
                   - 전화번호: [전화번호]
                   - SNS: [SNS]
                8. 여러 명의 정보를 나열할 때만 각 직원 정보 사이에 구분선(---)을 넣어주세요.
                   한 명의 정보만 표시할 때는 구분선을 넣지 마세요.
                9. 응답은 문장 단위로 전송해주세요.
                """, trainingData);
            
            // 시스템 메시지를 한 번만 생성하여 저장
            this.systemMessage = new SystemPromptTemplate(systemPrompt).createMessage();
            
            System.out.println("=== System Prompt Initialized ===");
            System.out.println("Total employees: " + employees.size());
            System.out.println("===============================");
            
        } catch (Exception e) {
            System.err.println("Error during system prompt initialization: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public SseEmitter queryEmployeeInfo(String prompt) {
        SseEmitter emitter = new SseEmitter();
        AtomicReference<StringBuilder> responseBuilder = new AtomicReference<>(new StringBuilder());
        AtomicReference<StringBuilder> currentSentence = new AtomicReference<>(new StringBuilder());
        AtomicReference<Boolean> hasError = new AtomicReference<>(false);
        AtomicReference<Boolean> hasCompleted = new AtomicReference<>(false);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Chat chat = Chat.getInstance();
                try {
                    chat.reset();
                    chat.setUserMessage(prompt);
                    chat.setRequestTime(LocalDateTime.now());
                    chat.setStatus("REQUESTED");
                    chatMapper.insert(chat);

                    List<Message> messages = new ArrayList<>();
                    messages.add(systemMessage);
                    messages.add(new UserMessage(prompt));
                    Prompt chatPrompt = new Prompt(messages);

                    openAiChatClient.stream(chatPrompt)
                        .doOnError(error -> {
                            try {
                                if (!hasCompleted.get()) {
                                    hasError.set(true);
                                    chat.setStatus("ERROR");
                                    chat.setErrorMessage(error.getMessage());
                                    chatMapper.update(chat);
                                    try {
                                        emitter.send(ERROR_MESSAGE, MediaType.TEXT_EVENT_STREAM);
                                        emitter.complete();
                                    } catch (IOException ex) {
                                        emitter.completeWithError(ex);
                                    }
                                }
                            } catch (Exception e) {
                                emitter.completeWithError(e);
                            }
                        })
                        .doOnComplete(() -> {
                            try {
                                if (!hasError.get()) {
                                    hasCompleted.set(true);
                                    // 마지막 문장이 있다면 전송
                                    String lastSentence = currentSentence.get().toString().trim();
                                    if (!lastSentence.isEmpty()) {
                                        emitter.send(lastSentence, MediaType.TEXT_EVENT_STREAM);
                                        responseBuilder.get().append(lastSentence);
                                    }
                                    
                                    chat.setStatus("COMPLETED");
                                    chat.setAssistantMessage(responseBuilder.get().toString());
                                    chat.setResponseTime(LocalDateTime.now());
                                    chatMapper.update(chat);
                                    emitter.complete();
                                }
                            } catch (IOException e) {
                                if (!hasError.get()) {
                                    hasError.set(true);
                                    chat.setStatus("ERROR");
                                    chat.setErrorMessage(e.getMessage());
                                    chatMapper.update(chat);
                                    try {
                                        emitter.send(ERROR_MESSAGE, MediaType.TEXT_EVENT_STREAM);
                                        emitter.complete();
                                    } catch (IOException ex) {
                                        emitter.completeWithError(ex);
                                    }
                                }
                            }
                        })
                        .subscribe(response -> {
                            try {
                                if (!hasError.get() && !hasCompleted.get()) {
                                    String content = response.getResult().getOutput().getContent();
                                    if (content != null && !content.isEmpty()) {
                                        currentSentence.get().append(content);
                                        
                                        // 문장이 완성되었는지 확인 (마침표, 줄바꿈, 콜론 등으로 구분)
                                        String currentText = currentSentence.get().toString();
                                        if (currentText.endsWith(".") || currentText.endsWith("\n") || 
                                            currentText.endsWith(":") || currentText.endsWith("-")) {
                                            String sentence = currentSentence.get().toString().trim();
                                            if (!sentence.isEmpty()) {
                                                emitter.send(sentence, MediaType.TEXT_EVENT_STREAM);
                                                responseBuilder.get().append(sentence);
                                                currentSentence.set(new StringBuilder());
                                            }
                                        }
                                    }
                                }
                            } catch (IOException e) {
                                if (!hasError.get() && !hasCompleted.get()) {
                                    hasError.set(true);
                                    chat.setStatus("ERROR");
                                    chat.setErrorMessage(e.getMessage());
                                    chatMapper.update(chat);
                                    try {
                                        emitter.send(ERROR_MESSAGE, MediaType.TEXT_EVENT_STREAM);
                                        emitter.complete();
                                    } catch (IOException ex) {
                                        emitter.completeWithError(ex);
                                    }
                                }
                            }
                        });
                } catch (Exception e) {
                    if (!hasError.get() && !hasCompleted.get()) {
                        try {
                            hasError.set(true);
                            chat.setStatus("ERROR");
                            chat.setErrorMessage(e.getMessage());
                            chatMapper.update(chat);
                            try {
                                emitter.send(ERROR_MESSAGE, MediaType.TEXT_EVENT_STREAM);
                                emitter.complete();
                            } catch (IOException ex) {
                                emitter.completeWithError(ex);
                            }
                        } catch (Exception ex) {
                            emitter.completeWithError(ex);
                        }
                    }
                }
            }
        });
        thread.start();

        return emitter;
    }
    
    public void resetSystemPrompt() {
        initializeSystemPrompt();
    }
} 