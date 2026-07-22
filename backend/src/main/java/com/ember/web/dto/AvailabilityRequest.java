package com.ember.web.dto;

import jakarta.validation.constraints.NotNull;

/** Quick 86 / un-86 toggle body. */
public record AvailabilityRequest(
        @NotNull Boolean available
) { }
