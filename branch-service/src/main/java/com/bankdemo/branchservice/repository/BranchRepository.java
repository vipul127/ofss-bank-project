package com.bankdemo.branchservice.repository;

import com.bankdemo.branchservice.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<Branch, String> {
}
