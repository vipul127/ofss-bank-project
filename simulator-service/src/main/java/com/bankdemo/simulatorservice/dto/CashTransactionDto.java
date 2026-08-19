package com.bankdemo.simulatorservice.dto;

import java.math.BigDecimal;

public class CashTransactionDto {
	private String branchId; private String txnType; private BigDecimal amount;
	public String getBranchId() { return branchId; } public void setBranchId(String value) { branchId = value; }
	public String getTxnType() { return txnType; } public void setTxnType(String value) { txnType = value; }
	public BigDecimal getAmount() { return amount; } public void setAmount(BigDecimal value) { amount = value; }
}
