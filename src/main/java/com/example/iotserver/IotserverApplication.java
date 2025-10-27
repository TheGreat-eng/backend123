package com.example.iotserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling; // ✅ THÊM IMPORT

@SpringBootApplication
@EnableScheduling // ✅ THÊM ANNOTATION NÀY
@EnableCaching // ✅ THÊM ANNOTATION NÀY
public class IotserverApplication {

	public static void main(String[] args) {
		SpringApplication.run(IotserverApplication.class, args);
	}

}
