package com.example.ecommerce.common.config;

import com.example.ecommerce.user.entity.Role;
import com.example.ecommerce.user.entity.User;
import com.example.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUser("admin@example.com", "Admin", "User", "admin123", Role.ROLE_ADMIN);
        seedUser("user@example.com",  "John",  "Doe",  "user1234", Role.ROLE_USER);
    }

    private void seedUser(String email, String firstName, String lastName, String password, Role role) {
        userRepository.findByEmail(email).ifPresentOrElse(
                existing -> {
                    if (existing.getRole() != role) {
                        existing.setRole(role);
                        userRepository.save(existing);
                    }
                },
                () -> userRepository.save(User.builder()
                        .email(email)
                        .firstName(firstName)
                        .lastName(lastName)
                        .password(passwordEncoder.encode(password))
                        .role(role)
                        .enabled(true)
                        .build())
        );
    }
}