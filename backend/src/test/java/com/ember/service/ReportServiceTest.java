package com.ember.service;

import com.ember.config.EmberProperties;
import com.ember.domain.MenuItem;
import com.ember.domain.Order;
import com.ember.domain.OrderLine;
import com.ember.domain.OrderType;
import com.ember.domain.Staff;
import com.ember.domain.StaffRole;
import com.ember.domain.TimeEntry;
import com.ember.repository.MenuItemRepository;
import com.ember.repository.OrderRepository;
import com.ember.repository.StaffRepository;
import com.ember.repository.TimeEntryRepository;
import com.ember.web.dto.LaborRow;
import com.ember.web.dto.MenuItemResponse;
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
    @Autowired
    private MenuItemRepository menu;
    @Autowired
    private StaffRepository staff;
    @Autowired
    private TimeEntryRepository timeEntries;

    private int ticket = 1;

    private ReportService service() {
        EmberProperties props = new EmberProperties();
        props.setTimezone(ZoneId.of("UTC"));
        return new ReportService(orders, menu, staff, timeEntries, props);
    }

    private void persist(String total, boolean toReady) {
        Order o = new Order(ticket++, OrderType.DINE_IN);
        o.setTotal(new BigDecimal(total));
        if (toReady) {
            o.advance();
            o.advance();
        }
        orders.saveAndFlush(o);
    }

    private OrderLine line(String menuItemId, String name, int qty, String unit) {
        OrderLine l = new OrderLine();
        l.setMenuItemId(menuItemId);
        l.setItemName(name);
        l.setQuantity(qty);
        l.setUnitPrice(new BigDecimal(unit));
        return l;
    }

    private void persistOrder(OrderType type, String total, String servedBy, OrderLine... lines) {
        Order o = new Order(ticket++, type);
        o.setTotal(new BigDecimal(total));
        o.setServedBy(servedBy);
        for (OrderLine l : lines) o.addLine(l);
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
    void voidedAndRefundedExcludedFromNetSales() {
        persistOrder(OrderType.DINE_IN, "10.00", "cashier", line("b1", "Ember Smash", 1, "10.00"));

        Order voided = new Order(ticket++, OrderType.DINE_IN);
        voided.setTotal(new BigDecimal("5.00"));
        voided.voidOrder();
        orders.saveAndFlush(voided);

        Order refunded = new Order(ticket++, OrderType.DINE_IN);
        refunded.setTotal(new BigDecimal("8.00"));
        refunded.advance();
        refunded.advance();
        refunded.collect();
        refunded.refund();
        orders.saveAndFlush(refunded);

        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        var a = service().analytics(today, today);

        assertThat(a.orderCount()).isEqualTo(1);
        assertThat(a.revenue()).isEqualByComparingTo("10.00");
        assertThat(a.voidedCount()).isEqualTo(1);
        assertThat(a.refundedCount()).isEqualTo(1);
        assertThat(a.refundedAmount()).isEqualByComparingTo("8.00");
    }

    @Test
    void lowStockListsTrackedItemsAtOrBelowThreshold() {
        MenuItem low = new MenuItem("b1", "Ember Smash", "Burgers", new BigDecimal("6.50"), true);
        low.setTracksStock(true);
        low.setStock(2);
        low.setLowStockThreshold(3);
        menu.saveAndFlush(low);

        MenuItem plenty = new MenuItem("d1", "Fountain Soda", "Drinks", new BigDecimal("2.25"), false);
        plenty.setTracksStock(true);
        plenty.setStock(50);
        plenty.setLowStockThreshold(5);
        menu.saveAndFlush(plenty);

        assertThat(service().lowStock()).extracting(MenuItemResponse::id).containsExactly("b1");
    }

    @Test
    void laborCombinesHoursAndSalesPerStaff() {
        Staff amy = staff.saveAndFlush(new Staff("amy", "Amy", StaffRole.CASHIER));
        TimeEntry entry = new TimeEntry(amy.getId());
        entry.clockOut();
        timeEntries.saveAndFlush(entry);
        persistOrder(OrderType.DINE_IN, "10.00", "amy", line("b1", "Ember Smash", 1, "10.00"));

        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        var rows = service().labor(today, today);

        var amyRow = rows.stream().filter(r -> "Amy".equals(r.displayName())).findFirst().orElseThrow();
        assertThat(amyRow.sales()).isEqualByComparingTo("10.00");
        assertThat(amyRow.ordersServed()).isEqualTo(1);
        assertThat(amyRow.hoursWorked()).isNotNull();
        assertThat(amyRow.salesPerHour()).isNotNull();
    }

    @Test
    void emptyDayIsZeroed() {
        persist("10.00", false);

        var summary = service().daySummary(LocalDate.now(ZoneId.of("UTC")).minusDays(1));

        assertThat(summary.orderCount()).isZero();
        assertThat(summary.revenue()).isEqualByComparingTo("0.00");
        assertThat(summary.avgPrepSeconds()).isNull();
    }

    @Test
    void analyticsAggregatesItemsCategoriesTypesAndStaff() {
        menu.saveAndFlush(new MenuItem("b1", "Ember Smash", "Burgers", new BigDecimal("6.50"), true));
        menu.saveAndFlush(new MenuItem("d1", "Fountain Soda", "Drinks", new BigDecimal("2.25"), false));

        persistOrder(OrderType.DINE_IN, "16.00", "cashier",
                line("b1", "Ember Smash", 2, "6.50"), line("d1", "Fountain Soda", 1, "3.00"));
        persistOrder(OrderType.TO_GO, "6.50", "cook", line("b1", "Ember Smash", 1, "6.50"));

        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        var a = service().analytics(today, today);

        assertThat(a.orderCount()).isEqualTo(2);
        assertThat(a.revenue()).isEqualByComparingTo("22.50");
        assertThat(a.avgOrderValue()).isEqualByComparingTo("11.25");

        // top items: Ember Smash 3 @ 6.50 = 19.50, Fountain Soda 1 @ 3.00
        assertThat(a.topItems().get(0).itemName()).isEqualTo("Ember Smash");
        assertThat(a.topItems().get(0).quantity()).isEqualTo(3);
        assertThat(a.topItems().get(0).revenue()).isEqualByComparingTo("19.50");

        // categories reconcile
        assertThat(a.byCategory()).anySatisfy(c -> {
            assertThat(c.category()).isEqualTo("Burgers");
            assertThat(c.revenue()).isEqualByComparingTo("19.50");
        });

        // order type split
        assertThat(a.byOrderType()).hasSize(2);

        // staff sales
        assertThat(a.byStaff()).extracting(s -> s.displayName())
                .contains("cashier", "cook"); // no Staff rows persisted → falls back to username

        // hours always cover 24 buckets
        assertThat(a.byHour()).hasSize(24);
        assertThat(a.salesByDay()).hasSize(1);
    }
}
