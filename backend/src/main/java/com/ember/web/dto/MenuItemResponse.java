package com.ember.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record MenuItemResponse(
        String id,
        String name,
        String category,
        BigDecimal basePrice,
        boolean mealAvailable,
        List<ModifierResponse> sizes,
        List<ModifierResponse> addons
) {
    public record ModifierResponse(String label, BigDecimal priceDelta) { }
}
