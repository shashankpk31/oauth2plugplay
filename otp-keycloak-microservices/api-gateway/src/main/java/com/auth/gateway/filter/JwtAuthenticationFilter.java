package com.auth.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions;
import org.springframework.cloud.gateway.server.mvc.filter.FilterFunctions;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerRequest;

import java.util.List;
import java.util.function.Function;

@Slf4j
@Component
public class JwtAuthenticationFilter {

    public static HandlerFilterFunction<?, ?> addUserContextHeaders() {
        return (request, next) -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                Jwt jwt = jwtAuth.getToken();

                // Extract user information from JWT
                String userId = jwt.getSubject();
                String email = jwt.getClaimAsString("email");
                String phone = jwt.getClaimAsString("phone_number");
                String preferredUsername = jwt.getClaimAsString("preferred_username");

                // Get roles from realm_access or resource_access
                List<String> roles = extractRoles(jwt);

                log.info("Processing request for user: {}", userId);

                // Add headers to the request using BeforeFilterFunctions
                ServerRequest modifiedRequest = ServerRequest.from(request)
                    .headers(headers -> {
                        headers.set("X-User-Id", userId != null ? userId : "");
                        headers.set("X-User-Email", email != null ? email : "");
                        headers.set("X-User-Phone", phone != null ? phone : "");
                        headers.set("X-User-Username", preferredUsername != null ? preferredUsername : "");
                        headers.set("X-User-Roles", String.join(",", roles));
                    })
                    .build();

                return next.handle(modifiedRequest);
            }

            // If no JWT authentication, continue without adding headers
            return next.handle(request);
        };
    }

    @SuppressWarnings("unchecked")
    private static List<String> extractRoles(Jwt jwt) {
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
}
