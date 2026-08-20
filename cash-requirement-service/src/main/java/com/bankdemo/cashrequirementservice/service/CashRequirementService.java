package com.bankdemo.cashrequirementservice.service;

import com.bankdemo.cashrequirementservice.dto.*;
import java.util.List;

public interface CashRequirementService {
	List<CashPositionDto> getDeficitBranches();
	SuggestedSourceDto suggestSourceBranch(String deficitBranchId);
	List<NearbyBranchDto> getNearbyBranches(String branchId);
	TransferRequestDto createTransferRequest(TransferRequestDto request);
	List<TransferRequestDto> getAllRequests();
	TransferRequestDto updateStatus(String requestId, String status, String role);
	void revokeRequest(String requestId, String role);
}
