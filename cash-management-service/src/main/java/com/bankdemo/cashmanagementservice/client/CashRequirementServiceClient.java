package com.bankdemo.cashmanagementservice.client;

import com.bankdemo.cashmanagementservice.dto.CashPositionDto;
import com.bankdemo.cashmanagementservice.dto.SuggestedSourceDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;

@FeignClient(name = "cash-requirement-service")
public interface CashRequirementServiceClient {
	@GetMapping("/api/cash-requirement/suggest-source/{deficitBranchId}")
	SuggestedSourceDto suggestSource(@PathVariable("deficitBranchId") String deficitBranchId);

	@GetMapping("/api/cash-requirement/deficit-branches")
	List<CashPositionDto> getDeficitBranches();
}
