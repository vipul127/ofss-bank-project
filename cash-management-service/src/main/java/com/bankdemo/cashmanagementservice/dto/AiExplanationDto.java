package com.bankdemo.cashmanagementservice.dto;

import java.math.BigDecimal;

public record AiExplanationDto(
        String headline,
        String reason,
        String recommendedAction,
        String source,
        // Grounded in forecast-service's next-day projection + cash-requirement-service's
        // nearest-surplus-branch lookup, so the UI can act on this directly instead of only
        // showing prose. null/false when no forecast was available (e.g. forecast-service down).
        boolean requiresCashLogistics,
        BigDecimal predictedTomorrowPosition,
        String suggestedSourceBranchId,
        String suggestedSourceBranchName) {

    /** Back-compat convenience for callers that only have the narrative fields. */
    public AiExplanationDto(String headline, String reason, String recommendedAction, String source) {
        this(headline, reason, recommendedAction, source, false, null, null, null);
    }
}
