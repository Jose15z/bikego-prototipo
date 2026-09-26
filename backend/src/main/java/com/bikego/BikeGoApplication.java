package com.bikego;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BikeGoApplication {

    public static void main(String[] args) {
        SpringApplication.run(BikeGoApplication.class, args);
    }
}
