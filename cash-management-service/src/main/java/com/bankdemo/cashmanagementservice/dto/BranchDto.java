package com.bankdemo.cashmanagementservice.dto;

import java.math.BigDecimal;

public class BranchDto {
	private String branchId;
	private String branchName;
	private BigDecimal latitude;
	private BigDecimal longitude;
	private BigDecimal openingReserve;
	private BigDecimal currentReserve;
	private BigDecimal minThresholdPct;
	public String getBranchId() { return branchId; }
	public void setBranchId(String branchId) { this.branchId = branchId; }
	public String getBranchName() { return branchName; }
	public void setBranchName(String branchName) { this.branchName = branchName; }
	public BigDecimal getLatitude() { return latitude; }
	public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
	public BigDecimal getLongitude() { return longitude; }
	public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
	public BigDecimal getOpeningReserve() { return openingReserve; }
	public void setOpeningReserve(BigDecimal openingReserve) { this.openingReserve = openingReserve; }
	public BigDecimal getCurrentReserve() { return currentReserve; }
	public void setCurrentReserve(BigDecimal currentReserve) { this.currentReserve = currentReserve; }
	public BigDecimal getMinThresholdPct() { return minThresholdPct; }
	public void setMinThresholdPct(BigDecimal minThresholdPct) { this.minThresholdPct = minThresholdPct; }
}
