package com.ember.web.dto;

import com.ember.domain.StaffRole;

import java.time.Instant;

/** Staff member as shown in the admin Employees tab — never exposes the secrets. */
public record StaffResponse(
        Long id,
        String username,
        String displayName,
        StaffRole role,
        boolean active,
        boolean hasPin,
        boolean hasPassword,
        Instant createdAt
) { }
