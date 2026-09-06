package com.memory.memora_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MemoryApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(MemoryApiApplication.class, args);
	}

}
