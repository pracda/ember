package com.ember.domain;

import java.util.Set;

/**
 * The order lifecycle. Transitions are enforced centrally so no controller or
 * client can move an order into an illegal state.
 *
 * <pre>
 *   NEW ──start──▶ PREP ──ready──▶ READY ──collect──▶ DONE
 *                   ▲                 │
 *                   └─────recall──────┘
 * </pre>
 */
public enum OrderStatus {
    NEW,
    PREP,
    READY,
    DONE;

    private static final Set<OrderStatus> ACTIVE = Set.of(NEW, PREP);

    public boolean isActive() {
        return ACTIVE.contains(this);
    }

    /** The next status when a station "advances" an order, or null if it cannot advance. */
    public OrderStatus advanced() {
        return switch (this) {
            case NEW -> PREP;
            case PREP -> READY;
            default -> null;
        };
    }
}
