package com.isovalidator.iso.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.isovalidator.iso.Repository.UserRepository;
import com.isovalidator.iso.model.User;

@Configuration
public class AdminUserInitializer {

    @Bean
    CommandLineRunner initializeAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${BOOTSTRAP_ADMIN_USERNAME:}") String username,
            @Value("${BOOTSTRAP_ADMIN_PASSWORD:}") String password) {

        return args -> {

            // Do nothing if bootstrap credentials are not configured
            if (username == null || username.isBlank()
                    || password == null || password.isBlank()) {
                return;
            }

            // Do nothing if an ADMIN already exists
            boolean adminExists = userRepository.findAll()
                    .stream()
                    .anyMatch(user ->
                            "ADMIN".equalsIgnoreCase(user.getRole())
                    );

            if (adminExists) {
                return;
            }

            User admin = User.builder()
                    .username(username.trim())
                    .password(passwordEncoder.encode(password))
                    .role("ADMIN")
                    .enabled(true)
                    .build();

            userRepository.save(admin);

            System.out.println(
                    "Bootstrap ADMIN user created: " + username
            );
        };
    }
}
