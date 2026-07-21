package com.ember.web.dto;

import com.ember.domain.OrderType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** A date-range analytics snapshot for the manager dashboard. */
public record AnalyticsResponse(
        LocalDate from,
        LocalDate to,
        int orderCount,
        BigDecimal revenue,
        BigDecimal avgOrderValue,
        int voidedCount,
        int refundedCount,
        BigDecimal refundedAmount,
        List<DailySales> salesByDay,
        List<ItemSales> topItems,
        List<CategorySales> byCategory,
        List<TypeSales> byOrderType,
        List<HourCount> byHour,
        List<StaffSales> byStaff
) {
    public record DailySales(LocalDate date, int orderCount, BigDecimal revenue) { }

    public record ItemSales(String menuItemId, String itemName, int quantity, BigDecimal revenue) { }

    public record CategorySales(String category, int quantity, BigDecimal revenue) { }

    public record TypeSales(OrderType type, int orderCount, BigDecimal revenue) { }

    public record HourCount(int hour, int orderCount) { }

    public record StaffSales(String username, String displayName, int orderCount, BigDecimal revenue) { }
}
