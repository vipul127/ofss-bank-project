package com.bankdemo.cashrequirementservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class CashRequirementServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CashRequirementServiceApplication.class, args);
    }
}
