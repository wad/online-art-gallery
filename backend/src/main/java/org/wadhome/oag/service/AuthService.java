package org.wadhome.oag.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wadhome.oag.domain.entity.AdminConfig;
import org.wadhome.oag.domain.repository.AdminConfigRepository;
import org.wadhome.oag.security.JwtUtil;

@Service
public class AuthService {

    private final AdminConfigRepository adminConfigRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(
        AdminConfigRepository adminConfigRepository,
        PasswordEncoder passwordEncoder,
        JwtUtil jwtUtil
    ) {
        this.adminConfigRepository = adminConfigRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public String login(String password) {
        AdminConfig config = adminConfigRepository.findFirstBy()
            .orElseThrow(() -> new IllegalStateException("Admin config not initialized"));
        if (!passwordEncoder.matches(password, config.getPasswordHash())) {
            throw new BadCredentialsException();
        }
        return jwtUtil.generateToken();
    }

    @Transactional
    public void changePassword(String currentPassword, String newPassword) {
        if (newPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        AdminConfig config = adminConfigRepository.findFirstBy()
            .orElseThrow(() -> new IllegalStateException("Admin config not initialized"));
        if (!passwordEncoder.matches(currentPassword, config.getPasswordHash())) {
            throw new BadCredentialsException();
        }
        config.setPasswordHash(passwordEncoder.encode(newPassword));
        adminConfigRepository.save(config);
    }

    public static class BadCredentialsException extends RuntimeException {
        public BadCredentialsException() {
            super("Invalid password");
        }
    }
}
