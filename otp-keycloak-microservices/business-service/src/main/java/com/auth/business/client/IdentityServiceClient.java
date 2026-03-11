package com.auth.business.client;

import com.auth.business.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;
import java.util.UUID;

@FeignClient(
    name = "identity-service",
    url = "${identity-service.url}",
    configuration = FeignClientConfig.class
)
public interface IdentityServiceClient {

    @GetMapping("/auth/user/{userId}")
    Map<String, Object> getUserById(@PathVariable("userId") UUID userId);

    @GetMapping("/auth/user/me")
    Map<String, Object> getCurrentUser();
}
