package com.ember.repository;

import com.ember.domain.Order;
import com.ember.domain.OrderStatus;
import com.ember.domain.OrderType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Slice test for the repository queries the kitchen rail and ticket sequence
 * depend on: active-only, oldest-first for the rail; newest-first for history;
 * and the max ticket number used to seed {@code TicketSequence}.
 */
@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orders;

    /** Persist an order at the given status, spacing createdAt so ordering is deterministic. */
    private Order persist(int ticket, OrderStatus status) {
        Order o = new Order(ticket, OrderType.DINE_IN);
        if (status == OrderStatus.PREP || status == OrderStatus.READY || status == OrderStatus.DONE) {
            o.advance(); // NEW -> PREP
        }
        if (status == OrderStatus.READY || status == OrderStatus.DONE) {
            o.advance(); // PREP -> READY
        }
        if (status == OrderStatus.DONE) {
            o.collect(); // READY -> DONE
        }
        Order saved = orders.saveAndFlush(o);
        try {
            Thread.sleep(5); // ensure distinct createdAt instants across inserts
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return saved;
    }

    @Test
    void activeRailIsOldestFirstAndExcludesReadyAndDone() {
        persist(1, OrderStatus.NEW);
        persist(2, OrderStatus.PREP);
        persist(3, OrderStatus.READY); // not active
        persist(4, OrderStatus.DONE);  // not active

        List<Order> rail = orders.findByStatusInOrderByCreatedAtAsc(
                List.of(OrderStatus.NEW, OrderStatus.PREP));

        assertThat(rail).extracting(Order::getTicketNumber).containsExactly(1, 2);
    }

    @Test
    void readyIsNewestReadyAtFirst() {
        persist(1, OrderStatus.READY);
        persist(2, OrderStatus.READY);
        persist(3, OrderStatus.READY);

        List<Order> ready = orders.findByStatusOrderByReadyAtDesc(OrderStatus.READY);

        assertThat(ready).extracting(Order::getTicketNumber).containsExactly(3, 2, 1);
    }

    @Test
    void historyIsNewestFirst() {
        persist(1, OrderStatus.NEW);
        persist(2, OrderStatus.NEW);
        persist(3, OrderStatus.NEW);

        List<Order> history = orders.findAllByOrderByCreatedAtDesc();

        assertThat(history).extracting(Order::getTicketNumber).containsExactly(3, 2, 1);
    }

    @Test
    void maxTicketNumberIsNullWhenEmptyThenTracksHighest() {
        assertThat(orders.findMaxTicketNumber()).isNull();

        persist(7, OrderStatus.NEW);
        persist(3, OrderStatus.NEW);
        persist(12, OrderStatus.NEW);

        assertThat(orders.findMaxTicketNumber()).isEqualTo(12);
    }
}
