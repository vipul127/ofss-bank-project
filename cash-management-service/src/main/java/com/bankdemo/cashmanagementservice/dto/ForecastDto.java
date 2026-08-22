package com.bankdemo.cashmanagementservice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Mirrors forecast-service's ForecastDto — this is the read-only view cash-management-service
 * consumes to ground the AI explanation in the actual next-day projection instead of guessing off
 * today's numbers alone. */
public class ForecastDto {
    private String branchId;
    private LocalDate forecastDate;
    private BigDecimal predictedPosition;
    private BigDecimal confidenceBandLow;
    private BigDecimal confidenceBandHigh;
    private BigDecimal currentPosition;
    private boolean atRisk;

    public String getBranchId() { return branchId; }
    public void setBranchId(String branchId) { this.branchId = branchId; }
    public LocalDate getForecastDate() { return forecastDate; }
    public void setForecastDate(LocalDate forecastDate) { this.forecastDate = forecastDate; }
    public BigDecimal getPredictedPosition() { return predictedPosition; }
    public void setPredictedPosition(BigDecimal predictedPosition) { this.predictedPosition = predictedPosition; }
    public BigDecimal getConfidenceBandLow() { return confidenceBandLow; }
    public void setConfidenceBandLow(BigDecimal confidenceBandLow) { this.confidenceBandLow = confidenceBandLow; }
    public BigDecimal getConfidenceBandHigh() { return confidenceBandHigh; }
    public void setConfidenceBandHigh(BigDecimal confidenceBandHigh) { this.confidenceBandHigh = confidenceBandHigh; }
    public BigDecimal getCurrentPosition() { return currentPosition; }
    public void setCurrentPosition(BigDecimal currentPosition) { this.currentPosition = currentPosition; }
    public boolean isAtRisk() { return atRisk; }
    public void setAtRisk(boolean atRisk) { this.atRisk = atRisk; }
}
