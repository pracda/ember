package com.ember.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/** Manager payload to create or update a menu item. */
public record MenuItemRequest(
        @NotBlank String id,
        @NotBlank String name,
        @NotBlank String category,
        @NotNull @DecimalMin("0.0") BigDecimal basePrice,
        boolean mealAvailable,
        @Valid List<ModifierRequest> sizes,
        @Valid List<ModifierRequest> addons,
        Boolean available,
        boolean tracksStock,
        @Min(0) int stock,
        @Min(0) int lowStockThreshold
) {
    public List<ModifierRequest> sizes() {
        return sizes == null ? List.of() : sizes;
    }

    public List<ModifierRequest> addons() {
        return addons == null ? List.of() : addons;
    }

    /** Missing means available (so callers don't accidentally 86 a new item). */
    public boolean availableOrDefault() {
        return available == null || available;
    }
}
