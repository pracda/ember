package com.ember.web.dto;

import com.ember.domain.OrderStatus;
import com.ember.domain.OrderType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        int ticketNumber,
        OrderType type,
        OrderStatus status,
        List<OrderLineResponse> lines,
        BigDecimal subtotal,
        BigDecimal tax,
        BigDecimal total,
        Instant createdAt,
        Instant startedAt,
        Instant readyAt,
        Instant collectedAt
) {
    public record OrderLineResponse(
            Long id,
            String menuItemId,
            String itemName,
            int quantity,
            String size,
            boolean meal,
            List<String> addons,
            String notes,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) { }
}
