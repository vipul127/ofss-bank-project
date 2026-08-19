package com.bankdemo.branchservice.dto;

import java.math.BigDecimal;

public class ThresholdUpdateRequest {
	private BigDecimal minThresholdPct;
	public BigDecimal getMinThresholdPct() { return minThresholdPct; }
	public void setMinThresholdPct(BigDecimal minThresholdPct) { this.minThresholdPct = minThresholdPct; }
}
