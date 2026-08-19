package com.bankdemo.cashmanagementservice.controller;

import com.bankdemo.cashmanagementservice.dto.CashPositionDto;
import com.bankdemo.cashmanagementservice.dto.CashTransactionDto;
import com.bankdemo.cashmanagementservice.service.CashPositionService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CashPositionController {
	private final CashPositionService cashPositionService;

	public CashPositionController(CashPositionService cashPositionService) { this.cashPositionService = cashPositionService; }

	@GetMapping("/cash-position")
	public List<CashPositionDto> getAllPositions(@RequestHeader(value = "X-Branch-Role", required = false) String role) {
		List<CashPositionDto> positions = cashPositionService.getAllPositions();
		return role == null || role.isBlank() ? positions : positions.stream()
				.filter(position -> role.equalsIgnoreCase(position.getBranchId())).toList();
	}

	@GetMapping("/cash-position/{branchId}")
	public CashPositionDto getPosition(@PathVariable String branchId,
			@RequestHeader(value = "X-Branch-Role", required = false) String role) {
		checkRole(branchId, role);
		return cashPositionService.getPosition(branchId);
	}

	@PostMapping("/cash-transaction")
	public CashTransactionDto recordTransaction(@RequestBody CashTransactionDto request,
			@RequestHeader(value = "X-Branch-Role", required = false) String role) {
		checkRole(request.getBranchId(), role);
		return cashPositionService.recordTransaction(request.getBranchId(), request.getTxnType(), request.getAmount());
	}

	@GetMapping("/cash-transaction/{branchId}/recent")
	public List<CashTransactionDto> getRecentTransactions(@PathVariable String branchId,
			@RequestHeader(value = "X-Branch-Role", required = false) String role) {
		checkRole(branchId, role);
		return cashPositionService.getRecentTransactions(branchId, 20);
	}

	private void checkRole(String branchId, String role) {
		if (role != null && !role.isBlank() && !branchId.equalsIgnoreCase(role)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Branch role cannot access this branch");
		}
	}
}
