package com.ywlabs.springai.controller;

import com.ywlabs.springai.service.EmployeeTrainingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeTrainingService employeeTrainingService;

    @Autowired
    public EmployeeController(EmployeeTrainingService employeeTrainingService) {
        this.employeeTrainingService = employeeTrainingService;
    }

    @GetMapping("/query")
    public SseEmitter queryEmployeeInfo(@RequestParam String prompt) {
        return employeeTrainingService.queryEmployeeInfo(prompt);
    }

    @PostMapping("/reset")
    public ResponseEntity<String> resetSystemPrompt() {
        try {
            employeeTrainingService.resetSystemPrompt();
            return ResponseEntity.ok("System prompt has been reset successfully.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
} 