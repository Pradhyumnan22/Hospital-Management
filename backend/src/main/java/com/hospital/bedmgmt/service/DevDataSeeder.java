package com.hospital.bedmgmt.service;

import com.hospital.bedmgmt.domain.Role;
import com.hospital.bedmgmt.domain.UserAccount;
import com.hospital.bedmgmt.repo.UserAccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "test"})
public class DevDataSeeder implements CommandLineRunner {

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;

    public DevDataSeeder(UserAccountRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        create("admin", "AdminDemo#2026", "Avery Admin", Role.ADMIN);
        create("admission", "AdmitDemo#2026", "Blair Admissions", Role.ADMISSION_STAFF);
        create("clinical", "ClinicDemo#2026", "Casey Clinical", Role.CLINICAL_STAFF);
    }

    private void create(String username, String password, String fullName, Role role) {
        if (users.findByUsername(username).isPresent()) {
            return;
        }
        UserAccount account = new UserAccount();
        account.setUsername(username);
        account.setPasswordHash(passwordEncoder.encode(password));
        account.setFullName(fullName);
        account.setRole(role);
        account.setEnabled(true);
        users.save(account);
    }
}
