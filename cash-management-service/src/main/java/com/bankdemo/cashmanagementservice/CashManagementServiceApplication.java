package com.bankdemo.cashmanagementservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class CashManagementServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CashManagementServiceApplication.class, args);
	}

}
