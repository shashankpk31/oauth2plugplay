package com.auth.gateway.config;

import com.auth.gateway.filter.JwtAuthenticationFilter;
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouterFunction<ServerResponse> gatewayRoutes() {
        return route("auth-public")
                .route(path("/auth/register", "/auth/otp/send", "/auth/otp/verify", "/auth/refresh", "/auth/mpin/login"),
                    HandlerFunctions.http("http://localhost:8082"))
                .build()
            .and(route("auth-protected")
                .route(path("/auth/**"),
                    HandlerFunctions.http("http://localhost:8082"))
                .filter(JwtAuthenticationFilter.addUserContextHeaders())
                .build())
            .and(route("business-service")
                .route(path("/api/**"),
                    HandlerFunctions.http("http://localhost:8083"))
                .filter(JwtAuthenticationFilter.addUserContextHeaders())
                .build());
    }
}
