package com.example.chatgptsse;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.chatgptsse.mapper")
public class ChatgptSseApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChatgptSseApplication.class, args);
    }
} 