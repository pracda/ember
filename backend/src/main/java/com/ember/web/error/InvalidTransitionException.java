package com.ember.web.error;

import com.ember.domain.OrderStatus;

/** Thrown when an order status change is not allowed from the current state. Maps to HTTP 409. */
public class InvalidTransitionException extends RuntimeException {
    public InvalidTransitionException(Long orderId, OrderStatus from, String action) {
        super("Cannot '" + action + "' order " + orderId + " from status " + from);
    }
}
