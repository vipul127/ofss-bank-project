package com.bankdemo.cashrequirementservice.service;

import com.bankdemo.cashrequirementservice.dto.BranchDto;
import com.bankdemo.cashrequirementservice.dto.CashPositionDto;
import org.springframework.stereotype.Component;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class NearestBranchLocator {
	private static final double EARTH_RADIUS_KM = 6371.0;
	public Optional<BranchDto> findNearestSurplusBranch(BranchDto deficitBranch, List<CashPositionDto> positions, List<BranchDto> branches) {
		return positions.stream().filter(position -> "SURPLUS".equals(position.getStatus()))
				.filter(position -> !position.getBranchId().equals(deficitBranch.getBranchId()))
				.map(position -> branches.stream().filter(branch -> branch.getBranchId().equals(position.getBranchId())).findFirst().orElse(null))
				.filter(java.util.Objects::nonNull)
				.min(Comparator.comparingDouble(candidate -> haversineKm(deficitBranch, candidate)));
	}
	private double haversineKm(BranchDto first, BranchDto second) {
		double dLat = Math.toRadians(second.getLatitude().doubleValue() - first.getLatitude().doubleValue());
		double dLon = Math.toRadians(second.getLongitude().doubleValue() - first.getLongitude().doubleValue());
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(first.getLatitude().doubleValue()))
				* Math.cos(Math.toRadians(second.getLatitude().doubleValue())) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
		return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
	}
}
