package com.bankdemo.cashmanagementservice.service;

import com.bankdemo.cashmanagementservice.client.CashRequirementServiceClient;
import com.bankdemo.cashmanagementservice.client.ForecastServiceClient;
import com.bankdemo.cashmanagementservice.dto.AiExplanationDto;
import com.bankdemo.cashmanagementservice.dto.CashPositionDto;
import com.bankdemo.cashmanagementservice.dto.DailyCashAnalysisDto;
import com.bankdemo.cashmanagementservice.dto.ForecastDto;
import com.bankdemo.cashmanagementservice.dto.SuggestedSourceDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class OpenRouterAiService {
    private static final Logger log = LoggerFactory.getLogger(OpenRouterAiService.class);
    // A branch holding at least this multiple of its minimum threshold has real spare headroom,
    // not just "technically surplus" — worth flagging as offload-worthy rather than just healthy.
    private static final BigDecimal COMFORTABLE_SURPLUS_MULTIPLE = BigDecimal.valueOf(1.5);
    private static final String SYSTEM_PROMPT = "You are a banking cash-liquidity analyst. Explain branch cash health clearly and conservatively. Use only the supplied metrics; do not invent transactions, balances, causes, or certainty. Distinguish zero activity from a deficit. "
            + "The payload includes a `forecast` block already computed by a statistical model (14-day rolling mean + weekday adjustment + trend, with a confidence band) — treat forecast.predictedPosition as the actual next-day projection, not something to re-derive yourself. If forecast.atRisk is true, the branch is projected to breach its minimum reserve threshold tomorrow and cash logistics must be alerted; if a `suggestedSource` branch is present, name it as the transfer source. "
            + "If a `surplusOpportunity` block is present (only sent when the branch is NOT at risk), the branch is holding comfortably more cash than its minimum threshold requires — be positive and congratulatory in headline and reason (e.g. \"doing great\", \"strong liquidity position\"), then proactively recommend offloading part of the excess (surplusOpportunity.excessAboveThreshold) via an inter-branch transfer. If surplusOpportunity.networkDeficitBranches is non-empty, name those specific branches as recipients; otherwise recommend it generically for network resilience. "
            + "All amounts in the payload are Indian Rupees. State every amount in Indian Rupees using the ₹ symbol (e.g. ₹1,25,000, Indian digit grouping) — never dollars, never a bare number with no currency mark. "
            + "Return exactly JSON with keys headline, reason, recommendedAction. Keep each value under 25 words.";
    private final RestClient client;
    private final ObjectMapper mapper;
    private final ForecastServiceClient forecastServiceClient;
    private final CashRequirementServiceClient cashRequirementServiceClient;
    private final CashPositionService cashPositionService;
    private final AiInsightCacheService cacheService;
    private final String apiKey;
    private final String model;

    public OpenRouterAiService(ObjectMapper mapper, ForecastServiceClient forecastServiceClient,
            CashRequirementServiceClient cashRequirementServiceClient, CashPositionService cashPositionService,
            AiInsightCacheService cacheService,
            @Value("${openrouter.url}") String url,
            @Value("${openrouter.api-key}") String apiKey,
            @Value("${openrouter.model}") String model) {
        this.client = RestClient.builder().baseUrl(url).build();
        this.mapper = mapper;
        this.forecastServiceClient = forecastServiceClient;
        this.cashRequirementServiceClient = cashRequirementServiceClient;
        this.cashPositionService = cashPositionService;
        this.cacheService = cacheService;
        this.apiKey = apiKey; this.model = model;
    }

    // Everything below (forecast lookup, surplus/deficit checks, the OpenRouter call itself) only
    // runs when the cache is missing or older than the configured TTL — a refresh within the TTL
    // window is served straight from AI_INSIGHT_CACHE, no outbound calls at all.
    public AiExplanationDto explain(String branchId, DailyCashAnalysisDto analysis) {
        return cacheService.getOrRefresh("EXPL:" + branchId, AiExplanationDto.class,
                () -> computeExplanation(branchId, analysis));
    }

    private AiExplanationDto computeExplanation(String branchId, DailyCashAnalysisDto analysis) {
        ForecastDto forecast = fetchForecast(branchId);
        boolean requiresLogistics = forecast != null && forecast.isAtRisk();
        SuggestedSourceDto source = requiresLogistics ? fetchSuggestedSource(branchId) : null;
        BigDecimal predicted = forecast != null ? forecast.getPredictedPosition() : null;

        // Only look for an offload opportunity when this branch itself isn't the one at risk.
        SurplusOpportunity surplusOpportunity = requiresLogistics ? null : detectSurplusOpportunity(branchId);

        AiExplanationDto fallback = fallback(analysis, requiresLogistics, predicted, source, surplusOpportunity);
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("PASTE_")) return fallback;
        try {
            String data = mapper.writeValueAsString(Map.of(
                    "branchId", branchId,
                    "analysis", analysis,
                    "forecast", forecast != null ? forecast : Map.of(),
                    "suggestedSource", source != null ? source : Map.of(),
                    "surplusOpportunity", surplusOpportunity != null ? surplusOpportunity : Map.of()));
            String body = client.post().header("Authorization", "Bearer " + apiKey)
                    .header("HTTP-Referer", "http://localhost:8000").header("X-Title", "Branch Cash Console")
                    .body(Map.of("model", model, "temperature", 0.2, "messages", new Object[]{
                            Map.of("role", "system", "content", SYSTEM_PROMPT),
                            Map.of("role", "user", "content", data)})).retrieve().body(String.class);
            JsonNode content = mapper.readTree(body).path("choices").path(0).path("message").path("content");
            JsonNode json = mapper.readTree(content.asText().replace("```json", "").replace("```", "").trim());
            return new AiExplanationDto(
                    json.path("headline").asText("Cash position reviewed"),
                    json.path("reason").asText(),
                    json.path("recommendedAction").asText(),
                    "OpenRouter",
                    requiresLogistics, predicted,
                    source != null ? source.getBranchId() : null,
                    source != null ? source.getBranchName() : null);
        } catch (Exception e) {
            log.warn("[AI] OpenRouter call failed for branch {}, falling back to rule-based summary: {}", branchId, e.toString());
            return fallback;
        }
    }

    private ForecastDto fetchForecast(String branchId) {
        try { return forecastServiceClient.getForecast(branchId); } catch (Exception ignored) { return null; }
    }

    private SuggestedSourceDto fetchSuggestedSource(String branchId) {
        try { return cashRequirementServiceClient.suggestSource(branchId); } catch (Exception ignored) { return null; }
    }

    private SurplusOpportunity detectSurplusOpportunity(String branchId) {
        try {
            CashPositionDto position = cashPositionService.getPosition(branchId);
            if (position == null || !"SURPLUS".equals(position.getStatus())) return null;
            BigDecimal comfortableFloor = position.getThresholdAmount().multiply(COMFORTABLE_SURPLUS_MULTIPLE);
            if (position.getCurrentReserve().compareTo(comfortableFloor) < 0) return null;
            BigDecimal excess = position.getCurrentReserve().subtract(position.getThresholdAmount());
            List<CashPositionDto> deficits = List.of();
            try {
                deficits = cashRequirementServiceClient.getDeficitBranches().stream()
                        .filter(d -> !d.getBranchId().equalsIgnoreCase(branchId)).toList();
            } catch (Exception ignored) { /* network deficit lookup is best-effort */ }
            return new SurplusOpportunity(excess, deficits.stream().map(CashPositionDto::getBranchId).toList());
        } catch (Exception ignored) {
            return null;
        }
    }

    private AiExplanationDto fallback(DailyCashAnalysisDto a, boolean requiresLogistics, BigDecimal predicted,
            SuggestedSourceDto source, SurplusOpportunity surplusOpportunity) {
        String sourceName = source != null ? source.getBranchName() : null;
        if (requiresLogistics) {
            String reason = "Forecast projects tomorrow's reserve at " + predicted
                    + ", below the branch's minimum threshold based on the 14-day rolling trend.";
            String action = sourceName != null
                    ? "Raise a cash-logistics transfer request sourced from " + sourceName + " before tomorrow's opening."
                    : "Raise a cash-logistics transfer request — no surplus source branch currently available.";
            return new AiExplanationDto("Cash Logistics Alert: Replenishment Required", reason, action,
                    "Rule-based fallback", true, predicted,
                    source != null ? source.getBranchId() : null, sourceName);
        }
        if (surplusOpportunity != null) {
            NumberFormat inr = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
            inr.setMaximumFractionDigits(0);
            String reason = "Doing great — this branch is holding " + inr.format(surplusOpportunity.excessAboveThreshold())
                    + " above its minimum threshold, well beyond what daily operations need.";
            String action = surplusOpportunity.networkDeficitBranchIds().isEmpty()
                    ? "Consider offloading part of the excess to strengthen network-wide resilience."
                    : "Consider offloading part of the excess to " + String.join(", ", surplusOpportunity.networkDeficitBranchIds()) + ", which are currently short.";
            return new AiExplanationDto("Strong Liquidity Position", reason, action, "Rule-based fallback", false, predicted, null, null);
        }
        if (a.getTodayNet() == null || a.getTodayNet().signum() == 0) {
            return new AiExplanationDto("No cash movement today", "No deposits or withdrawals were recorded today.", "Continue monitoring the next operating day.", "Rule-based fallback", false, predicted, null, null);
        }
        return a.getTodayNet().signum() > 0
                ? new AiExplanationDto("Reserve improved today", "Bundled deposits exceeded withdrawals for the current day.", "Maintain monitoring against the rolling average and forecast band.", "Rule-based fallback", false, predicted, null, null)
                : new AiExplanationDto("Reserve reduced today", "Bundled withdrawals exceeded deposits for the current day.", "Review nearby surplus branches and raise a transfer request if needed.", "Rule-based fallback", false, predicted, null, null);
    }

    private record SurplusOpportunity(BigDecimal excessAboveThreshold, List<String> networkDeficitBranchIds) {}
}
