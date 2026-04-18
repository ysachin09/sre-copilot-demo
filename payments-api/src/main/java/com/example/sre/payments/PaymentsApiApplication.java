package com.example.sre.payments;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PaymentsApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentsApiApplication.class, args);
    }
}
