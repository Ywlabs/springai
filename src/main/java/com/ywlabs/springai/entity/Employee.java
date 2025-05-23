package com.ywlabs.springai.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Employee {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String position;
    private String deptNm;
    private String sns;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 