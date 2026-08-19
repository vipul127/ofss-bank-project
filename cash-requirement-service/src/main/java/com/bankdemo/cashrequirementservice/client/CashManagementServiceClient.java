package com.bankdemo.cashrequirementservice.client;

import com.bankdemo.cashrequirementservice.dto.CashPositionDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

@FeignClient(name = "cash-management-service")
public interface CashManagementServiceClient {
	@GetMapping("/api/cash-position") List<CashPositionDto> getAllPositions();
}
