package com.bankdemo.forecastservice.service;

import com.bankdemo.forecastservice.dto.ForecastDto;

public interface ForecastService {
	ForecastDto getOrComputeForecast(String branchId);
}
