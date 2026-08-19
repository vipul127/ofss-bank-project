package com.bankdemo.forecastservice.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ForecastCalculator {
	public ForecastResult forecastNextDay(List<DailyPosition> positions, DayOfWeek targetDay, BigDecimal thresholdAmount) {
		double[] values = positions.stream().mapToDouble(position -> position.netPosition().doubleValue()).toArray();
		double mean = mean(values);
		double stdDev = stdDev(values, mean);
		Map<DayOfWeek, List<Double>> byDay = positions.stream().collect(Collectors.groupingBy(
				position -> position.date().getDayOfWeek(),
				Collectors.mapping(position -> position.netPosition().doubleValue(), Collectors.toList())));
		double weekdayAdjustment = byDay.containsKey(targetDay)
				? mean(byDay.get(targetDay).stream().mapToDouble(Double::doubleValue).toArray()) - mean : 0;
		double predicted = mean + weekdayAdjustment + linearTrendSlope(values);
		double low = predicted - stdDev;
		double high = predicted + stdDev;
		return new ForecastResult(BigDecimal.valueOf(predicted), BigDecimal.valueOf(low), BigDecimal.valueOf(high),
				BigDecimal.valueOf(low).compareTo(thresholdAmount) < 0);
	}

	private double mean(double[] values) {
		if (values.length == 0) return 0;
		double sum = 0;
		for (double value : values) sum += value;
		return sum / values.length;
	}

	private double stdDev(double[] values, double mean) {
		if (values.length < 2) return 0;
		double sum = 0;
		for (double value : values) sum += Math.pow(value - mean, 2);
		return Math.sqrt(sum / (values.length - 1));
	}

	private double linearTrendSlope(double[] values) {
		int n = values.length;
		if (n < 2) return 0;
		double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
		for (int i = 0; i < n; i++) {
			sumX += i;
			sumY += values[i];
			sumXY += i * values[i];
			sumXX += i * i;
		}
		double denominator = n * sumXX - sumX * sumX;
		return denominator == 0 ? 0 : (n * sumXY - sumX * sumY) / denominator;
	}

	public record DailyPosition(java.time.LocalDate date, BigDecimal netPosition) {}
	public record ForecastResult(BigDecimal predictedPosition, BigDecimal bandLow, BigDecimal bandHigh, boolean atRisk) {}
}
