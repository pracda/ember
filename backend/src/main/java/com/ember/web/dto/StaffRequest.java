package com.ember.web.dto;

import com.ember.domain.StaffRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Manager payload to create a staff member. At least one of pin/password is required. */
public record StaffRequest(
        @NotBlank String username,
        @NotBlank String displayName,
        @NotNull StaffRole role,
        @Pattern(regexp = "\\d{4,6}", message = "PIN must be 4–6 digits") String pin,
        @Size(min = 6, message = "Password must be at least 6 characters") String password
) { }
