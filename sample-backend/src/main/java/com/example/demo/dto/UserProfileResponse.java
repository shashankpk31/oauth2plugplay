package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private List<String> roles;
    private boolean emailVerified;
}
