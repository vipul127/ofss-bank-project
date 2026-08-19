package com.bankdemo.simulatorservice.client;

import com.bankdemo.simulatorservice.dto.CashTransactionDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "cash-management-service")
public interface CashManagementServiceClient {
	@PostMapping("/api/cash-transaction")
	void recordTransaction(@RequestBody CashTransactionDto request);
}
