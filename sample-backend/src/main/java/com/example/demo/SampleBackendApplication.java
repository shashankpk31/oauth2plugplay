package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Sample Spring Boot Application demonstrating oidcplugplay OAuth2 Starter
 *
 * This application showcases enterprise-level authentication using:
 * - Keycloak as OAuth2 provider
 * - JWT token-based authentication
 * - Protected REST APIs
 * - Role-based access control
 */
@SpringBootApplication
public class SampleBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SampleBackendApplication.class, args);
	}

}
