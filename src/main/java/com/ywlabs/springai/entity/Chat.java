package com.ywlabs.springai.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class Chat {
    private static Chat instance;
    private Long id;
    private String userMessage;
    private String assistantMessage;
    private LocalDateTime requestTime;
    private LocalDateTime responseTime;
    private String status; // REQUESTED, COMPLETED, ERROR
    private String errorMessage;

    private Chat() {}

    public static synchronized Chat getInstance() {
        if (instance == null) {
            instance = new Chat();
        }
        return instance;
    }

    public void reset() {
        this.id = null;
        this.userMessage = null;
        this.assistantMessage = null;
        this.requestTime = null;
        this.responseTime = null;
        this.status = null;
        this.errorMessage = null;
    }
} 