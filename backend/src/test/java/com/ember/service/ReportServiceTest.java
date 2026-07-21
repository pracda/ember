package com.ember.service;

import com.ember.config.EmberProperties;
import com.ember.domain.Order;
import com.ember.domain.OrderType;
import com.ember.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ReportServiceTest {

    @Autowired
    private OrderRepository orders;

    private int ticket = 1;

    private ReportService service() {
        EmberProperties props = new EmberProperties();
        props.setTimezone(ZoneId.of("UTC"));
        return new ReportService(orders, props);
    }

    private void persist(String total, boolean toReady) {
        Order o = new Order(ticket++, OrderType.DINE_IN);
        o.setTotal(new BigDecimal(total));
        if (toReady) {
            o.advance(); // NEW -> PREP (startedAt)
            o.advance(); // PREP -> READY (readyAt)
        }
        orders.saveAndFlush(o);
    }

    @Test
    void summarisesTodayCountRevenueAndPrepTime() {
        persist("10.00", true);
        persist("5.50", true);
        persist("3.00", false);

        var summary = service().daySummary(LocalDate.now(ZoneId.of("UTC")));

        assertThat(summary.orderCount()).isEqualTo(3);
        assertThat(summary.revenue()).isEqualByComparingTo("18.50");
        assertThat(summary.avgPrepSeconds()).isNotNull().isGreaterThanOrEqualTo(0L);
        assertThat(summary.collectedCount()).isZero();
    }

    @Test
    void emptyDayIsZeroed() {
        persist("10.00", false);

        var summary = service().daySummary(LocalDate.now(ZoneId.of("UTC")).minusDays(1));

        assertThat(summary.orderCount()).isZero();
        assertThat(summary.revenue()).isEqualByComparingTo("0.00");
        assertThat(summary.avgPrepSeconds()).isNull();
    }
}
