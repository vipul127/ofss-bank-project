package com.bankdemo.cashrequirementservice.repository;

import com.bankdemo.cashrequirementservice.entity.TransferRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferRequestRepository extends JpaRepository<TransferRequest, String> {
}
