package com.parkingApplication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/*
    Created by anshanyan
    on 26.05.26
*/
@SpringBootApplication(scanBasePackages = {"com.parkingApplication", "com.parkingsystem", "configs"})
//@Import({configs.ParkingServiceConfiguration.class, configs.PersistenceConfiguration.class, configs.DBproperties.class})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}