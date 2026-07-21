package com.ember.service;

import com.ember.domain.Staff;
import com.ember.domain.StaffRole;
import com.ember.repository.StaffRepository;
import com.ember.web.dto.StaffRequest;
import com.ember.web.dto.StaffResponse;
import com.ember.web.dto.StaffUpdateRequest;
import com.ember.web.error.BadRequestException;
import com.ember.web.error.NotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Manager-facing staff management. Guards against a manager locking themselves out. */
@Service
public class StaffService {

    private final StaffRepository staff;
    private final PasswordEncoder encoder;

    public StaffService(StaffRepository staff, PasswordEncoder encoder) {
        this.staff = staff;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public List<StaffResponse> list() {
        return staff.findAllByOrderByDisplayNameAsc().stream().map(StaffService::toResponse).toList();
    }

    @Transactional
    public StaffResponse create(StaffRequest request) {
        if (staff.existsByUsername(request.username())) {
            throw new BadRequestException("Username already taken: " + request.username());
        }
        boolean hasPin = request.pin() != null && !request.pin().isBlank();
        boolean hasPassword = request.password() != null && !request.password().isBlank();
        if (!hasPin && !hasPassword) {
            throw new BadRequestException("Set a PIN, a password, or both.");
        }
        Staff s = new Staff(request.username(), request.displayName(), request.role());
        if (hasPin) s.setPinHash(encoder.encode(request.pin()));
        if (hasPassword) s.setPasswordHash(encoder.encode(request.password()));
        return toResponse(staff.save(s));
    }

    @Transactional
    public StaffResponse update(Long id, StaffUpdateRequest request, String currentUsername) {
        Staff s = require(id);
        if (s.getUsername().equals(currentUsername)) {
            if (request.role() != StaffRole.MANAGER) {
                throw new BadRequestException("You can't remove your own manager role.");
            }
            if (!request.active()) {
                throw new BadRequestException("You can't deactivate yourself.");
            }
        }
        s.setDisplayName(request.displayName());
        s.setRole(request.role());
        s.setActive(request.active());
        return toResponse(staff.save(s));
    }

    @Transactional
    public void setPin(Long id, String pin) {
        require(id).setPinHash(pin == null || pin.isBlank() ? null : encoder.encode(pin));
    }

    @Transactional
    public void setPassword(Long id, String password) {
        require(id).setPasswordHash(password == null || password.isBlank() ? null : encoder.encode(password));
    }

    @Transactional
    public void delete(Long id, String currentUsername) {
        Staff s = require(id);
        if (s.getUsername().equals(currentUsername)) {
            throw new BadRequestException("You can't delete yourself.");
        }
        staff.delete(s);
    }

    private Staff require(Long id) {
        return staff.findById(id).orElseThrow(() -> new NotFoundException("Staff not found: " + id));
    }

    private static StaffResponse toResponse(Staff s) {
        return new StaffResponse(
                s.getId(), s.getUsername(), s.getDisplayName(), s.getRole(), s.isActive(),
                s.getPinHash() != null, s.getPasswordHash() != null, s.getCreatedAt());
    }
}
