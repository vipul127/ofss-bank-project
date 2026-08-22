package com.bankdemo.branchservice.service;

import com.bankdemo.branchservice.dto.BranchDto;
import com.bankdemo.branchservice.entity.Branch;
import com.bankdemo.branchservice.repository.BranchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
public class BranchServiceImpl implements BranchService {
	private final BranchRepository branchRepository;

	public BranchServiceImpl(BranchRepository branchRepository) {
		this.branchRepository = branchRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<BranchDto> getAllBranches() {
		return branchRepository.findAll().stream().map(this::toDto).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public BranchDto getBranch(String branchId) {
		return toDto(findBranch(branchId));
	}

	@Override
	@Transactional
	public BranchDto updateThreshold(String branchId, BigDecimal minThresholdPct) {
		if (minThresholdPct == null || minThresholdPct.signum() < 0 || minThresholdPct.compareTo(BigDecimal.valueOf(100)) > 0) {
			throw new IllegalArgumentException("minThresholdPct must be between 0 and 100");
		}
		Branch branch = findBranch(branchId);
		branch.setMinThresholdPct(minThresholdPct);
		return toDto(branchRepository.save(branch));
	}

	@Override
	@Transactional
	public BranchDto adjustReserve(String branchId, BigDecimal delta) {
		if (delta == null) {
			throw new IllegalArgumentException("delta is required");
		}
		Branch branch = findBranch(branchId);
		branch.setCurrentReserve(branch.getCurrentReserve().add(delta));
		return toDto(branchRepository.save(branch));
	}

	private Branch findBranch(String branchId) {
		return branchRepository.findById(branchId)
				.orElseThrow(() -> new IllegalArgumentException("Unknown branchId: " + branchId));
	}

	private BranchDto toDto(Branch branch) {
		BranchDto dto = new BranchDto();
		dto.setBranchId(branch.getBranchId());
		dto.setBranchName(branch.getBranchName());
		dto.setLatitude(branch.getLatitude());
		dto.setLongitude(branch.getLongitude());
		dto.setOpeningReserve(branch.getOpeningReserve());
		dto.setCurrentReserve(branch.getCurrentReserve());
		dto.setMinThresholdPct(branch.getMinThresholdPct());
		dto.setLoanBook(branch.getLoanBook());
		dto.setTotalAssets(branch.getTotalAssets());
		return dto;
	}
}
