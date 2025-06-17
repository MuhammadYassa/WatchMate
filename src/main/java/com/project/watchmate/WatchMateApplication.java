package com.project.watchmate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WatchMateApplication {

	public static void main(String[] args) {
		SpringApplication.run(WatchMateApplication.class, args);
	}
	
}