package com.bankdemo.cashrequirementservice.client;

import com.bankdemo.cashrequirementservice.dto.BranchDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;

@FeignClient(name = "branch-service")
public interface BranchServiceClient {
	@GetMapping("/api/branches/{branchId}") BranchDto getBranch(@PathVariable("branchId") String branchId);
	@GetMapping("/api/branches") List<BranchDto> getAllBranches();
}
