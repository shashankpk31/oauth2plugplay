package com.shashankpk.oauth2.starter.exception;

/**
 * Custom exception for OAuth2 authentication failures
 */
public class OAuth2AuthenticationException extends RuntimeException {

    public OAuth2AuthenticationException(String message) {
        super(message);
    }

    public OAuth2AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
