package com.bankdemo.forecastservice.service;

import com.bankdemo.forecastservice.client.BranchServiceClient;
import com.bankdemo.forecastservice.client.CashManagementServiceClient;
import com.bankdemo.forecastservice.dto.BranchDto;
import com.bankdemo.forecastservice.dto.CashTransactionDto;
import com.bankdemo.forecastservice.dto.ForecastDto;
import com.bankdemo.forecastservice.entity.ForecastSnapshot;
import com.bankdemo.forecastservice.repository.ForecastSnapshotRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ForecastServiceImpl implements ForecastService {
	private final ForecastCalculator calculator;
	private final ForecastSnapshotRepository snapshotRepository;
	private final BranchServiceClient branchServiceClient;
	private final CashManagementServiceClient cashManagementServiceClient;

	public ForecastServiceImpl(ForecastCalculator calculator, ForecastSnapshotRepository snapshotRepository,
			BranchServiceClient branchServiceClient, CashManagementServiceClient cashManagementServiceClient) {
		this.calculator = calculator;
		this.snapshotRepository = snapshotRepository;
		this.branchServiceClient = branchServiceClient;
		this.cashManagementServiceClient = cashManagementServiceClient;
	}

	@Override
	public ForecastDto getOrComputeForecast(String branchId) {
		LocalDate targetDate = LocalDate.now().plusDays(1);
		BranchDto branch = branchServiceClient.getBranch(branchId);
		BigDecimal threshold = branch.getCurrentReserve().multiply(branch.getMinThresholdPct()).divide(BigDecimal.valueOf(100));
		List<ForecastCalculator.DailyPosition> positions = dailyPositions(cashManagementServiceClient.getRecentTransactions(branchId));
		ForecastCalculator.ForecastResult result = calculator.forecastNextDay(positions, targetDate.getDayOfWeek(), threshold);
		// Legacy/demo data may contain duplicate snapshots for the same branch/date.
		// Select one deterministically instead of relying on a unique-result query.
		ForecastSnapshot snapshot = snapshotRepository
				.findByBranchIdAndForecastDateOrderByForecastIdDesc(branchId, targetDate)
				.stream().findFirst().orElseGet(ForecastSnapshot::new);
		snapshot.setForecastId(snapshot.getForecastId() == null ? UUID.randomUUID().toString() : snapshot.getForecastId());
		snapshot.setBranchId(branchId);
		snapshot.setForecastDate(targetDate);
		snapshot.setPredictedPosition(result.predictedPosition());
		snapshot.setConfidenceBandLow(result.bandLow());
		snapshot.setConfidenceBandHigh(result.bandHigh());
		snapshotRepository.save(snapshot);
		ForecastDto dto = new ForecastDto();
		dto.setBranchId(branchId);
		dto.setForecastDate(targetDate);
		dto.setPredictedPosition(result.predictedPosition());
		dto.setConfidenceBandLow(result.bandLow());
		dto.setConfidenceBandHigh(result.bandHigh());
		dto.setCurrentPosition(branch.getCurrentReserve());
		dto.setAtRisk(result.atRisk());
		return dto;
	}

	private List<ForecastCalculator.DailyPosition> dailyPositions(List<CashTransactionDto> transactions) {
		List<ForecastCalculator.DailyPosition> result = new ArrayList<>();
		for (CashTransactionDto transaction : transactions) {
			BigDecimal net = "DEPOSIT".equals(transaction.getTxnType()) ? transaction.getAmount() : transaction.getAmount().negate();
			result.add(new ForecastCalculator.DailyPosition(transaction.getEventTimestamp().toLocalDate(), net));
		}
		return result;
	}
}
