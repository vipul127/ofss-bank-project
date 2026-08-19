package com.bankdemo.branchservice.service;

import com.bankdemo.branchservice.dto.BranchDto;
import java.math.BigDecimal;
import java.util.List;

public interface BranchService {
	List<BranchDto> getAllBranches();
	BranchDto getBranch(String branchId);
	BranchDto updateThreshold(String branchId, BigDecimal minThresholdPct);
	BranchDto adjustReserve(String branchId, BigDecimal delta);
}
