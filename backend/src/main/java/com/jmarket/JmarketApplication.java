package com.jmarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class
JmarketApplication {

    public static void main(String[] args) {
        SpringApplication.run(JmarketApplication.class, args);
    }
}
