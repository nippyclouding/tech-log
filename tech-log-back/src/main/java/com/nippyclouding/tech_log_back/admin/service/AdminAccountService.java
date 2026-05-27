package com.nippyclouding.tech_log_back.admin.service;

import com.nippyclouding.tech_log_back.admin.entity.Admin;
import com.nippyclouding.tech_log_back.admin.repository.AdminRepository;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAccountService implements UserDetailsService, ApplicationRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final String bootstrapUsername;
    private final String bootstrapPassword;

    public AdminAccountService(
            AdminRepository adminRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin-console.username}") String bootstrapUsername,
            @Value("${app.admin-console.password}") String bootstrapPassword
    ) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapUsername = normalize(bootstrapUsername);
        this.bootstrapPassword = bootstrapPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminRepository.count() == 0) {
            if (bootstrapUsername.isBlank() || bootstrapPassword == null || bootstrapPassword.isBlank()) {
                throw new IllegalStateException("Initial admin credentials must be configured.");
            }
            adminRepository.save(new Admin(
                    bootstrapUsername,
                    passwordEncoder.encode(bootstrapPassword),
                    bootstrapUsername
            ));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        Admin admin = adminRepository.findByUsername(normalize(username))
                .orElseThrow(() -> new UsernameNotFoundException("Admin account was not found."));
        return User.withUsername(admin.getUsername())
                .password(admin.getPasswordHash())
                .authorities("ROLE_" + admin.getRole().toUpperCase(Locale.ROOT))
                .disabled(!admin.isActive())
                .accountLocked(admin.isLocked())
                .build();
    }

    @Transactional
    public void recordSuccessfulLogin(String username) {
        adminRepository.findByUsername(normalize(username)).ifPresent(Admin::recordSuccessfulLogin);
    }

    @Transactional
    public void recordFailedLogin(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        adminRepository.findByUsername(normalize(username)).ifPresent(Admin::recordFailedLogin);
    }

    private static String normalize(String username) {
        return username == null ? "" : username.trim();
    }
}
