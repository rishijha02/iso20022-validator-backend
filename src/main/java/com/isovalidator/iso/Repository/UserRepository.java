package com.isovalidator.iso.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.isovalidator.iso.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}
