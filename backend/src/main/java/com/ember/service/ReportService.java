package com.ember.service;

import com.ember.config.EmberProperties;
import com.ember.domain.Order;
import com.ember.domain.OrderStatus;
import com.ember.repository.OrderRepository;
import com.ember.web.dto.DaySummaryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/** Computes the manager day-summary from order timestamps, in the outlet timezone. */
@Service
public class ReportService {

    private final OrderRepository orders;
    private final EmberProperties props;

    public ReportService(OrderRepository orders, EmberProperties props) {
        this.orders = orders;
        this.props = props;
    }

    @Transactional(readOnly = true)
    public DaySummaryResponse daySummary(LocalDate date) {
        ZoneId zone = props.getTimezone();
        Instant start = date.atStartOfDay(zone).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(zone).toInstant();

        List<Order> dayOrders = orders.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end);

        BigDecimal revenue = dayOrders.stream()
                .map(Order::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        int collected = (int) dayOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DONE)
                .count();

        List<Long> prepSeconds = dayOrders.stream()
                .filter(o -> o.getStartedAt() != null && o.getReadyAt() != null)
                .map(o -> Duration.between(o.getStartedAt(), o.getReadyAt()).getSeconds())
                .toList();

        Long avgPrep = prepSeconds.isEmpty()
                ? null
                : Math.round(prepSeconds.stream().mapToLong(Long::longValue).average().orElse(0));

        return new DaySummaryResponse(date, dayOrders.size(), collected, revenue, avgPrep);
    }
}
