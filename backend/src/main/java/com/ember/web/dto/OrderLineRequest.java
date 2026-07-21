package com.ember.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OrderLineRequest(
        @NotBlank String itemId,
        @Min(1) @Max(99) int quantity,
        String size,
        boolean meal,
        List<String> addons,
        @Size(max = 255) String notes
) {
    public List<String> addons() {
        return addons == null ? List.of() : addons;
    }
}
