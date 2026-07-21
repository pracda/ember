package com.ember.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Station sign-in: pick your name from the roster, then punch your PIN. */
public record PinLoginRequest(
        @NotNull Long staffId,
        @NotBlank String pin
) { }
