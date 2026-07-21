package com.ember.web;

import com.ember.service.OrderService;
import com.ember.web.dto.CreateOrderRequest;
import com.ember.web.dto.OrderResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The POS posts new orders here; the kitchen and board read and advance them.
 * Every state change also fans out over WebSocket, so polling these endpoints is
 * only needed for the initial load or as a fallback.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    /** Create a ticket from the POS. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
        return orders.create(request);
    }

    /** List orders. {@code ?status=active} (default) returns the kitchen rail; {@code all} returns history. */
    @GetMapping
    public List<OrderResponse> list(@RequestParam(defaultValue = "active") String status) {
        return "all".equalsIgnoreCase(status) ? orders.listAll() : orders.listActive();
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id) {
        return orders.get(id);
    }

    /** NEW → PREP (start cooking) or PREP → READY (mark ready). */
    @PostMapping("/{id}/advance")
    public OrderResponse advance(@PathVariable Long id) {
        return orders.advance(id);
    }

    /** READY → PREP (bring a bumped order back). */
    @PostMapping("/{id}/recall")
    public OrderResponse recall(@PathVariable Long id) {
        return orders.recall(id);
    }

    /** READY → DONE (customer collected). */
    @PostMapping("/{id}/collect")
    public OrderResponse collect(@PathVariable Long id) {
        return orders.collect(id);
    }
}
