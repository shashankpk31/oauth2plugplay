package com.shashankpk.oauth2.starter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * User information DTO from OAuth2 provider
 * Follows OIDC standard claims with provider-specific extensions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserInfo {

    @JsonProperty("sub")
    private String subject;

    @JsonProperty("preferred_username")
    private String username;

    @JsonProperty("name")
    private String name;

    @JsonProperty("given_name")
    private String givenName;

    @JsonProperty("family_name")
    private String familyName;

    @JsonProperty("email")
    private String email;

    @JsonProperty("email_verified")
    private Boolean emailVerified;

    @JsonProperty("phone_number")
    private String phoneNumber;

    @JsonProperty("phone_number_verified")
    private Boolean phoneNumberVerified;

    @JsonProperty("picture")
    private String picture;

    @JsonProperty("locale")
    private String locale;

    @JsonProperty("zoneinfo")
    private String zoneinfo;

    /**
     * Provider-specific attributes
     */
    private Map<String, Object> attributes;
}
