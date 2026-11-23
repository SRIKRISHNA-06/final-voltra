package com.voltra.platform.config;

import com.voltra.platform.model.Role;
import com.voltra.platform.model.User;
import com.voltra.platform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements CommandLineRunner {
    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminBootstrap(UserRepository userRepo,
                          PasswordEncoder encoder,
                          @Value("${voltra.admin.email:}") String adminEmail,
                          @Value("${voltra.admin.password:}") String adminPassword) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            return;
        }
        if (userRepo.countByRole(Role.ADMIN) == 0 && !userRepo.existsByEmailIgnoreCase(adminEmail.trim())) {
            User admin = new User();
            admin.setFullName("VOLTRA Platform Administrator");
            admin.setEmail(adminEmail.trim().toLowerCase());
            admin.setPasswordHash(encoder.encode(adminPassword));
            admin.setRole(Role.ADMIN);
            userRepo.save(admin);
        }
    }
}
