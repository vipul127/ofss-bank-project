package com.bankdemo.cashmanagementservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "CASH_TRANSACTION")
public class CashTransaction {
	@Id
	@Column(name = "TRANSACTION_ID", length = 36)
	private String transactionId;
	@Column(name = "BRANCH_ID", nullable = false, length = 10)
	private String branchId;
	@Column(name = "TXN_TYPE", nullable = false, length = 20)
	private String txnType;
	@Column(name = "AMOUNT", nullable = false, precision = 15, scale = 2)
	private BigDecimal amount;
	@Column(name = "EVENT_TIMESTAMP", nullable = false)
	private LocalDateTime eventTimestamp;

	public String getTransactionId() { return transactionId; }
	public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
	public String getBranchId() { return branchId; }
	public void setBranchId(String branchId) { this.branchId = branchId; }
	public String getTxnType() { return txnType; }
	public void setTxnType(String txnType) { this.txnType = txnType; }
	public BigDecimal getAmount() { return amount; }
	public void setAmount(BigDecimal amount) { this.amount = amount; }
	public LocalDateTime getEventTimestamp() { return eventTimestamp; }
	public void setEventTimestamp(LocalDateTime eventTimestamp) { this.eventTimestamp = eventTimestamp; }
}
