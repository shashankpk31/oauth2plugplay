package com.shashankpk.oauth2.starter.provider;

import lombok.Getter;

/**
 * Supported OAuth2/OIDC providers
 */
@Getter
public enum OAuth2Provider {

    KEYCLOAK("Keycloak",
             "{issuer-uri}/protocol/openid-connect/auth",
             "{issuer-uri}/protocol/openid-connect/token",
             "{issuer-uri}/protocol/openid-connect/userinfo",
             "{issuer-uri}/protocol/openid-connect/certs",
             "{issuer-uri}/protocol/openid-connect/logout"),

    OKTA("Okta",
         "{issuer-uri}/v1/authorize",
         "{issuer-uri}/v1/token",
         "{issuer-uri}/v1/userinfo",
         "{issuer-uri}/v1/keys",
         "{issuer-uri}/v1/logout"),

    GOOGLE("Google",
           "https://accounts.google.com/o/oauth2/v2/auth",
           "https://oauth2.googleapis.com/token",
           "https://www.googleapis.com/oauth2/v3/userinfo",
           "https://www.googleapis.com/oauth2/v3/certs",
           "https://accounts.google.com/logout"),

    META("Meta (Facebook)",
         "https://www.facebook.com/v18.0/dialog/oauth",
         "https://graph.facebook.com/v18.0/oauth/access_token",
         "https://graph.facebook.com/me?fields=id,name,email",
         null,
         "https://www.facebook.com/logout.php"),

    GITHUB("GitHub",
           "https://github.com/login/oauth/authorize",
           "https://github.com/login/oauth/access_token",
           "https://api.github.com/user",
           null,
           null),

    MICROSOFT("Microsoft",
              "https://login.microsoftonline.com/common/oauth2/v2.0/authorize",
              "https://login.microsoftonline.com/common/oauth2/v2.0/token",
              "https://graph.microsoft.com/v1.0/me",
              "https://login.microsoftonline.com/common/discovery/v2.0/keys",
              "https://login.microsoftonline.com/common/oauth2/v2.0/logout"),

    CUSTOM("Custom OAuth2 Provider", null, null, null, null, null);

    private final String displayName;
    private final String authorizationUriTemplate;
    private final String tokenUriTemplate;
    private final String userInfoUriTemplate;
    private final String jwkSetUriTemplate;
    private final String logoutUriTemplate;

    OAuth2Provider(String displayName,
                   String authorizationUriTemplate,
                   String tokenUriTemplate,
                   String userInfoUriTemplate,
                   String jwkSetUriTemplate,
                   String logoutUriTemplate) {
        this.displayName = displayName;
        this.authorizationUriTemplate = authorizationUriTemplate;
        this.tokenUriTemplate = tokenUriTemplate;
        this.userInfoUriTemplate = userInfoUriTemplate;
        this.jwkSetUriTemplate = jwkSetUriTemplate;
        this.logoutUriTemplate = logoutUriTemplate;
    }

    /**
     * Build authorization URI from issuer URI
     */
    public String buildAuthorizationUri(String issuerUri) {
        if (authorizationUriTemplate == null) return null;
        return authorizationUriTemplate.replace("{issuer-uri}", issuerUri);
    }

    /**
     * Build token URI from issuer URI
     */
    public String buildTokenUri(String issuerUri) {
        if (tokenUriTemplate == null) return null;
        return tokenUriTemplate.replace("{issuer-uri}", issuerUri);
    }

    /**
     * Build user info URI from issuer URI
     */
    public String buildUserInfoUri(String issuerUri) {
        if (userInfoUriTemplate == null) return null;
        return userInfoUriTemplate.replace("{issuer-uri}", issuerUri);
    }

    /**
     * Build JWK Set URI from issuer URI
     */
    public String buildJwkSetUri(String issuerUri) {
        if (jwkSetUriTemplate == null) return null;
        return jwkSetUriTemplate.replace("{issuer-uri}", issuerUri);
    }

    /**
     * Build logout URI from issuer URI
     */
    public String buildLogoutUri(String issuerUri) {
        if (logoutUriTemplate == null) return null;
        return logoutUriTemplate.replace("{issuer-uri}", issuerUri);
    }

    /**
     * Parse provider from string
     */
    public static OAuth2Provider fromString(String provider) {
        try {
            return OAuth2Provider.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException e) {
            return CUSTOM;
        }
    }
}
