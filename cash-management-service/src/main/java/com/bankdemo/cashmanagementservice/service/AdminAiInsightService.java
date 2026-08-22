package com.bankdemo.cashmanagementservice.service;

import com.bankdemo.cashmanagementservice.client.BranchServiceClient;
import com.bankdemo.cashmanagementservice.client.ForecastServiceClient;
import com.bankdemo.cashmanagementservice.dto.AdminAiInsightDto;
import com.bankdemo.cashmanagementservice.dto.BranchDto;
import com.bankdemo.cashmanagementservice.dto.CashPositionDto;
import com.bankdemo.cashmanagementservice.dto.ForecastDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Network-wide counterpart to OpenRouterAiService. Deliberately a separate system prompt, not a
 * parameterized version of the branch one: the branch prompt reasons about one branch's own
 * numbers for the branch manager, this one reasons across every branch for the admin — it must
 * name branches, compare them against each other, and give one network-level next-day verdict. */
@Service
public class AdminAiInsightService {
    private static final Logger log = LoggerFactory.getLogger(AdminAiInsightService.class);
    private static final String SYSTEM_PROMPT = "You are a bank's network liquidity controller briefing the admin office. You are given every branch's current reserve, its minimum threshold, and forecast-service's next-day projection. "
            + "Use only the supplied numbers; do not invent branches, amounts, or causes. "
            + "Name every branch by its branchName (and branchId) at least once. For each branch flagged atRisk, state its projected shortfall amount explicitly. "
            + "Conclude with one clear network-wide verdict for tomorrow: either that operations are expected to run smoothly across the network, or that problems are expected because specific named branch(es) are projected to be deficient by specific amounts. "
            + "All amounts in the payload are Indian Rupees. State every amount in Indian Rupees using the ₹ symbol (e.g. ₹1,25,000, Indian digit grouping) — never dollars, never a bare number with no currency mark. "
            + "Return exactly JSON with keys headline, overallConclusion, recommendedAction. Keep headline under 12 words, overallConclusion under 60 words, recommendedAction under 30 words.";

    private final CashPositionService cashPositionService;
    private final BranchServiceClient branchServiceClient;
    private final ForecastServiceClient forecastServiceClient;
    private final RestClient client;
    private final ObjectMapper mapper;
    private final AiInsightCacheService cacheService;
    private final String apiKey;
    private final String model;

    public AdminAiInsightService(CashPositionService cashPositionService, BranchServiceClient branchServiceClient,
            ForecastServiceClient forecastServiceClient, ObjectMapper mapper, AiInsightCacheService cacheService,
            @Value("${openrouter.url}") String url,
            @Value("${openrouter.api-key}") String apiKey,
            @Value("${openrouter.model}") String model) {
        this.cashPositionService = cashPositionService;
        this.branchServiceClient = branchServiceClient;
        this.forecastServiceClient = forecastServiceClient;
        this.client = RestClient.builder().baseUrl(url).build();
        this.mapper = mapper;
        this.cacheService = cacheService;
        this.apiKey = apiKey; this.model = model;
    }

    // Same cache-first pattern as OpenRouterAiService — a request within the TTL window never
    // touches forecast-service, cash-requirement-service, or OpenRouter at all.
    public AdminAiInsightDto getInsights() {
        return cacheService.getOrRefresh("ADMIN_INSIGHTS", AdminAiInsightDto.class, this::computeInsights);
    }

    private AdminAiInsightDto computeInsights() {
        List<CashPositionDto> positions = cashPositionService.getAllPositions();
        Map<String, BranchDto> branchesById = branchServiceClient.getAllBranches().stream()
                .collect(Collectors.toMap(BranchDto::getBranchId, b -> b));

        List<AdminAiInsightDto.BranchInsight> insights = new ArrayList<>();
        for (CashPositionDto position : positions) {
            BranchDto branch = branchesById.get(position.getBranchId());
            String name = branch != null ? branch.getBranchName() : position.getBranchId();
            ForecastDto forecast = fetchForecast(position.getBranchId());
            boolean atRisk = forecast != null && forecast.isAtRisk();
            BigDecimal predicted = forecast != null ? round(forecast.getPredictedPosition()) : null;
            BigDecimal shortfall = atRisk ? position.getThresholdAmount().subtract(predicted).max(BigDecimal.ZERO) : null;
            insights.add(new AdminAiInsightDto.BranchInsight(position.getBranchId(), name, position.getCurrentReserve(),
                    position.getThresholdAmount(), position.getStatus(), predicted, atRisk, shortfall));
        }

        AdminAiInsightDto fallback = fallback(insights);
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("PASTE_")) return fallback;
        try {
            String data = mapper.writeValueAsString(Map.of("branches", insights));
            String body = client.post().header("Authorization", "Bearer " + apiKey)
                    .header("HTTP-Referer", "http://localhost:8000").header("X-Title", "Bank Admin Console")
                    .body(Map.of("model", model, "temperature", 0.2, "messages", new Object[]{
                            Map.of("role", "system", "content", SYSTEM_PROMPT),
                            Map.of("role", "user", "content", data)})).retrieve().body(String.class);
            JsonNode content = mapper.readTree(body).path("choices").path(0).path("message").path("content");
            JsonNode json = mapper.readTree(content.asText().replace("```json", "").replace("```", "").trim());
            return new AdminAiInsightDto(
                    json.path("headline").asText("Network liquidity reviewed"),
                    json.path("overallConclusion").asText(),
                    json.path("recommendedAction").asText(),
                    "OpenRouter", insights);
        } catch (Exception e) {
            log.warn("[AI] OpenRouter admin insight call failed, falling back to rule-based summary: {}", e.toString());
            return fallback;
        }
    }

    private ForecastDto fetchForecast(String branchId) {
        try { return forecastServiceClient.getForecast(branchId); } catch (Exception ignored) { return null; }
    }

    private BigDecimal round(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    private AdminAiInsightDto fallback(List<AdminAiInsightDto.BranchInsight> insights) {
        NumberFormat inr = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        inr.setMaximumFractionDigits(0);
        List<AdminAiInsightDto.BranchInsight> atRisk = insights.stream().filter(AdminAiInsightDto.BranchInsight::atRisk).toList();
        String headline;
        String conclusion;
        String action;
        if (atRisk.isEmpty()) {
            headline = "Smooth Operations Expected Tomorrow";
            conclusion = "All " + insights.size() + " branches are projected to stay above their minimum reserve threshold tomorrow based on the 14-day rolling trend. No network-wide cash logistics action required.";
            action = "Continue routine monitoring; no transfers required tomorrow.";
        } else {
            headline = atRisk.size() + " Branch" + (atRisk.size() > 1 ? "es" : "") + " At Risk Tomorrow";
            String detail = atRisk.stream()
                    .map(b -> b.branchName() + " (" + b.branchId() + ") short by " + inr.format(b.shortfallAmount() != null ? b.shortfallAmount() : BigDecimal.ZERO))
                    .collect(Collectors.joining("; "));
            conclusion = "We expect problems tomorrow because " + detail + ", based on the 14-day rolling forecast against each branch's minimum threshold.";
            action = "Raise cash-logistics transfer requests into " + atRisk.stream().map(AdminAiInsightDto.BranchInsight::branchName).collect(Collectors.joining(", ")) + " before tomorrow's opening.";
        }
        return new AdminAiInsightDto(headline, conclusion, action, "Rule-based fallback", insights);
    }
}
