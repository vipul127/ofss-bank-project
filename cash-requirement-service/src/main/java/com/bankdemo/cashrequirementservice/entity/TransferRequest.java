package com.bankdemo.cashrequirementservice.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "TRANSFER_REQUEST")
public class TransferRequest {
	@Id @Column(name = "REQUEST_ID", length = 36) private String requestId;
	@Column(name = "SOURCE_BRANCH_ID", nullable = false, length = 10) private String sourceBranchId;
	@Column(name = "DESTINATION_BRANCH_ID", nullable = false, length = 10) private String destinationBranchId;
	@Column(name = "AMOUNT", nullable = false, precision = 15, scale = 2) private BigDecimal amount;
	@Column(name = "STATUS", nullable = false, length = 20) private String status;
	@Column(name = "REQUESTED_AT") private LocalDateTime requestedAt;
	@Column(name = "UPDATED_AT") private LocalDateTime updatedAt;
	public String getRequestId() { return requestId; }
	public void setRequestId(String requestId) { this.requestId = requestId; }
	public String getSourceBranchId() { return sourceBranchId; }
	public void setSourceBranchId(String sourceBranchId) { this.sourceBranchId = sourceBranchId; }
	public String getDestinationBranchId() { return destinationBranchId; }
	public void setDestinationBranchId(String destinationBranchId) { this.destinationBranchId = destinationBranchId; }
	public BigDecimal getAmount() { return amount; }
	public void setAmount(BigDecimal amount) { this.amount = amount; }
	public String getStatus() { return status; }
	public void setStatus(String status) { this.status = status; }
	public LocalDateTime getRequestedAt() { return requestedAt; }
	public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
	public LocalDateTime getUpdatedAt() { return updatedAt; }
	public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
