package com.bankdemo.branchservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "BRANCH")
public class Branch {
	@Id
	@Column(name = "BRANCH_ID", length = 10)
	private String branchId;
	@Column(name = "BRANCH_NAME", nullable = false, length = 100)
	private String branchName;
	@Column(name = "LATITUDE", nullable = false, precision = 9, scale = 6)
	private BigDecimal latitude;
	@Column(name = "LONGITUDE", nullable = false, precision = 9, scale = 6)
	private BigDecimal longitude;
	@Column(name = "OPENING_RESERVE", nullable = false, precision = 15, scale = 2)
	private BigDecimal openingReserve;
	@Column(name = "CURRENT_RESERVE", nullable = false, precision = 15, scale = 2)
	private BigDecimal currentReserve;
	@Column(name = "MIN_THRESHOLD_PCT", nullable = false, precision = 5, scale = 2)
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
