package com.ember.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Staff login payload. */
public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password
) { }
