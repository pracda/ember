package com.ember.web.dto;

import jakarta.validation.constraints.Size;

/** Reset a staff member's admin password. */
public record PasswordResetRequest(
        @Size(min = 6, message = "Password must be at least 6 characters") String password
) { }
