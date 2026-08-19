package com.bankdemo.cashrequirementservice.dto;

import java.math.BigDecimal;

public class BranchDto {
	private String branchId; private String branchName; private BigDecimal latitude; private BigDecimal longitude;
	public String getBranchId() { return branchId; } public void setBranchId(String value) { branchId = value; }
	public String getBranchName() { return branchName; } public void setBranchName(String value) { branchName = value; }
	public BigDecimal getLatitude() { return latitude; } public void setLatitude(BigDecimal value) { latitude = value; }
	public BigDecimal getLongitude() { return longitude; } public void setLongitude(BigDecimal value) { longitude = value; }
}
