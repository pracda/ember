package com.ember.config;

import com.ember.domain.Staff;
import com.ember.domain.StaffRole;
import com.ember.repository.StaffRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Seeds the starting staff on an empty table so a fresh outlet can sign in and
 * then manage its own people. Demo credentials — change them in production.
 *
 * <ul>
 *   <li>manager — password {@code manager123} (admin), PIN {@code 9999}</li>
 *   <li>cashier — password {@code cashier123}, PIN {@code 1111}</li>
 *   <li>cook — password {@code cook123}, PIN {@code 2222}</li>
 * </ul>
 */
@Configuration
public class StaffSeeder {

    @Bean
    CommandLineRunner seedStaff(StaffRepository repo, PasswordEncoder encoder) {
        return args -> {
            if (repo.count() > 0) {
                return;
            }
            repo.save(member("manager", "Manager", StaffRole.MANAGER, "9999", "manager123", encoder));
            repo.save(member("cashier", "Cashier", StaffRole.CASHIER, "1111", "cashier123", encoder));
            repo.save(member("cook", "Cook", StaffRole.COOK, "2222", "cook123", encoder));
        };
    }

    private static Staff member(String username, String name, StaffRole role,
                                String pin, String password, PasswordEncoder encoder) {
        Staff s = new Staff(username, name, role);
        s.setPinHash(encoder.encode(pin));
        s.setPasswordHash(encoder.encode(password));
        return s;
    }
}
