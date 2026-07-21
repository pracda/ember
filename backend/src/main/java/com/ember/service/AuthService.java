package com.ember.service;

import com.ember.domain.Staff;
import com.ember.repository.StaffRepository;
import com.ember.security.JwtService;
import com.ember.web.dto.LoginResponse;
import com.ember.web.dto.RosterEntry;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Sign-in: password (admin) and PIN (stations), both minting the same JWT. */
@Service
public class AuthService {

    private final AuthenticationManager authManager;
    private final StaffRepository staff;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(AuthenticationManager authManager, StaffRepository staff,
                       PasswordEncoder encoder, JwtService jwt) {
        this.authManager = authManager;
        this.staff = staff;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    /** Password sign-in for the admin back-office. */
    @Transactional(readOnly = true)
    public LoginResponse passwordLogin(String username, String password) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        Staff s = staff.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Unknown user"));
        return tokenFor(s);
    }

    /** The station roster — active members who can sign in with a PIN. */
    @Transactional(readOnly = true)
    public List<RosterEntry> roster() {
        return staff.findByActiveTrueAndPinHashIsNotNullOrderByDisplayNameAsc().stream()
                .map(s -> new RosterEntry(s.getId(), s.getDisplayName(), s.getRole()))
                .toList();
    }

    /** PIN sign-in for a station, scoped to the picked staff member. */
    @Transactional(readOnly = true)
    public LoginResponse pinLogin(Long staffId, String pin) {
        Staff s = staff.findById(staffId)
                .filter(x -> x.isActive() && x.getPinHash() != null && encoder.matches(pin, x.getPinHash()))
                .orElseThrow(() -> new BadCredentialsException("Invalid PIN"));
        return tokenFor(s);
    }

    private LoginResponse tokenFor(Staff s) {
        String token = jwt.generate(s.getUsername(), s.getRole().name());
        return new LoginResponse(token, s.getUsername(), s.getRole().name(), s.getDisplayName());
    }
}
