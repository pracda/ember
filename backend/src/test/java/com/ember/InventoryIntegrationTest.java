package com.ember;

import com.ember.domain.MenuItem;
import com.ember.domain.OrderType;
import com.ember.repository.MenuItemRepository;
import com.ember.service.OrderService;
import com.ember.web.dto.CreateOrderRequest;
import com.ember.web.dto.OrderLineRequest;
import com.ember.web.dto.OrderResponse;
import com.ember.web.error.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Inventory enforcement on the order path: decrement, sold-out rejection, restock on void. */
@SpringBootTest
class InventoryIntegrationTest {

    @Autowired
    private OrderService orders;
    @Autowired
    private MenuItemRepository menu;

    private static CreateOrderRequest orderFor(String itemId, int qty) {
        return new CreateOrderRequest(
                OrderType.DINE_IN, List.of(new OrderLineRequest(itemId, qty, null, false, List.of(), null)));
    }

    @BeforeEach
    void trackB1() {
        MenuItem b1 = menu.findById("b1").orElseThrow();
        b1.setAvailable(true);
        b1.setTracksStock(true);
        b1.setStock(2);
        b1.setLowStockThreshold(1);
        menu.saveAndFlush(b1);
    }

    @Test
    void saleDecrementsStock() {
        orders.create(orderFor("b1", 1));
        assertThat(menu.findById("b1").orElseThrow().getStock()).isEqualTo(1);
    }

    @Test
    void rejectsOrderBeyondStock() {
        assertThatThrownBy(() -> orders.create(orderFor("b1", 3)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsSoldOutItem() {
        MenuItem b1 = menu.findById("b1").orElseThrow();
        b1.setAvailable(false);
        menu.saveAndFlush(b1);
        assertThatThrownBy(() -> orders.create(orderFor("b1", 1)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void voidRestoresStock() {
        OrderResponse created = orders.create(orderFor("b1", 2));
        assertThat(menu.findById("b1").orElseThrow().getStock()).isZero();
        orders.voidOrder(created.id(), "mistake");
        assertThat(menu.findById("b1").orElseThrow().getStock()).isEqualTo(2);
    }
}
