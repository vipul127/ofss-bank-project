package com.bankdemo.cashrequirementservice.dto;

public class CashPositionDto {
	private String branchId; private String status; private java.math.BigDecimal currentReserve; private java.math.BigDecimal surplusOrDeficitAmount;
	public String getBranchId() { return branchId; } public void setBranchId(String value) { branchId = value; }
	public String getStatus() { return status; } public void setStatus(String value) { status = value; }
	public java.math.BigDecimal getCurrentReserve() { return currentReserve; } public void setCurrentReserve(java.math.BigDecimal value) { currentReserve = value; }
	public java.math.BigDecimal getSurplusOrDeficitAmount() { return surplusOrDeficitAmount; } public void setSurplusOrDeficitAmount(java.math.BigDecimal value) { surplusOrDeficitAmount = value; }
}
