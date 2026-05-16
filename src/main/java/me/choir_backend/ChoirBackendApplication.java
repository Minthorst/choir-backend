package me.choir_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ChoirBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChoirBackendApplication.class, args);
	}

}
