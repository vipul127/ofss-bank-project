package com.bankdemo.cashmanagementservice.service;

import com.bankdemo.cashmanagementservice.dto.CashPositionDto;
import com.bankdemo.cashmanagementservice.dto.CashTransactionDto;
import java.math.BigDecimal;
import java.util.List;

public interface CashPositionService {
	CashPositionDto getPosition(String branchId);
	List<CashPositionDto> getAllPositions();
	CashTransactionDto recordTransaction(String branchId, String txnType, BigDecimal amount);
	List<CashTransactionDto> getRecentTransactions(String branchId, int limit);
}
