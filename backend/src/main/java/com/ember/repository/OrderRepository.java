package com.ember.repository;

import com.ember.domain.Order;
import com.ember.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /** Active tickets for the kitchen rail, oldest first. */
    List<Order> findByStatusInOrderByCreatedAtAsc(List<OrderStatus> statuses);

    List<Order> findByStatusOrderByCreatedAtAsc(OrderStatus status);

    List<Order> findAllByOrderByCreatedAtDesc();

    /** Highest ticket number issued so far, used to seed the ticket sequence on start-up. */
    @Query("select max(o.ticketNumber) from Order o")
    Integer findMaxTicketNumber();
}
