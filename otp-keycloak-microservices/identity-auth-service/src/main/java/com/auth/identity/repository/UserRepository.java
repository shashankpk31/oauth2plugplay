package com.auth.identity.repository;

import com.auth.identity.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    Optional<User> findByKeycloakId(String keycloakId);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);
}
