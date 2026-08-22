package com.bankdemo.branchservice.controller;

import com.bankdemo.branchservice.dto.AdminOverviewDto;
import com.bankdemo.branchservice.dto.BranchDto;
import com.bankdemo.branchservice.service.BranchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final BranchService branchService;

    public AdminController(BranchService branchService) { this.branchService = branchService; }

    @GetMapping("/overview")
    public AdminOverviewDto getOverview() {
        List<BranchDto> branches = branchService.getAllBranches();
        AdminOverviewDto overview = new AdminOverviewDto();
        overview.setBranches(branches);
        overview.setTotalCashReserve(branches.stream().map(BranchDto::getCurrentReserve).reduce(BigDecimal.ZERO, BigDecimal::add));
        overview.setTotalLoanBook(branches.stream().map(BranchDto::getLoanBook).reduce(BigDecimal.ZERO, BigDecimal::add));
        overview.setTotalAssets(branches.stream().map(BranchDto::getTotalAssets).reduce(BigDecimal.ZERO, BigDecimal::add));
        return overview;
    }
}