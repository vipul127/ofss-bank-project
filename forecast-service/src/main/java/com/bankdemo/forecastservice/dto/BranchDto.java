package com.bankdemo.forecastservice.dto;

import java.math.BigDecimal;

public class BranchDto {
	private String branchId;
	private BigDecimal openingReserve;
	private BigDecimal currentReserve;
	private BigDecimal minThresholdPct;
	public String getBranchId() { return branchId; }
	public void setBranchId(String branchId) { this.branchId = branchId; }
	public BigDecimal getOpeningReserve() { return openingReserve; }
	public void setOpeningReserve(BigDecimal openingReserve) { this.openingReserve = openingReserve; }
	public BigDecimal getCurrentReserve() { return currentReserve; }
	public void setCurrentReserve(BigDecimal currentReserve) { this.currentReserve = currentReserve; }
	public BigDecimal getMinThresholdPct() { return minThresholdPct; }
	public void setMinThresholdPct(BigDecimal minThresholdPct) { this.minThresholdPct = minThresholdPct; }
}
