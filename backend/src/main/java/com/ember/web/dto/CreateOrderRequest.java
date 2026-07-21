package com.ember.web.dto;

import com.ember.domain.OrderType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Payload the POS sends to open a ticket. Note there is no money here — the
 * server prices the order from the menu, so the client cannot dictate totals.
 */
public record CreateOrderRequest(
        @NotNull OrderType type,
        @NotEmpty @Valid List<OrderLineRequest> lines
) { }
