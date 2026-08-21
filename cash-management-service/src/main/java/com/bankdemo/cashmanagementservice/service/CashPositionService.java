package com.bankdemo.cashmanagementservice.service;

import com.bankdemo.cashmanagementservice.dto.CashPositionDto;
import com.bankdemo.cashmanagementservice.dto.CashTransactionDto;
import com.bankdemo.cashmanagementservice.dto.DailyCashAnalysisDto;
import java.math.BigDecimal;
import java.util.List;

public interface CashPositionService {
	CashPositionDto getPosition(String branchId);
	List<CashPositionDto> getAllPositions();
	CashTransactionDto recordTransaction(String branchId, String txnType, BigDecimal amount);
	List<CashTransactionDto> getRecentTransactions(String branchId, int limit);
	List<CashTransactionDto> getTransactionsSince(String branchId, int days);
	DailyCashAnalysisDto getDailyAnalysis(String branchId, int days);
}
