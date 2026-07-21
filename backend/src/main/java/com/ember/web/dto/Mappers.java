package com.ember.web.dto;

import com.ember.domain.MenuItem;
import com.ember.domain.Order;
import com.ember.domain.OrderLine;
import com.ember.domain.PriceModifier;

import java.util.List;

/** Entity → DTO mapping. Kept in one place so the wire shape is easy to see and change. */
public final class Mappers {

    private Mappers() { }

    public static OrderResponse toOrder(Order o) {
        List<OrderResponse.OrderLineResponse> lines = o.getLines().stream()
                .map(Mappers::toLine)
                .toList();
        return new OrderResponse(
                o.getId(), o.getTicketNumber(), o.getType(), o.getStatus(), lines,
                o.getSubtotal(), o.getTax(), o.getTotal(),
                o.getCreatedAt(), o.getStartedAt(), o.getReadyAt(), o.getCollectedAt(), o.getReason());
    }

    public static OrderResponse.OrderLineResponse toLine(OrderLine l) {
        return new OrderResponse.OrderLineResponse(
                l.getId(), l.getMenuItemId(), l.getItemName(), l.getQuantity(),
                l.getSize(), l.isMeal(), List.copyOf(l.getAddons()), l.getNotes(),
                l.getUnitPrice(), l.lineTotal());
    }

    public static MenuItemResponse toMenuItem(MenuItem m) {
        return new MenuItemResponse(
                m.getId(), m.getName(), m.getCategory(), m.getBasePrice(), m.isMealAvailable(),
                m.getSizes().stream().map(Mappers::toModifier).toList(),
                m.getAddons().stream().map(Mappers::toModifier).toList());
    }

    private static MenuItemResponse.ModifierResponse toModifier(PriceModifier p) {
        return new MenuItemResponse.ModifierResponse(p.getLabel(), p.getPriceDelta());
    }
}
