package com.bankdemo.cashmanagementservice.service;

import com.bankdemo.cashmanagementservice.client.BranchServiceClient;
import com.bankdemo.cashmanagementservice.dto.BranchDto;
import com.bankdemo.cashmanagementservice.dto.CashPositionDto;
import com.bankdemo.cashmanagementservice.dto.CashTransactionDto;
import com.bankdemo.cashmanagementservice.dto.DailyCashAnalysisDto;
import com.bankdemo.cashmanagementservice.entity.CashTransaction;
import com.bankdemo.cashmanagementservice.repository.CashTransactionRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;
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

	@Override
	public DailyCashAnalysisDto getDailyAnalysis(String branchId, int days) {
		BigDecimal currentReserve = branchServiceClient.getBranch(branchId).getCurrentReserve();
		List<CashTransaction> transactions = transactionRepository.findByBranchIdOrderByEventTimestampDesc(branchId);
		Map<LocalDate, BigDecimal> movementByDay = transactions.stream().collect(Collectors.groupingBy(
			t -> t.getEventTimestamp().toLocalDate(), Collectors.reducing(BigDecimal.ZERO,
				t -> "WITHDRAWAL".equals(t.getTxnType()) ? t.getAmount().negate() : t.getAmount(), BigDecimal::add)));
		LocalDate today = LocalDate.now();
		LocalDate firstDay = today.minusDays(days - 1L);
		BigDecimal totalWindowMovement = movementByDay.entrySet().stream()
				.filter(e -> !e.getKey().isBefore(firstDay) && !e.getKey().isAfter(today))
				.map(Map.Entry::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal running = currentReserve.subtract(totalWindowMovement);
		BigDecimal todayDeposits = transactions.stream().filter(t -> t.getEventTimestamp().toLocalDate().equals(today) && "DEPOSIT".equals(t.getTxnType())).map(CashTransaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal todayWithdrawals = transactions.stream().filter(t -> t.getEventTimestamp().toLocalDate().equals(today) && "WITHDRAWAL".equals(t.getTxnType())).map(CashTransaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal todayNet = todayDeposits.subtract(todayWithdrawals);
		List<DailyCashAnalysisDto.DailyPoint> points = new ArrayList<>();
		List<BigDecimal> nets = new ArrayList<>();
		for (int i = 0; i < days; i++) {
			LocalDate date = firstDay.plusDays(i);
			BigDecimal net = movementByDay.getOrDefault(date, BigDecimal.ZERO);
			running = running.add(net);
			points.add(new DailyCashAnalysisDto.DailyPoint(date, net, running));
			nets.add(net);
		}
		double mean = nets.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
		double variance = nets.size() < 2 ? 0 : nets.stream().mapToDouble(v -> Math.pow(v.doubleValue() - mean, 2)).sum() / (nets.size() - 1);
		BigDecimal averageSurplus = nets.stream().filter(v -> v.signum() > 0).reduce(BigDecimal.ZERO, BigDecimal::add)
				.divide(BigDecimal.valueOf(Math.max(1, nets.stream().filter(v -> v.signum() > 0).count())), 2, java.math.RoundingMode.HALF_UP);
		BigDecimal averageDeficit = nets.stream().filter(v -> v.signum() < 0).map(BigDecimal::abs).reduce(BigDecimal.ZERO, BigDecimal::add)
				.divide(BigDecimal.valueOf(Math.max(1, nets.stream().filter(v -> v.signum() < 0).count())), 2, java.math.RoundingMode.HALF_UP);
		BigDecimal largestWithdrawal = transactions.stream().filter(t -> "WITHDRAWAL".equals(t.getTxnType()))
				.map(CashTransaction::getAmount).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
		DailyCashAnalysisDto dto = new DailyCashAnalysisDto();
		dto.setDailyPoints(points);
		dto.setAverageDailyNet(BigDecimal.valueOf(mean).setScale(2, java.math.RoundingMode.HALF_UP));
		dto.setStandardDeviation(BigDecimal.valueOf(Math.sqrt(variance)).setScale(2, java.math.RoundingMode.HALF_UP));
		dto.setAverageSurplus(averageSurplus);
		dto.setAverageDeficit(averageDeficit);
		dto.setLargestWithdrawal(largestWithdrawal);
		dto.setWithdrawalImpactPercent(currentReserve.signum() == 0 ? BigDecimal.ZERO : largestWithdrawal.multiply(BigDecimal.valueOf(100)).divide(currentReserve, 2, java.math.RoundingMode.HALF_UP));
		dto.setTodayDeposits(todayDeposits);
		dto.setTodayWithdrawals(todayWithdrawals);
		dto.setTodayNet(todayNet);
		dto.setReserveBeforeToday(currentReserve.subtract(todayNet));
		BigDecimal average = dto.getAverageDailyNet();
		dto.setTodayVsAverageAmount(todayNet.subtract(average));
		dto.setTodayVsAveragePercent(average.signum() == 0 ? BigDecimal.ZERO : todayNet.subtract(average).abs().multiply(BigDecimal.valueOf(100)).divide(average.abs(), 2, java.math.RoundingMode.HALF_UP));
		return dto;
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
