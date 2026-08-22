package com.bankdemo.cashmanagementservice.client;

import com.bankdemo.cashmanagementservice.dto.ForecastDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "forecast-service")
public interface ForecastServiceClient {
	@GetMapping("/api/forecast/{branchId}")
	ForecastDto getForecast(@PathVariable("branchId") String branchId);
}
