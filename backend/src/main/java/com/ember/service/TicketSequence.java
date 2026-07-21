package com.ember.service;

import com.ember.repository.OrderRepository;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hands out the customer-facing ticket numbers. Seeded from the highest number
 * already in the database so it survives restarts, and thread-safe for
 * concurrent tills.
 *
 * <p>For a daily reset, schedule a job at midnight that resets the counter to 0,
 * or switch to a database sequence keyed by business date.</p>
 */
@Component
public class TicketSequence {

    private final AtomicInteger counter;

    public TicketSequence(OrderRepository orders) {
        Integer max = orders.findMaxTicketNumber();
        this.counter = new AtomicInteger(max == null ? 0 : max);
    }

    public int next() {
        return counter.incrementAndGet();
    }
}
