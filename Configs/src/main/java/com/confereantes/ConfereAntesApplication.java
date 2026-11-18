package com.confereantes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ConfereAntesApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConfereAntesApplication.class, args);
	}

}
