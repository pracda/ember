package com.ember.security;

import com.ember.repository.StaffRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Password sign-in for the admin back-office, backed by the {@code staff} table.
 * Only members who have a password (managers) can authenticate this way; PIN
 * sign-in for stations is handled separately in {@code AuthService}.
 */
@Service
public class StaffUserDetailsService implements UserDetailsService {

    private final StaffRepository staff;

    public StaffUserDetailsService(StaffRepository staff) {
        this.staff = staff;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return staff.findByUsername(username)
                .filter(s -> s.getPasswordHash() != null)
                .map(s -> User.withUsername(s.getUsername())
                        .password(s.getPasswordHash())
                        .roles(s.getRole().name())
                        .disabled(!s.isActive())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("No password login for " + username));
    }
}
