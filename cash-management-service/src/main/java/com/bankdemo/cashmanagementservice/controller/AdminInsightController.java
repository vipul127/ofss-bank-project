package com.bankdemo.cashmanagementservice.controller;

import com.bankdemo.cashmanagementservice.dto.AdminAiInsightDto;
import com.bankdemo.cashmanagementservice.service.AdminAiInsightService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminInsightController {
    private final AdminAiInsightService adminAiInsightService;
    public AdminInsightController(AdminAiInsightService adminAiInsightService) { this.adminAiInsightService = adminAiInsightService; }

    @GetMapping("/insights")
    public AdminAiInsightDto getInsights() {
        return adminAiInsightService.getInsights();
    }
}
