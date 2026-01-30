package com.osen.msvc_order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;


@SpringBootApplication
@EnableFeignClients
public class MsvcOrderApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsvcOrderApplication.class, args);
	}

}
