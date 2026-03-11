package com.auth.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    public JwtAuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> ReactiveSecurityContextHolder.getContext()
            .map(securityContext -> securityContext.getAuthentication())
            .filter(authentication -> authentication instanceof JwtAuthenticationToken)
            .cast(JwtAuthenticationToken.class)
            .flatMap(authentication -> {
                Jwt jwt = authentication.getToken();

                // Extract user information from JWT
                String userId = jwt.getSubject();
                String email = jwt.getClaimAsString("email");
                String phone = jwt.getClaimAsString("phone_number");
                String preferredUsername = jwt.getClaimAsString("preferred_username");

                // Get roles from realm_access or resource_access
                List<String> roles = extractRoles(jwt);

                log.info("Processing request for user: {}", userId);

                // Add headers to the request
                ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", userId != null ? userId : "")
                    .header("X-User-Email", email != null ? email : "")
                    .header("X-User-Phone", phone != null ? phone : "")
                    .header("X-User-Username", preferredUsername != null ? preferredUsername : "")
                    .header("X-User-Roles", String.join(",", roles))
                    .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            })
            .switchIfEmpty(chain.filter(exchange));
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Jwt jwt) {
        try {
            // Try to get roles from realm_access
            Object realmAccess = jwt.getClaim("realm_access");
            if (realmAccess instanceof java.util.Map) {
                Object roles = ((java.util.Map<String, Object>) realmAccess).get("roles");
                if (roles instanceof List) {
                    return (List<String>) roles;
                }
            }

            // Try to get roles from resource_access
            Object resourceAccess = jwt.getClaim("resource_access");
            if (resourceAccess instanceof java.util.Map) {
                // Get first client's roles
                for (Object clientRoles : ((java.util.Map<String, Object>) resourceAccess).values()) {
                    if (clientRoles instanceof java.util.Map) {
                        Object roles = ((java.util.Map<String, Object>) clientRoles).get("roles");
                        if (roles instanceof List) {
                            return (List<String>) roles;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error extracting roles from JWT: {}", e.getMessage());
        }

        return List.of();
    }

    public static class Config {
        // Configuration properties if needed
    }
}
