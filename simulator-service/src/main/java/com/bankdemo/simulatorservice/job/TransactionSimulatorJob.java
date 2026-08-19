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

@Component
public class TransactionSimulatorJob {
	private static final Logger log = LoggerFactory.getLogger(TransactionSimulatorJob.class);
	private static final List<String> BRANCHES = List.of("BR001", "BR002", "BR003", "BR004");
	private final CashManagementServiceClient client;
	public TransactionSimulatorJob(CashManagementServiceClient client) { this.client = client; }
	@Scheduled(fixedDelay = 5000)
	public void generateTransaction() {
		String branchId = BRANCHES.get(ThreadLocalRandom.current().nextInt(BRANCHES.size()));
		boolean deposit = ThreadLocalRandom.current().nextInt(100) < ("BR004".equals(branchId) ? 30 : 70);
		CashTransactionDto transaction = new CashTransactionDto();
		transaction.setBranchId(branchId); transaction.setTxnType(deposit ? "DEPOSIT" : "WITHDRAWAL"); transaction.setAmount(BigDecimal.valueOf(ThreadLocalRandom.current().nextInt(5000, 50000)));
		client.recordTransaction(transaction);
		log.info("[SIMULATOR] {} {} for branch {}", transaction.getTxnType(), transaction.getAmount(), branchId);
	}
}
