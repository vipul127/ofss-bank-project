package com.bankdemo.forecastservice.repository;

import com.bankdemo.forecastservice.entity.ForecastSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;

public interface ForecastSnapshotRepository extends JpaRepository<ForecastSnapshot, String> {
	Optional<ForecastSnapshot> findByBranchIdAndForecastDate(String branchId, LocalDate forecastDate);
}
