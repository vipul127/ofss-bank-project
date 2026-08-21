package com.bankdemo.forecastservice.client;

import com.bankdemo.forecastservice.dto.CashTransactionDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "cash-management-service")
public interface CashManagementServiceClient {
	@GetMapping("/api/cash-transaction/{branchId}/recent")
	List<CashTransactionDto> getRecentTransactions(@PathVariable("branchId") String branchId);
	@GetMapping("/api/cash-transaction/{branchId}/since")
	List<CashTransactionDto> getTransactionsSince(@PathVariable("branchId") String branchId, @RequestParam("days") int days);
}
