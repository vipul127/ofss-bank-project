package com.bankdemo.forecastservice.client;

import com.bankdemo.forecastservice.dto.CashTransactionDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;

@FeignClient(name = "cash-management-service")
public interface CashManagementServiceClient {
	@GetMapping("/api/cash-transaction/{branchId}/recent")
	List<CashTransactionDto> getRecentTransactions(@PathVariable("branchId") String branchId);
}
