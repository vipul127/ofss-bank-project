package com.bankdemo.cashmanagementservice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class DailyCashAnalysisDto {
    private List<DailyPoint> dailyPoints;
    private BigDecimal averageDailyNet;
    private BigDecimal standardDeviation;
    private BigDecimal averageSurplus;
    private BigDecimal averageDeficit;
    private BigDecimal largestWithdrawal;
    private BigDecimal withdrawalImpactPercent;
    private BigDecimal todayDeposits;
    private BigDecimal todayWithdrawals;
    private BigDecimal todayNet;
    private BigDecimal reserveBeforeToday;
    private BigDecimal todayVsAverageAmount;
    private BigDecimal todayVsAveragePercent;

    public List<DailyPoint> getDailyPoints() { return dailyPoints; }
    public void setDailyPoints(List<DailyPoint> dailyPoints) { this.dailyPoints = dailyPoints; }
    public BigDecimal getAverageDailyNet() { return averageDailyNet; }
    public void setAverageDailyNet(BigDecimal value) { this.averageDailyNet = value; }
    public BigDecimal getStandardDeviation() { return standardDeviation; }
    public void setStandardDeviation(BigDecimal value) { this.standardDeviation = value; }
    public BigDecimal getAverageSurplus() { return averageSurplus; }
    public void setAverageSurplus(BigDecimal value) { this.averageSurplus = value; }
    public BigDecimal getAverageDeficit() { return averageDeficit; }
    public void setAverageDeficit(BigDecimal value) { this.averageDeficit = value; }
    public BigDecimal getLargestWithdrawal() { return largestWithdrawal; }
    public void setLargestWithdrawal(BigDecimal value) { this.largestWithdrawal = value; }
    public BigDecimal getWithdrawalImpactPercent() { return withdrawalImpactPercent; }
    public void setWithdrawalImpactPercent(BigDecimal value) { this.withdrawalImpactPercent = value; }
    public BigDecimal getTodayDeposits() { return todayDeposits; }
    public void setTodayDeposits(BigDecimal value) { this.todayDeposits = value; }
    public BigDecimal getTodayWithdrawals() { return todayWithdrawals; }
    public void setTodayWithdrawals(BigDecimal value) { this.todayWithdrawals = value; }
    public BigDecimal getTodayNet() { return todayNet; }
    public void setTodayNet(BigDecimal value) { this.todayNet = value; }
    public BigDecimal getReserveBeforeToday() { return reserveBeforeToday; }
    public void setReserveBeforeToday(BigDecimal value) { this.reserveBeforeToday = value; }
    public BigDecimal getTodayVsAverageAmount() { return todayVsAverageAmount; }
    public void setTodayVsAverageAmount(BigDecimal value) { this.todayVsAverageAmount = value; }
    public BigDecimal getTodayVsAveragePercent() { return todayVsAveragePercent; }
    public void setTodayVsAveragePercent(BigDecimal value) { this.todayVsAveragePercent = value; }

    public record DailyPoint(LocalDate date, BigDecimal netMovement, BigDecimal closingReserve) {}
}
