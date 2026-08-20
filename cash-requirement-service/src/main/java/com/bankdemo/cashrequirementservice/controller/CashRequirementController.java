package com.bankdemo.cashrequirementservice.controller;

import com.bankdemo.cashrequirementservice.dto.*;
import com.bankdemo.cashrequirementservice.service.CashRequirementService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CashRequirementController {
	private final CashRequirementService service;
	public CashRequirementController(CashRequirementService service) { this.service = service; }
	@GetMapping("/cash-requirement/deficit-branches")
	public List<CashPositionDto> getDeficitBranches(@RequestHeader(value = "X-Branch-Role", required = false) String role) {
		List<CashPositionDto> deficits = service.getDeficitBranches();
		return role == null || role.isBlank() ? deficits : deficits.stream()
				.filter(position -> role.equalsIgnoreCase(position.getBranchId())).toList();
	}
	@GetMapping("/cash-requirement/suggest-source/{deficitBranchId}")
	public SuggestedSourceDto suggestSource(@PathVariable String deficitBranchId,
			@RequestHeader(value = "X-Branch-Role", required = false) String role) {
		checkRole(deficitBranchId, role);
		return service.suggestSourceBranch(deficitBranchId);
	}
	@GetMapping("/cash-requirement/nearby/{branchId}")
	public List<NearbyBranchDto> getNearby(@PathVariable String branchId, @RequestHeader(value = "X-Branch-Role", required = false) String role) {
		checkRole(branchId, role);
		return service.getNearbyBranches(branchId);
	}
	@PostMapping("/transfer-requests")
	public TransferRequestDto createRequest(@RequestBody TransferRequestDto request,
			@RequestHeader(value = "X-Branch-Role", required = false) String role) {
		// The logged-in branch is the requester/source. The destination branch authorizes later.
		checkRole(request.getSourceBranchId(), role);
		return service.createTransferRequest(request);
	}
	@GetMapping("/transfer-requests")
	public List<TransferRequestDto> getAllRequests(@RequestHeader(value = "X-Branch-Role", required = false) String role) {
		List<TransferRequestDto> requests = service.getAllRequests();
		return role == null || role.isBlank() ? requests : requests.stream()
				.filter(request -> role.equalsIgnoreCase(request.getDestinationBranchId()) || role.equalsIgnoreCase(request.getSourceBranchId())).toList();
	}
	@PutMapping("/transfer-requests/{requestId}/status")
	public TransferRequestDto updateStatus(@PathVariable String requestId, @RequestParam String status, @RequestHeader("X-Branch-Role") String role) {
		return service.updateStatus(requestId, status, role);
	}
	@DeleteMapping("/transfer-requests/{requestId}")
	public void revokeRequest(@PathVariable String requestId, @RequestHeader("X-Branch-Role") String role) {
		service.revokeRequest(requestId, role);
	}

	private void checkRole(String branchId, String role) {
		if (role != null && !role.isBlank() && !branchId.equalsIgnoreCase(role)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Branch role cannot access this branch");
		}
	}
}
