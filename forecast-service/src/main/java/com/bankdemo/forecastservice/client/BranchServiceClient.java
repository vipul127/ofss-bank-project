package com.bankdemo.forecastservice.client;

import com.bankdemo.forecastservice.dto.BranchDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "branch-service")
public interface BranchServiceClient {
	@GetMapping("/api/branches/{branchId}")
	BranchDto getBranch(@PathVariable("branchId") String branchId);
}
