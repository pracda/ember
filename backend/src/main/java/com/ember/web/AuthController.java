package com.ember.web;

import com.ember.service.AuthService;
import com.ember.web.dto.LoginRequest;
import com.ember.web.dto.LoginResponse;
import com.ember.web.dto.PinLoginRequest;
import com.ember.web.dto.RosterEntry;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Sign-in endpoints (all public): password for admin, roster + PIN for stations. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return auth.passwordLogin(request.username(), request.password());
    }

    @GetMapping("/roster")
    public List<RosterEntry> roster() {
        return auth.roster();
    }

    @PostMapping("/pin")
    public LoginResponse pinLogin(@Valid @RequestBody PinLoginRequest request) {
        return auth.pinLogin(request.staffId(), request.pin());
    }
}
