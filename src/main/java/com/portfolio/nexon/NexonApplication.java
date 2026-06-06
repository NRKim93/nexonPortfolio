package com.portfolio.nexon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class NexonApplication {

	public static void main(String[] args) {
		SpringApplication.run(NexonApplication.class, args);
	}

}
