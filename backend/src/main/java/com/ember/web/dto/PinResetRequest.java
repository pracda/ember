package com.ember.web.dto;

import jakarta.validation.constraints.Pattern;

/** Reset a staff member's station PIN. */
public record PinResetRequest(
        @Pattern(regexp = "\\d{4,6}", message = "PIN must be 4–6 digits") String pin
) { }
