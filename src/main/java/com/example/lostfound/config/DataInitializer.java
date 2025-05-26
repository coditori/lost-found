package com.example.lostfound.config;

import com.example.lostfound.entity.Role;
import com.example.lostfound.entity.User;
import com.example.lostfound.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.default-username:admin}")
    private String adminUsername;

    @Value("${app.admin.default-password:admin123}")
    private String adminPassword;

    @Override
    public void run(String... args) throws Exception {
        initializeAdminUser();
    }

    @Transactional
    private void initializeAdminUser() {
        if (!userRepository.existsByUsername(adminUsername)) {
            log.info("Creating default admin user with username: {}", adminUsername);
            
            User adminUser = User.builder()
                    .username(adminUsername)
                    .password(passwordEncoder.encode(adminPassword))
                    .name("System Administrator")
                    .email("admin@lostfound.com")
                    .role(Role.ADMIN)
                    .enabled(true)
                    .accountNonExpired(true)
                    .accountNonLocked(true)
                    .credentialsNonExpired(true)
                    .build();

            userRepository.save(adminUser);
            log.info("Default admin user created successfully with username: {}", adminUsername);
        } else {
            log.info("Admin user already exists with username: {}", adminUsername);
        }
    }
} 