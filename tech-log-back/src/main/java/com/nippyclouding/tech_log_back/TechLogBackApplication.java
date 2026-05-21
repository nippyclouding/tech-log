package com.nippyclouding.tech_log_back;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TechLogBackApplication {

	public static void main(String[] args) {
		SpringApplication.run(TechLogBackApplication.class, args);
	}

}
