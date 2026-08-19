package com.bankdemo.forecastservice.dto;

import java.math.BigDecimal;

public class BranchDto {
	private String branchId;
	private BigDecimal currentReserve;
	private BigDecimal minThresholdPct;
	public String getBranchId() { return branchId; }
	public void setBranchId(String branchId) { this.branchId = branchId; }
	public BigDecimal getCurrentReserve() { return currentReserve; }
	public void setCurrentReserve(BigDecimal currentReserve) { this.currentReserve = currentReserve; }
	public BigDecimal getMinThresholdPct() { return minThresholdPct; }
	public void setMinThresholdPct(BigDecimal minThresholdPct) { this.minThresholdPct = minThresholdPct; }
}
