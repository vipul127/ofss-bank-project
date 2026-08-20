package com.bankdemo.cashrequirementservice.service;

import com.bankdemo.cashrequirementservice.client.*;
import com.bankdemo.cashrequirementservice.dto.*;
import com.bankdemo.cashrequirementservice.entity.TransferRequest;
import com.bankdemo.cashrequirementservice.repository.TransferRequestRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class CashRequirementServiceImpl implements CashRequirementService {
	private static final List<String> STATUSES = List.of("REQUESTED", "APPROVED", "DISPATCHED", "DELIVERED");
	private final TransferRequestRepository repository;
	private final BranchServiceClient branchClient;
	private final CashManagementServiceClient cashClient;
	private final NearestBranchLocator locator;
	public CashRequirementServiceImpl(TransferRequestRepository repository, BranchServiceClient branchClient,
			CashManagementServiceClient cashClient, NearestBranchLocator locator) {
		this.repository = repository; this.branchClient = branchClient; this.cashClient = cashClient; this.locator = locator;
	}
	public List<CashPositionDto> getDeficitBranches() { return cashClient.getAllPositions().stream().filter(p -> "DEFICIT".equals(p.getStatus())).toList(); }
	public SuggestedSourceDto suggestSourceBranch(String deficitBranchId) {
		BranchDto deficit = branchClient.getBranch(deficitBranchId);
		BranchDto source = locator.findNearestSurplusBranch(deficit, cashClient.getAllPositions(), branchClient.getAllBranches())
				.orElseThrow(() -> new IllegalArgumentException("No surplus source branch available"));
		SuggestedSourceDto dto = new SuggestedSourceDto(); dto.setBranchId(source.getBranchId()); dto.setBranchName(source.getBranchName()); return dto;
	}
	public List<NearbyBranchDto> getNearbyBranches(String branchId) {
		BranchDto current = branchClient.getBranch(branchId);
		List<CashPositionDto> positions = cashClient.getAllPositions();
		return branchClient.getAllBranches().stream().filter(b -> !b.getBranchId().equalsIgnoreCase(branchId)).map(branch -> {
			CashPositionDto position = positions.stream().filter(p -> p.getBranchId().equals(branch.getBranchId())).findFirst().orElse(null);
			NearbyBranchDto dto = new NearbyBranchDto(); dto.setBranchId(branch.getBranchId()); dto.setBranchName(branch.getBranchName()); dto.setDistanceKm(distance(current, branch));
			if (position != null) { dto.setStatus(position.getStatus()); dto.setCurrentReserve(position.getCurrentReserve()); dto.setSurplusOrDeficitAmount(position.getSurplusOrDeficitAmount()); }
			return dto;
		}).sorted(java.util.Comparator.comparingDouble(NearbyBranchDto::getDistanceKm)).toList();
	}
	private double distance(BranchDto a, BranchDto b) {
		double lat = Math.toRadians(b.getLatitude().doubleValue() - a.getLatitude().doubleValue());
		double lon = Math.toRadians(b.getLongitude().doubleValue() - a.getLongitude().doubleValue());
		double x = Math.sin(lat / 2) * Math.sin(lat / 2) + Math.cos(Math.toRadians(a.getLatitude().doubleValue())) * Math.cos(Math.toRadians(b.getLatitude().doubleValue())) * Math.sin(lon / 2) * Math.sin(lon / 2);
		return 6371 * 2 * Math.atan2(Math.sqrt(x), Math.sqrt(1 - x));
	}
	public TransferRequestDto createTransferRequest(TransferRequestDto request) {
		if (request.getAmount() == null || request.getAmount().signum() <= 0 || request.getSourceBranchId() == null || request.getDestinationBranchId() == null) throw new IllegalArgumentException("source, destination, and positive amount are required");
		if (request.getSourceBranchId().equals(request.getDestinationBranchId())) throw new IllegalArgumentException("source and destination must differ");
		branchClient.getBranch(request.getSourceBranchId()); branchClient.getBranch(request.getDestinationBranchId());
		TransferRequest entity = new TransferRequest(); entity.setRequestId(UUID.randomUUID().toString()); entity.setSourceBranchId(request.getSourceBranchId()); entity.setDestinationBranchId(request.getDestinationBranchId()); entity.setAmount(request.getAmount()); entity.setStatus("REQUESTED"); entity.setRequestedAt(LocalDateTime.now()); entity.setUpdatedAt(entity.getRequestedAt());
		return toDto(repository.save(entity));
	}
	public List<TransferRequestDto> getAllRequests() { return repository.findAll().stream().map(this::toDto).toList(); }
	public void revokeRequest(String requestId, String role) {
		TransferRequest entity = repository.findById(requestId).orElseThrow(() -> new IllegalArgumentException("Unknown requestId: " + requestId));
		boolean participant = entity.getSourceBranchId().equalsIgnoreCase(role) || entity.getDestinationBranchId().equalsIgnoreCase(role);
		if (!participant || !"REQUESTED".equals(entity.getStatus())) throw new IllegalArgumentException("Only a participating branch can revoke a requested transfer");
		repository.delete(entity);
	}
	public TransferRequestDto updateStatus(String requestId, String status, String role) {
		int target = STATUSES.indexOf(status); if (target < 0) throw new IllegalArgumentException("Unknown transfer status");
		TransferRequest entity = repository.findById(requestId).orElseThrow(() -> new IllegalArgumentException("Unknown requestId: " + requestId));
		if (role != null && !role.isBlank() && !entity.getDestinationBranchId().equalsIgnoreCase(role)) throw new IllegalArgumentException("Only the receiving branch can authorize a transfer");
		int current = STATUSES.indexOf(entity.getStatus()); if (target != current + 1) throw new IllegalArgumentException("Transfer status must advance one state at a time");
		entity.setStatus(status); entity.setUpdatedAt(LocalDateTime.now()); return toDto(repository.save(entity));
	}
	private TransferRequestDto toDto(TransferRequest entity) { TransferRequestDto dto = new TransferRequestDto(); dto.setRequestId(entity.getRequestId()); dto.setSourceBranchId(entity.getSourceBranchId()); dto.setDestinationBranchId(entity.getDestinationBranchId()); dto.setAmount(entity.getAmount()); dto.setStatus(entity.getStatus()); dto.setRequestedAt(entity.getRequestedAt()); dto.setUpdatedAt(entity.getUpdatedAt()); return dto; }
}
