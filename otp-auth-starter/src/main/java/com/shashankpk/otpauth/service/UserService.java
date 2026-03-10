package com.shashankpk.otpauth.service;

import com.shashankpk.otpauth.dto.UserProfile;
import com.shashankpk.otpauth.model.User;
import com.shashankpk.otpauth.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Create user in application database
     */
    public User createUser(String id, String phoneNumber, String email, String name) {
        User user = new User();
        user.setId(id);
        user.setPhoneNumber(phoneNumber);
        user.setEmail(email);
        user.setName(name);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    /**
     * Get user profile
     */
    public UserProfile getUserProfile(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        UserProfile profile = new UserProfile();
        profile.setId(user.getId());
        profile.setPhoneNumber(user.getPhoneNumber());
        profile.setEmail(user.getEmail());
        profile.setName(user.getName());
        profile.setActive(user.getActive());

        return profile;
    }

    /**
     * Update last login time
     */
    public void updateLastLogin(String userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
            log.debug("Updated last login for user: {}", userId);
        });
    }
}
