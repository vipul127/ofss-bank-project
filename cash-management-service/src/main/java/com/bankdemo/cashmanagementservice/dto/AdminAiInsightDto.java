package com.bankdemo.cashmanagementservice.dto;

import java.math.BigDecimal;
import java.util.List;

/** Network-wide counterpart to AiExplanationDto. Where the branch explanation talks about one
 * branch to the branch manager, this talks about every branch to the admin — naming each one,
 * quantifying its position, and concluding whether tomorrow looks smooth network-wide or which
 * branch(es) will be short and by how much. */
public record AdminAiInsightDto(
        String headline,
        String overallConclusion,
        String recommendedAction,
        String source,
        List<BranchInsight> branchInsights) {

    /** Deterministic per-branch numbers — always accurate even when the narrative above is the
     * rule-based fallback, because these are computed directly from forecast-service/cash-position
     * data, never left to the LLM to state. */
    public record BranchInsight(
            String branchId,
            String branchName,
            BigDecimal currentReserve,
            BigDecimal thresholdAmount,
            String status,
            BigDecimal predictedTomorrowPosition,
            boolean atRisk,
            BigDecimal shortfallAmount) {}
}
