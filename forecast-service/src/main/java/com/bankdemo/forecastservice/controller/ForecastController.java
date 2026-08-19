package com.bankdemo.forecastservice.controller;

import com.bankdemo.forecastservice.dto.ForecastDto;
import com.bankdemo.forecastservice.service.ForecastService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/forecast")
public class ForecastController {
	private final ForecastService forecastService;
	public ForecastController(ForecastService forecastService) { this.forecastService = forecastService; }
	@GetMapping("/{branchId}")
	public ForecastDto getForecast(@PathVariable String branchId,
			@RequestHeader(value = "X-Branch-Role", required = false) String role) {
		if (role != null && !role.isBlank() && !branchId.equalsIgnoreCase(role)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Branch role cannot access this branch");
		}
		return forecastService.getOrComputeForecast(branchId);
	}
}
