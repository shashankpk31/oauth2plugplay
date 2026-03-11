package com.auth.business.config;

import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@Configuration
public class FeignClientConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();

                // Propagate Authorization header (JWT)
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    requestTemplate.header("Authorization", authHeader);
                    log.debug("Propagating JWT token to FeignClient");
                }

                // Propagate user context headers
                String userId = request.getHeader("X-User-Id");
                String userEmail = request.getHeader("X-User-Email");
                String userPhone = request.getHeader("X-User-Phone");

                if (userId != null) {
                    requestTemplate.header("X-User-Id", userId);
                }
                if (userEmail != null) {
                    requestTemplate.header("X-User-Email", userEmail);
                }
                if (userPhone != null) {
                    requestTemplate.header("X-User-Phone", userPhone);
                }
            }
        };
    }
}
