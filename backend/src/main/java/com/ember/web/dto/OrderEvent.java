package com.ember.web.dto;

/**
 * Envelope broadcast over STOMP whenever an order changes. Every station
 * subscribes to the same stream and filters by the order's status, exactly as
 * the prototype's three views share one order list.
 */
public record OrderEvent(Type type, OrderResponse order) {

    public enum Type {
        ORDER_CREATED,
        ORDER_STARTED,
        ORDER_READY,
        ORDER_RECALLED,
        ORDER_COLLECTED
    }
}
