package com.ember.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A day's trading summary for the manager view.
 *
 * @param date            the business date (outlet timezone)
 * @param orderCount      orders created that day
 * @param collectedCount  of those, how many were collected (DONE)
 * @param revenue         sum of order totals for the day
 * @param avgPrepSeconds  average PREP→READY time in seconds, or null if none reached ready
 */
public record DaySummaryResponse(
        LocalDate date,
        int orderCount,
        int collectedCount,
        BigDecimal revenue,
        Long avgPrepSeconds
) { }
