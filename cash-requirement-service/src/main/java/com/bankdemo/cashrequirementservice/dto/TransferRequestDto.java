package com.bankdemo.cashrequirementservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransferRequestDto {
	private String requestId; private String sourceBranchId; private String destinationBranchId; private BigDecimal amount; private String status; private LocalDateTime requestedAt; private LocalDateTime updatedAt;
	public String getRequestId() { return requestId; } public void setRequestId(String value) { requestId = value; }
	public String getSourceBranchId() { return sourceBranchId; } public void setSourceBranchId(String value) { sourceBranchId = value; }
	public String getDestinationBranchId() { return destinationBranchId; } public void setDestinationBranchId(String value) { destinationBranchId = value; }
	public BigDecimal getAmount() { return amount; } public void setAmount(BigDecimal value) { amount = value; }
	public String getStatus() { return status; } public void setStatus(String value) { status = value; }
	public LocalDateTime getRequestedAt() { return requestedAt; } public void setRequestedAt(LocalDateTime value) { requestedAt = value; }
	public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime value) { updatedAt = value; }
}
