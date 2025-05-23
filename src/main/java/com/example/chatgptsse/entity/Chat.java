package com.example.chatgptsse.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class Chat {
    private Long id;
    private String userMessage;
    private String assistantMessage;
    private LocalDateTime requestTime;
    private LocalDateTime responseTime;
    private String status; // REQUESTED, COMPLETED, ERROR
    private String errorMessage;
} 