package com.ember.web.dto;

import com.ember.domain.StaffRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Manager payload to update a staff member's profile (username is immutable). */
public record StaffUpdateRequest(
        @NotBlank String displayName,
        @NotNull StaffRole role,
        boolean active
) { }
