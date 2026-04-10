package org.wadhome.oag.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.wadhome.oag.domain.entity.AdminConfig;
import org.wadhome.oag.domain.repository.AdminConfigRepository;

/**
 * Seeds the admin_config table with the default password on first startup.
 * Default password: WorstPassword666! (change immediately after deployment)
 */
@Component
public class DataInitializer implements ApplicationRunner {

    public static final String DEFAULT_PASSWORD = "WorstPassword666!";

    private final AdminConfigRepository adminConfigRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
        AdminConfigRepository adminConfigRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.adminConfigRepository = adminConfigRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminConfigRepository.count() == 0) {
            AdminConfig config = new AdminConfig();
            config.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
            adminConfigRepository.save(config);
        }
    }
}
