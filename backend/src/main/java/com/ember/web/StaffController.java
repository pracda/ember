package com.ember.web;

import com.ember.service.StaffService;
import com.ember.web.dto.PasswordResetRequest;
import com.ember.web.dto.PinResetRequest;
import com.ember.web.dto.StaffRequest;
import com.ember.web.dto.StaffResponse;
import com.ember.web.dto.StaffUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Employee management. Manager-only, enforced by {@code SecurityConfig}. */
@RestController
@RequestMapping("/api/staff")
public class StaffController {

    private final StaffService staff;

    public StaffController(StaffService staff) {
        this.staff = staff;
    }

    @GetMapping
    public List<StaffResponse> list() {
        return staff.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StaffResponse create(@Valid @RequestBody StaffRequest request) {
        return staff.create(request);
    }

    @PutMapping("/{id}")
    public StaffResponse update(@PathVariable Long id, @Valid @RequestBody StaffUpdateRequest request,
                                Authentication caller) {
        return staff.update(id, request, caller.getName());
    }

    @PutMapping("/{id}/pin")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setPin(@PathVariable Long id, @Valid @RequestBody PinResetRequest request) {
        staff.setPin(id, request.pin());
    }

    @PutMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setPassword(@PathVariable Long id, @Valid @RequestBody PasswordResetRequest request) {
        staff.setPassword(id, request.password());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication caller) {
        staff.delete(id, caller.getName());
    }
}
