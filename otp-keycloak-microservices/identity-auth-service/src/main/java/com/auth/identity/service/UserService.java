package com.auth.identity.service;

import com.auth.identity.dto.UserDto;
import com.auth.identity.exception.AuthException;
import com.auth.identity.model.User;
import com.auth.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User createUser(String keycloakId, String email, String phone) {
        User user = User.builder()
                .keycloakId(keycloakId)
                .email(email)
                .phone(phone)
                .build();

        return userRepository.save(user);
    }

    public Optional<User> findByIdentifier(String identifier) {
        Optional<User> user = userRepository.findByEmail(identifier);
        if (user.isPresent()) {
            return user;
        }
        return userRepository.findByPhone(identifier);
    }

    public Optional<User> findByKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId);
    }

    public boolean existsByIdentifier(String identifier) {
        return userRepository.existsByEmail(identifier) || userRepository.existsByPhone(identifier);
    }

    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("User not found"));
    }

    public UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .keycloakId(user.getKeycloakId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .build();
    }
}
