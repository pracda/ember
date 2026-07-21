package com.ember.web.dto;

import com.ember.domain.StaffRole;

/** A pick-able staff member on the station sign-in screen (no secrets). */
public record RosterEntry(
        Long id,
        String displayName,
        StaffRole role
) { }
