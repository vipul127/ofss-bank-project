package com.bankdemo.cashmanagementservice.repository;

import com.bankdemo.cashmanagementservice.entity.CashTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CashTransactionRepository extends JpaRepository<CashTransaction, String> {
	List<CashTransaction> findByBranchIdOrderByEventTimestampDesc(String branchId);
}
