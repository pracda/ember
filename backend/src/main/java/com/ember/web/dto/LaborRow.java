package com.ember.web.dto;

import java.math.BigDecimal;

/** One staff member's hours + sales for the shift-performance report. */
public record LaborRow(
        Long staffId,
        String displayName,
        BigDecimal hoursWorked,
        BigDecimal sales,
        int ordersServed,
        BigDecimal salesPerHour
) { }
