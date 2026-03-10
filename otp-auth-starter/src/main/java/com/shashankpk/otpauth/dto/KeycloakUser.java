package com.shashankpk.otpauth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeycloakUser {

    private String id;

    private String username;

    private String email;

    private String phoneNumber;

    private String name;

    private boolean newlyCreated;
}
