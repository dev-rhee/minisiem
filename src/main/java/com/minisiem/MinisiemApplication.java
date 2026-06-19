package com.minisiem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MinisiemApplication {

    public static void main(String[] args) {
        SpringApplication.run(MinisiemApplication.class, args);
    }

}
