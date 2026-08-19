package com.bankdemo.cashmanagementservice.dto;

import java.math.BigDecimal;

public class CashPositionDto {
	private String branchId;
	private BigDecimal currentReserve;
	private BigDecimal thresholdAmount;
	private String status;
	private BigDecimal surplusOrDeficitAmount;
	public String getBranchId() { return branchId; }
	public void setBranchId(String branchId) { this.branchId = branchId; }
	public BigDecimal getCurrentReserve() { return currentReserve; }
	public void setCurrentReserve(BigDecimal currentReserve) { this.currentReserve = currentReserve; }
	public BigDecimal getThresholdAmount() { return thresholdAmount; }
	public void setThresholdAmount(BigDecimal thresholdAmount) { this.thresholdAmount = thresholdAmount; }
	public String getStatus() { return status; }
	public void setStatus(String status) { this.status = status; }
	public BigDecimal getSurplusOrDeficitAmount() { return surplusOrDeficitAmount; }
	public void setSurplusOrDeficitAmount(BigDecimal surplusOrDeficitAmount) { this.surplusOrDeficitAmount = surplusOrDeficitAmount; }
}
