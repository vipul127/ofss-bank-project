package com.bankdemo.cashmanagementservice.service;

import com.bankdemo.cashmanagementservice.dto.AiDiagnosticsDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import java.util.List;
import java.util.Map;

/** Backs the hidden /openr debug page. Fires one real, minimal OpenRouter call and reports back
 * exactly what happened — HTTP status, response body, exception type — instead of the silent
 * catch(Exception) → fallback that every other AI call path uses on purpose (so the product UI
 * never breaks), which is exactly why this needed its own dedicated route to actually see. */
@Service
public class AiDiagnosticsService {
    private final RestClient client;
    private final String apiKey;
    private final String model;
    private final String url;

    public AiDiagnosticsService(RestClient.Builder restClientBuilder,
            @Value("${openrouter.url}") String url,
            @Value("${openrouter.api-key:}") String apiKey,
            @Value("${openrouter.model}") String model) {
        this.client = restClientBuilder.baseUrl(url).build();
        this.url = url; this.apiKey = apiKey; this.model = model;
    }

    public AiDiagnosticsDto check() {
        boolean keyConfigured = apiKey != null && !apiKey.isBlank() && !apiKey.startsWith("PASTE_");
        String masked = maskKey(apiKey);
        if (!keyConfigured) {
            return new AiDiagnosticsDto(false, masked, model, url, false, null, null,
                    "No API key configured", "openrouter.api-key is blank or still the placeholder — set a real key in application.yml.");
        }
        try {
            String body = client.post().header("Authorization", "Bearer " + apiKey)
                    .header("HTTP-Referer", "http://localhost:8000").header("X-Title", "Branch Cash Console - Diagnostics")
                    .body(Map.of("model", model, "temperature", 0, "messages", List.of(
                            Map.of("role", "user", "content", "Reply with exactly: OK"))))
                    .retrieve().body(String.class);
            return new AiDiagnosticsDto(true, masked, model, url, true, 200, body, null, "Working — OpenRouter answered normally.");
        } catch (RestClientResponseException e) {
            return new AiDiagnosticsDto(true, masked, model, url, false, e.getStatusCode().value(),
                    e.getResponseBodyAsString(), e.getMessage(), likelyCause(e.getStatusCode().value(), e.getResponseBodyAsString()));
        } catch (Exception e) {
            return new AiDiagnosticsDto(true, masked, model, url, false, null, null,
                    e.toString(), "Network-level failure (DNS/timeout/connection refused) — check outbound connectivity to " + url + ".");
        }
    }

    private String maskKey(String key) {
        if (key == null || key.isBlank()) return "(not set)";
        if (key.length() <= 12) return "****";
        return key.substring(0, 8) + "..." + key.substring(key.length() - 4);
    }

    private String likelyCause(int status, String body) {
        return switch (status) {
            case 401 -> "401 Unauthorized — the API key itself is invalid, revoked, or malformed.";
            case 403 -> "403 Forbidden — the key is valid but not permitted to call this model/route (e.g. blocked region, missing scope).";
            case 404 -> body != null && body.contains("unavailable for free")
                    ? "404 — this model is no longer free on this account. OpenRouter requires the account to have credits/verification before free-tier models work, regardless of which free model slug you pick — this is an openrouter.ai account setting, not something fixable in code."
                    : "404 — the model slug is wrong or no longer exists on OpenRouter.";
            case 429 -> "429 — rate limited or free-tier quota exhausted for this key/account.";
            case 402 -> "402 Payment Required — the account has no credits and this call requires them.";
            default -> "HTTP " + status + " — see errorBody for OpenRouter's exact message.";
        };
    }
}
