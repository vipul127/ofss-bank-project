package com.bankdemo.cashrequirementservice.dto;

import java.math.BigDecimal;

public class NearbyBranchDto {
    private String branchId;
    private String branchName;
    private double distanceKm;
    private BigDecimal currentReserve;
    private BigDecimal surplusOrDeficitAmount;
    private String status;
    public String getBranchId() { return branchId; } public void setBranchId(String v) { branchId = v; }
    public String getBranchName() { return branchName; } public void setBranchName(String v) { branchName = v; }
    public double getDistanceKm() { return distanceKm; } public void setDistanceKm(double v) { distanceKm = v; }
    public BigDecimal getCurrentReserve() { return currentReserve; } public void setCurrentReserve(BigDecimal v) { currentReserve = v; }
    public BigDecimal getSurplusOrDeficitAmount() { return surplusOrDeficitAmount; } public void setSurplusOrDeficitAmount(BigDecimal v) { surplusOrDeficitAmount = v; }
    public String getStatus() { return status; } public void setStatus(String v) { status = v; }
}
