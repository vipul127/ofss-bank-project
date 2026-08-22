package com.bankdemo.cashmanagementservice.controller;

import com.bankdemo.cashmanagementservice.dto.AiDiagnosticsDto;
import com.bankdemo.cashmanagementservice.service.AiDiagnosticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Backs the hidden /openr frontend debug page. Not linked from normal navigation and not
 * branch-scoped — it's an operator tool, deliberately outside the X-Branch-Role model. */
@RestController
public class AiDiagnosticsController {
    private final AiDiagnosticsService aiDiagnosticsService;
    public AiDiagnosticsController(AiDiagnosticsService aiDiagnosticsService) { this.aiDiagnosticsService = aiDiagnosticsService; }

    @GetMapping("/api/debug/openrouter")
    public AiDiagnosticsDto checkOpenRouter() {
        return aiDiagnosticsService.check();
    }
}
