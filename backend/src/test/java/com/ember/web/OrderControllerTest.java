package com.ember.web;

import com.ember.domain.OrderStatus;
import com.ember.domain.OrderType;
import com.ember.service.OrderService;
import com.ember.web.dto.OrderResponse;
import com.ember.web.error.InvalidTransitionException;
import com.ember.web.error.NotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer slice for the order endpoints: status codes, the created-order body,
 * and the mapping of domain/validation errors to RFC 7807 problem responses via
 * {@link GlobalExceptionHandler}.
 */
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @MockitoBean
    private OrderService orders;

    private static OrderResponse sample() {
        var line = new OrderResponse.OrderLineResponse(
                1L, "b1", "Ember Smash", 1, null, false, List.of(), null,
                new BigDecimal("6.50"), new BigDecimal("6.50"));
        return new OrderResponse(
                1L, 1, OrderType.DINE_IN, OrderStatus.NEW, List.of(line),
                new BigDecimal("6.50"), new BigDecimal("0.55"), new BigDecimal("7.05"),
                Instant.now(), null, null, null);
    }

    @Test
    void createReturns201WithTicketAndTotals() throws Exception {
        when(orders.create(any())).thenReturn(sample());
        String body = """
                {"type":"DINE_IN","lines":[{"itemId":"b1","quantity":1}]}""";

        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketNumber").value(1))
                .andExpect(jsonPath("$.total").value(7.05));
    }

    @Test
    void createWithEmptyLinesReturns400() throws Exception {
        String body = """
                {"type":"DINE_IN","lines":[]}""";

        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void illegalTransitionReturns409() throws Exception {
        when(orders.advance(eq(1L)))
                .thenThrow(new InvalidTransitionException(1L, OrderStatus.READY, "advance"));

        mvc.perform(post("/api/orders/1/advance"))
                .andExpect(status().isConflict());
    }

    @Test
    void unknownOrderReturns404() throws Exception {
        when(orders.get(eq(99L))).thenThrow(new NotFoundException("Order not found: 99"));

        mvc.perform(get("/api/orders/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listReturnsActiveOrders() throws Exception {
        when(orders.listActive()).thenReturn(List.of(sample()));

        mvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("NEW"));
    }

    @Test
    void readyStatusReturnsReadyList() throws Exception {
        when(orders.listReady()).thenReturn(List.of(sample()));

        mvc.perform(get("/api/orders").param("status", "ready"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }
}
