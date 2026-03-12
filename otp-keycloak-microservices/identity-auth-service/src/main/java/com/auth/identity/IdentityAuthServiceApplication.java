package com.auth.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IdentityAuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityAuthServiceApplication.class, args);
    }
}
