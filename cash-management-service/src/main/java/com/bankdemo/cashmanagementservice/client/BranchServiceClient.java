package com.bankdemo.cashmanagementservice.client;

import com.bankdemo.cashmanagementservice.dto.BranchDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.math.BigDecimal;
import java.util.List;

@FeignClient(name = "branch-service")
public interface BranchServiceClient {
	@GetMapping("/api/branches/{branchId}")
	BranchDto getBranch(@PathVariable("branchId") String branchId);

	@GetMapping("/api/branches")
	List<BranchDto> getAllBranches();

	// PUT, matching branch-service's BranchController — see the comment there for why not PATCH.
	@PutMapping("/api/branches/{branchId}/reserve")
	BranchDto adjustReserve(@PathVariable("branchId") String branchId, @RequestBody BigDecimal delta);
}
