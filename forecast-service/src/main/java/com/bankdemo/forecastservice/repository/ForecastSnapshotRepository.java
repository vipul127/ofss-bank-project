package com.bankdemo.forecastservice.repository;

import com.bankdemo.forecastservice.entity.ForecastSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface ForecastSnapshotRepository extends JpaRepository<ForecastSnapshot, String> {
	List<ForecastSnapshot> findByBranchIdAndForecastDateOrderByForecastIdDesc(String branchId, LocalDate forecastDate);
}
