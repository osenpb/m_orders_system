package com.osen.msvc_payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MsvcPaymentApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsvcPaymentApplication.class, args);
	}

}
