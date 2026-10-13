package com.cloudpulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
// Turns on @Scheduled methods (MonitoringService.tick runs every 5 seconds)
@EnableScheduling
public class CloudpulseApplication {

	public static void main(String[] args) {
		SpringApplication.run(CloudpulseApplication.class, args);
	}

}
