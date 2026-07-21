package com.ember.service;

import com.ember.domain.MenuItem;
import com.ember.domain.Order;
import com.ember.domain.OrderLine;
import com.ember.domain.OrderStatus;
import com.ember.repository.MenuItemRepository;
import com.ember.repository.OrderRepository;
import com.ember.web.dto.CreateOrderRequest;
import com.ember.web.dto.Mappers;
import com.ember.web.dto.OrderEvent;
import com.ember.web.dto.OrderLineRequest;
import com.ember.web.dto.OrderResponse;
import com.ember.web.error.NotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Application service for the order lifecycle. Every mutation persists the change
 * and publishes an {@link OrderEvent}; the broadcast to {@code /topic/orders}
 * happens after the transaction commits (see
 * {@link com.ember.web.OrderEventBroadcaster}), so the kitchen display and pickup
 * board never see an event for a change that later rolls back.
 */
@Service
public class OrderService {

    private final OrderRepository orders;
    private final MenuItemRepository menu;
    private final PricingService pricing;
    private final TicketSequence tickets;
    private final ApplicationEventPublisher events;

    public OrderService(OrderRepository orders, MenuItemRepository menu, PricingService pricing,
                        TicketSequence tickets, ApplicationEventPublisher events) {
        this.orders = orders;
        this.menu = menu;
        this.pricing = pricing;
        this.tickets = tickets;
        this.events = events;
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        Order order = new Order(tickets.next(), request.type());
        order.setServedBy(currentUsername());

        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderLineRequest lineReq : request.lines()) {
            MenuItem item = menu.findById(lineReq.itemId())
                    .orElseThrow(() -> new NotFoundException("Unknown menu item: " + lineReq.itemId()));

            BigDecimal unit = pricing.unitPrice(item, lineReq);

            OrderLine line = new OrderLine();
            line.setMenuItemId(item.getId());
            line.setItemName(item.getName());
            line.setQuantity(lineReq.quantity());
            line.setSize(lineReq.size());
            line.setMeal(lineReq.meal());
            line.setAddons(new java.util.ArrayList<>(lineReq.addons()));
            line.setNotes(lineReq.notes());
            line.setUnitPrice(unit);
            order.addLine(line);

            subtotal = subtotal.add(line.lineTotal());
        }

        subtotal = pricing.scale(subtotal);
        BigDecimal tax = pricing.tax(subtotal);
        order.setSubtotal(subtotal);
        order.setTax(tax);
        order.setTotal(pricing.scale(subtotal.add(tax)));

        Order saved = orders.save(order);
        return publish(saved, OrderEvent.Type.ORDER_CREATED);
    }

    @Transactional
    public OrderResponse advance(Long id) {
        Order order = require(id);
        boolean wasPrep = order.getStatus() == OrderStatus.PREP;
        order.advance();
        return publish(order, wasPrep ? OrderEvent.Type.ORDER_READY : OrderEvent.Type.ORDER_STARTED);
    }

    @Transactional
    public OrderResponse recall(Long id) {
        Order order = require(id);
        order.recall();
        return publish(order, OrderEvent.Type.ORDER_RECALLED);
    }

    @Transactional
    public OrderResponse collect(Long id) {
        Order order = require(id);
        order.collect();
        return publish(order, OrderEvent.Type.ORDER_COLLECTED);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listActive() {
        return orders.findByStatusInOrderByCreatedAtAsc(List.of(OrderStatus.NEW, OrderStatus.PREP))
                .stream().map(Mappers::toOrder).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listReady() {
        return orders.findByStatusOrderByReadyAtDesc(OrderStatus.READY)
                .stream().map(Mappers::toOrder).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listAll() {
        return orders.findAllByOrderByCreatedAtDesc().stream().map(Mappers::toOrder).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long id) {
        return Mappers.toOrder(require(id));
    }

    private Order require(Long id) {
        return orders.findById(id).orElseThrow(() -> new NotFoundException("Order not found: " + id));
    }

    /** The signed-in staff member's username, or null when created without auth (e.g. tests). */
    private static String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return auth.getName();
    }

    /**
     * Snapshot the order to a DTO while it is still attached, then publish an event.
     * The actual STOMP broadcast is deferred to after commit by the listener.
     */
    private OrderResponse publish(Order order, OrderEvent.Type type) {
        OrderResponse dto = Mappers.toOrder(order);
        events.publishEvent(new OrderEvent(type, dto));
        return dto;
    }
}
