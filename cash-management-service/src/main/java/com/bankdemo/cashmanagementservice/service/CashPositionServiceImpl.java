package com.bankdemo.cashmanagementservice.service;

import com.bankdemo.cashmanagementservice.client.BranchServiceClient;
import com.bankdemo.cashmanagementservice.dto.BranchDto;
import com.bankdemo.cashmanagementservice.dto.CashPositionDto;
import com.bankdemo.cashmanagementservice.dto.CashTransactionDto;
import com.bankdemo.cashmanagementservice.entity.CashTransaction;
import com.bankdemo.cashmanagementservice.repository.CashTransactionRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CashPositionServiceImpl implements CashPositionService {
	private final CashTransactionRepository transactionRepository;
	private final BranchServiceClient branchServiceClient;

	public CashPositionServiceImpl(CashTransactionRepository transactionRepository, BranchServiceClient branchServiceClient) {
		this.transactionRepository = transactionRepository;
		this.branchServiceClient = branchServiceClient;
	}

	@Override
	public CashPositionDto getPosition(String branchId) {
		BranchDto branch = branchServiceClient.getBranch(branchId);
		BigDecimal threshold = branch.getCurrentReserve().multiply(branch.getMinThresholdPct())
				.divide(BigDecimal.valueOf(100));
		CashPositionDto position = new CashPositionDto();
		position.setBranchId(branchId);
		position.setCurrentReserve(branch.getCurrentReserve());
		position.setThresholdAmount(threshold);
		position.setStatus(branch.getCurrentReserve().compareTo(threshold) < 0 ? "DEFICIT" : "SURPLUS");
		position.setSurplusOrDeficitAmount(branch.getCurrentReserve().subtract(threshold));
		return position;
	}

	@Override
	public List<CashPositionDto> getAllPositions() {
		return branchServiceClient.getAllBranches().stream().map(branch -> getPosition(branch.getBranchId())).toList();
	}

	@Override
	public CashTransactionDto recordTransaction(String branchId, String txnType, BigDecimal amount) {
		if (amount == null || amount.signum() <= 0 || !("DEPOSIT".equals(txnType) || "WITHDRAWAL".equals(txnType))) {
			throw new IllegalArgumentException("txnType must be DEPOSIT or WITHDRAWAL and amount must be positive");
		}
		CashTransaction transaction = new CashTransaction();
		transaction.setTransactionId(UUID.randomUUID().toString());
		transaction.setBranchId(branchId);
		transaction.setTxnType(txnType);
		transaction.setAmount(amount);
		transaction.setEventTimestamp(LocalDateTime.now());
		CashTransaction saved = transactionRepository.save(transaction);
		branchServiceClient.adjustReserve(branchId, "DEPOSIT".equals(txnType) ? amount : amount.negate());
		return toDto(saved);
	}

	@Override
	public List<CashTransactionDto> getRecentTransactions(String branchId, int limit) {
		return transactionRepository.findByBranchIdOrderByEventTimestampDesc(branchId).stream()
				.limit(Math.max(0, limit)).map(this::toDto).toList();
	}

	private CashTransactionDto toDto(CashTransaction transaction) {
		CashTransactionDto dto = new CashTransactionDto();
		dto.setTransactionId(transaction.getTransactionId());
		dto.setBranchId(transaction.getBranchId());
		dto.setTxnType(transaction.getTxnType());
		dto.setAmount(transaction.getAmount());
		dto.setEventTimestamp(transaction.getEventTimestamp());
		return dto;
	}
}
