package com.bankdemo.forecastservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ForecastServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ForecastServiceApplication.class, args);
	}

}
