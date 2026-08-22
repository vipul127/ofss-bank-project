package com.bankdemo.simulatorservice.job;

import com.bankdemo.simulatorservice.client.CashManagementServiceClient;
import com.bankdemo.simulatorservice.dto.CashTransactionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class TransactionSimulatorJob {
	private static final Logger log = LoggerFactory.getLogger(TransactionSimulatorJob.class);

	/** BR001/BR002 are deposit-biased so they trend toward surplus; BR003/BR004 are
	 * withdrawal-biased so they trend toward deficit against their threshold. */
	private record BranchProfile(String branchId, int depositProbabilityPct) {}
	private static final List<BranchProfile> BRANCHES = List.of(
			new BranchProfile("BR001", 80),
			new BranchProfile("BR002", 75),
			new BranchProfile("BR003", 25),
			new BranchProfile("BR004", 20));

	private final CashManagementServiceClient client;
	private final AtomicInteger cursor = new AtomicInteger(0);

	public TransactionSimulatorJob(CashManagementServiceClient client) { this.client = client; }

	@Scheduled(fixedDelay = 10000)
	public void generateTransaction() {
		// Round-robin, not random pick, so every branch gets a transaction in a fixed
		// sequence (BR001 -> BR002 -> BR003 -> BR004 -> BR001 -> ...) once per 10s tick.
		int index = Math.floorMod(cursor.getAndIncrement(), BRANCHES.size());
		BranchProfile profile = BRANCHES.get(index);
		boolean deposit = ThreadLocalRandom.current().nextInt(100) < profile.depositProbabilityPct();

		CashTransactionDto transaction = new CashTransactionDto();
		transaction.setBranchId(profile.branchId());
		transaction.setTxnType(deposit ? "DEPOSIT" : "WITHDRAWAL");
		transaction.setAmount(BigDecimal.valueOf(ThreadLocalRandom.current().nextInt(5000, 50000)));
		try {
			client.recordTransaction(transaction);
			log.info("[SIMULATOR] {} {} for branch {}", transaction.getTxnType(), transaction.getAmount(), profile.branchId());
		} catch (Exception e) {
			// Don't let one failed hop (service momentarily down, etc.) kill the schedule.
			log.warn("[SIMULATOR] failed to record {} for branch {}: {}", transaction.getTxnType(), profile.branchId(), e.getMessage());
		}
	}
}
