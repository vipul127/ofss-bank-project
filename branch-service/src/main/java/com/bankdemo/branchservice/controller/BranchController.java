package com.bankdemo.branchservice.controller;

import com.bankdemo.branchservice.dto.BranchDto;
import com.bankdemo.branchservice.dto.ThresholdUpdateRequest;
import com.bankdemo.branchservice.service.BranchService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/branches")
public class BranchController {
	private final BranchService branchService;

	public BranchController(BranchService branchService) {
		this.branchService = branchService;
	}

	@GetMapping
	public List<BranchDto> getAllBranches(@RequestHeader(value = "X-Branch-Role", required = false) String role) {
		return role == null || role.isBlank() ? branchService.getAllBranches() : List.of(branchService.getBranch(role));
	}

	@GetMapping("/{branchId}")
	public BranchDto getBranch(@PathVariable String branchId,
			@RequestHeader(value = "X-Branch-Role", required = false) String role) {
		checkRole(branchId, role);
		return branchService.getBranch(branchId);
	}

	@PutMapping("/{branchId}/threshold")
	public BranchDto updateThreshold(@PathVariable String branchId, @RequestBody ThresholdUpdateRequest request,
			@RequestHeader(value = "X-Branch-Role", required = false) String role) {
		checkRole(branchId, role);
		return branchService.updateThreshold(branchId, request.getMinThresholdPct());
	}

	@PatchMapping("/{branchId}/reserve")
	public BranchDto adjustReserve(@PathVariable String branchId, @RequestBody BigDecimal delta) {
		return branchService.adjustReserve(branchId, delta);
	}

	private void checkRole(String branchId, String role) {
		if (role != null && !role.isBlank() && !branchId.equalsIgnoreCase(role)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Branch role cannot access this branch");
		}
	}
}
