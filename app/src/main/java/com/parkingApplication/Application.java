package com.parkingApplication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/*
    Created by anshanyan
    on 26.05.26
*/
@SpringBootApplication(scanBasePackages = {"com.parkingApplication", "com.parkingsystem"})
@EntityScan("com.parkingsystem.entity")
@EnableJpaRepositories("com.parkingsystem")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
