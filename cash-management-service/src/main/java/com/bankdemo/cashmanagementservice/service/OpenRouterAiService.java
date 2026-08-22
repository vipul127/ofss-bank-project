package com.bankdemo.cashmanagementservice.service;

import com.bankdemo.cashmanagementservice.dto.AiExplanationDto;
import com.bankdemo.cashmanagementservice.dto.DailyCashAnalysisDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.Map;

@Service
public class OpenRouterAiService {
    private static final String SYSTEM_PROMPT = "You are a banking cash-liquidity analyst. Explain branch cash health clearly and conservatively. Use only the supplied metrics; do not invent transactions, balances, causes, or certainty. Distinguish zero activity from a deficit. Return exactly JSON with keys headline, reason, recommendedAction. Keep each value under 25 words.";
    private final RestClient client;
    private final ObjectMapper mapper;
    private final String apiKey;
    private final String model;

    public OpenRouterAiService(ObjectMapper mapper,
            @Value("${openrouter.url}") String url,
            @Value("${openrouter.api-key}") String apiKey,
            @Value("${openrouter.model}") String model) {
        this.client = RestClient.builder().baseUrl(url).build();
        this.mapper = mapper; this.apiKey = apiKey; this.model = model;
    }

    public AiExplanationDto explain(String branchId, DailyCashAnalysisDto analysis) {
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("PASTE_")) return fallback(analysis);
        try {
            String data = mapper.writeValueAsString(Map.of("branchId", branchId, "analysis", analysis));
            String body = client.post().header("Authorization", "Bearer " + apiKey)
                    .header("HTTP-Referer", "http://localhost:8000").header("X-Title", "Branch Cash Console")
                    .body(Map.of("model", model, "temperature", 0.2, "messages", new Object[]{
                            Map.of("role", "system", "content", SYSTEM_PROMPT),
                            Map.of("role", "user", "content", data)})).retrieve().body(String.class);
            JsonNode content = mapper.readTree(body).path("choices").path(0).path("message").path("content");
            JsonNode json = mapper.readTree(content.asText().replace("```json", "").replace("```", "").trim());
            return new AiExplanationDto(json.path("headline").asText("Cash position reviewed"), json.path("reason").asText(), json.path("recommendedAction").asText(), "OpenRouter");
        } catch (Exception ignored) { return fallback(analysis); }
    }

    private AiExplanationDto fallback(DailyCashAnalysisDto a) {
        if (a.getTodayNet() == null || a.getTodayNet().signum() == 0) return new AiExplanationDto("No cash movement today", "No deposits or withdrawals were recorded today.", "Continue monitoring the next operating day.", "Rule-based fallback");
        return a.getTodayNet().signum() > 0
                ? new AiExplanationDto("Reserve improved today", "Bundled deposits exceeded withdrawals for the current day.", "Maintain monitoring against the rolling average and forecast band.", "Rule-based fallback")
                : new AiExplanationDto("Reserve reduced today", "Bundled withdrawals exceeded deposits for the current day.", "Review nearby surplus branches and raise a transfer request if needed.", "Rule-based fallback");
    }
}
