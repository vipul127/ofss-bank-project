package com.bankdemo.cashrequirementservice.service;

import com.bankdemo.cashrequirementservice.dto.*;
import java.util.List;

public interface CashRequirementService {
	List<CashPositionDto> getDeficitBranches();
	SuggestedSourceDto suggestSourceBranch(String deficitBranchId);
	TransferRequestDto createTransferRequest(TransferRequestDto request);
	List<TransferRequestDto> getAllRequests();
	TransferRequestDto updateStatus(String requestId, String status);
}
