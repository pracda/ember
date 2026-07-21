package com.ember.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** A size or add-on option in a menu-admin request. */
public record ModifierRequest(
        @NotBlank String label,
        @NotNull BigDecimal priceDelta
) { }
