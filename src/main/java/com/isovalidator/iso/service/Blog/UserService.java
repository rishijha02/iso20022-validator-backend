package com.isovalidator.iso.service.Blog;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.isovalidator.iso.DTO.Blog.CreateUserRequest;
import com.isovalidator.iso.Repository.UserRepository;
import com.isovalidator.iso.model.User;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User createUser(CreateUserRequest request) {

        if (request.getUsername() == null ||
                request.getUsername().isBlank()) {

            throw new IllegalArgumentException(
                    "Username cannot be empty"
            );
        }

        if (request.getPassword() == null ||
                request.getPassword().isBlank()) {

            throw new IllegalArgumentException(
                    "Password cannot be empty"
            );
        }

        if (request.getRole() == null ||
                request.getRole().isBlank()) {

            throw new IllegalArgumentException(
                    "Role cannot be empty"
            );
        }

        String username = request.getUsername().trim();
        String role = request.getRole().trim().toUpperCase();

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException(
                    "Username already exists: " + username
            );
        }

        if (!role.equals("ADMIN") && !role.equals("USER")) {
            throw new IllegalArgumentException(
                    "Invalid role. Allowed roles: ADMIN, USER"
            );
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .enabled(true)
                .build();

        return userRepository.save(user);
    }

}
