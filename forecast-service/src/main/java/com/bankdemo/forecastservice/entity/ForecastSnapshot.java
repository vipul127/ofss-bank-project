package com.bankdemo.forecastservice.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "FORECAST_SNAPSHOT")
public class ForecastSnapshot {
	@Id
	@Column(name = "FORECAST_ID", length = 36)
	private String forecastId;
	@Column(name = "BRANCH_ID", nullable = false, length = 10)
	private String branchId;
	@Column(name = "FORECAST_DATE", nullable = false)
	private LocalDate forecastDate;
	@Column(name = "PREDICTED_POSITION", nullable = false, precision = 15, scale = 2)
	private BigDecimal predictedPosition;
	@Column(name = "CONFIDENCE_BAND_LOW", nullable = false, precision = 15, scale = 2)
	private BigDecimal confidenceBandLow;
	@Column(name = "CONFIDENCE_BAND_HIGH", nullable = false, precision = 15, scale = 2)
	private BigDecimal confidenceBandHigh;
	@Column(name = "ACTUAL_POSITION", precision = 15, scale = 2)
	private BigDecimal actualPosition;
	public String getForecastId() { return forecastId; }
	public void setForecastId(String forecastId) { this.forecastId = forecastId; }
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
	public BigDecimal getActualPosition() { return actualPosition; }
	public void setActualPosition(BigDecimal actualPosition) { this.actualPosition = actualPosition; }
}
