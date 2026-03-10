package com.shashankpk.otpauth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {

    private String id;

    private String phoneNumber;

    private String email;

    private String name;

    private Boolean active;
}
