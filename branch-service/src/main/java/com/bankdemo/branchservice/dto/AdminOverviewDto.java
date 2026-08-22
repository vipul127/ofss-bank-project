package com.bankdemo.branchservice.dto;

import java.math.BigDecimal;
import java.util.List;

public class AdminOverviewDto {
    private List<BranchDto> branches;
    private BigDecimal totalCashReserve;
    private BigDecimal totalLoanBook;
    private BigDecimal totalAssets;

    public List<BranchDto> getBranches() { return branches; }
    public void setBranches(List<BranchDto> branches) { this.branches = branches; }
    public BigDecimal getTotalCashReserve() { return totalCashReserve; }
    public void setTotalCashReserve(BigDecimal value) { totalCashReserve = value; }
    public BigDecimal getTotalLoanBook() { return totalLoanBook; }
    public void setTotalLoanBook(BigDecimal value) { totalLoanBook = value; }
    public BigDecimal getTotalAssets() { return totalAssets; }
    public void setTotalAssets(BigDecimal value) { totalAssets = value; }
}