package com.ywlabs.springai.config;

import com.ywlabs.springai.service.EmployeeTrainingService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class EmployeeTrainingRunner implements CommandLineRunner {
    
    private final EmployeeTrainingService employeeTrainingService;
    
    public EmployeeTrainingRunner(EmployeeTrainingService employeeTrainingService) {
        this.employeeTrainingService = employeeTrainingService;
    }
    
    @Override
    public void run(String... args) {
        // 서비스가 초기화될 때 자동으로 학습이 진행되므로 별도의 호출이 필요 없음
        System.out.println("Employee training service initialized.");
    }
} 