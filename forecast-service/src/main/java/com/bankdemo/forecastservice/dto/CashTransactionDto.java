package com.bankdemo.forecastservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CashTransactionDto {
	private String branchId;
	private String txnType;
	private BigDecimal amount;
	private LocalDateTime eventTimestamp;
	public String getBranchId() { return branchId; }
	public void setBranchId(String branchId) { this.branchId = branchId; }
	public String getTxnType() { return txnType; }
	public void setTxnType(String txnType) { this.txnType = txnType; }
	public BigDecimal getAmount() { return amount; }
	public void setAmount(BigDecimal amount) { this.amount = amount; }
	public LocalDateTime getEventTimestamp() { return eventTimestamp; }
	public void setEventTimestamp(LocalDateTime eventTimestamp) { this.eventTimestamp = eventTimestamp; }
}
