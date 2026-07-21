package com.ember.service;

import com.ember.config.EmberProperties;
import com.ember.domain.MenuItem;
import com.ember.domain.Order;
import com.ember.domain.OrderLine;
import com.ember.domain.OrderStatus;
import com.ember.domain.OrderType;
import com.ember.domain.Staff;
import com.ember.repository.MenuItemRepository;
import com.ember.repository.OrderRepository;
import com.ember.repository.StaffRepository;
import com.ember.web.dto.AnalyticsResponse;
import com.ember.web.dto.AnalyticsResponse.CategorySales;
import com.ember.web.dto.AnalyticsResponse.DailySales;
import com.ember.web.dto.AnalyticsResponse.HourCount;
import com.ember.web.dto.AnalyticsResponse.ItemSales;
import com.ember.web.dto.AnalyticsResponse.StaffSales;
import com.ember.web.dto.AnalyticsResponse.TypeSales;
import com.ember.web.dto.DaySummaryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Manager reporting: the day summary and the date-range analytics dashboard. */
@Service
public class ReportService {

    private final OrderRepository orders;
    private final MenuItemRepository menu;
    private final StaffRepository staff;
    private final EmberProperties props;

    public ReportService(OrderRepository orders, MenuItemRepository menu,
                         StaffRepository staff, EmberProperties props) {
        this.orders = orders;
        this.menu = menu;
        this.staff = staff;
        this.props = props;
    }

    @Transactional(readOnly = true)
    public DaySummaryResponse daySummary(LocalDate date) {
        List<Order> dayOrders = ordersOn(date, date);
        List<Order> net = netSales(dayOrders);

        BigDecimal revenue = revenueOf(net);
        int collected = (int) dayOrders.stream().filter(o -> o.getStatus() == OrderStatus.DONE).count();
        Long avgPrep = averagePrepSeconds(dayOrders);

        return new DaySummaryResponse(date, net.size(), collected, revenue, avgPrep);
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse analytics(LocalDate from, LocalDate to) {
        ZoneId zone = props.getTimezone();
        List<Order> range = ordersOn(from, to);

        // Voided orders never happened; refunded orders' money was returned — both
        // drop out of the net-sales figures, but are reported as their own counts.
        List<Order> net = netSales(range);
        long voided = range.stream().filter(o -> o.getStatus() == OrderStatus.VOIDED).count();
        List<Order> refunded = range.stream().filter(o -> o.getStatus() == OrderStatus.REFUNDED).toList();

        int orderCount = net.size();
        BigDecimal revenue = revenueOf(net);
        BigDecimal avg = orderCount == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : revenue.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP);

        return new AnalyticsResponse(
                from, to, orderCount, revenue, avg,
                (int) voided, refunded.size(), revenueOf(refunded),
                salesByDay(net, from, to, zone),
                topItems(net),
                byCategory(net),
                byOrderType(net),
                byHour(net, zone),
                byStaff(net));
    }

    /** Orders that count as sales — everything except VOIDED and REFUNDED. */
    private static List<Order> netSales(List<Order> list) {
        return list.stream()
                .filter(o -> o.getStatus() != OrderStatus.VOIDED && o.getStatus() != OrderStatus.REFUNDED)
                .toList();
    }

    /* ----- aggregations ----- */

    private List<DailySales> salesByDay(List<Order> range, LocalDate from, LocalDate to, ZoneId zone) {
        Map<LocalDate, int[]> counts = new LinkedHashMap<>();
        Map<LocalDate, BigDecimal> money = new LinkedHashMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            counts.put(d, new int[1]);
            money.put(d, BigDecimal.ZERO);
        }
        for (Order o : range) {
            LocalDate day = LocalDate.ofInstant(o.getCreatedAt(), zone);
            counts.get(day)[0]++;
            money.put(day, money.get(day).add(o.getTotal()));
        }
        List<DailySales> out = new ArrayList<>();
        counts.forEach((day, c) -> out.add(new DailySales(day, c[0], scale(money.get(day)))));
        return out;
    }

    private List<ItemSales> topItems(List<Order> range) {
        Map<String, int[]> qty = new LinkedHashMap<>();
        Map<String, BigDecimal> rev = new LinkedHashMap<>();
        Map<String, String> names = new LinkedHashMap<>();
        for (Order o : range) {
            for (OrderLine l : o.getLines()) {
                qty.computeIfAbsent(l.getMenuItemId(), k -> new int[1])[0] += l.getQuantity();
                rev.merge(l.getMenuItemId(), l.lineTotal(), BigDecimal::add);
                names.putIfAbsent(l.getMenuItemId(), l.getItemName());
            }
        }
        return qty.keySet().stream()
                .map(id -> new ItemSales(id, names.get(id), qty.get(id)[0], scale(rev.get(id))))
                .sorted(Comparator.comparing(ItemSales::revenue).reversed())
                .limit(10)
                .toList();
    }

    private List<CategorySales> byCategory(List<Order> range) {
        Map<String, String> itemCategory = new LinkedHashMap<>();
        for (MenuItem m : menu.findAll()) {
            itemCategory.put(m.getId(), m.getCategory());
        }
        Map<String, int[]> qty = new LinkedHashMap<>();
        Map<String, BigDecimal> rev = new LinkedHashMap<>();
        for (Order o : range) {
            for (OrderLine l : o.getLines()) {
                String category = itemCategory.getOrDefault(l.getMenuItemId(), "Other");
                qty.computeIfAbsent(category, k -> new int[1])[0] += l.getQuantity();
                rev.merge(category, l.lineTotal(), BigDecimal::add);
            }
        }
        return qty.keySet().stream()
                .map(c -> new CategorySales(c, qty.get(c)[0], scale(rev.get(c))))
                .sorted(Comparator.comparing(CategorySales::revenue).reversed())
                .toList();
    }

    private List<TypeSales> byOrderType(List<Order> range) {
        List<TypeSales> out = new ArrayList<>();
        for (OrderType type : OrderType.values()) {
            List<Order> ofType = range.stream().filter(o -> o.getType() == type).toList();
            if (!ofType.isEmpty()) {
                out.add(new TypeSales(type, ofType.size(), revenueOf(ofType)));
            }
        }
        return out;
    }

    private List<HourCount> byHour(List<Order> range, ZoneId zone) {
        int[] hours = new int[24];
        for (Order o : range) {
            hours[o.getCreatedAt().atZone(zone).getHour()]++;
        }
        List<HourCount> out = new ArrayList<>(24);
        for (int h = 0; h < 24; h++) {
            out.add(new HourCount(h, hours[h]));
        }
        return out;
    }

    private List<StaffSales> byStaff(List<Order> range) {
        Map<String, String> displayNames = new LinkedHashMap<>();
        for (Staff s : staff.findAll()) {
            displayNames.put(s.getUsername(), s.getDisplayName());
        }
        Map<String, int[]> count = new LinkedHashMap<>();
        Map<String, BigDecimal> rev = new LinkedHashMap<>();
        for (Order o : range) {
            String key = o.getServedBy() == null ? "" : o.getServedBy();
            count.computeIfAbsent(key, k -> new int[1])[0]++;
            rev.merge(key, o.getTotal(), BigDecimal::add);
        }
        return count.keySet().stream()
                .map(username -> new StaffSales(
                        username.isEmpty() ? null : username,
                        username.isEmpty() ? "Unassigned" : displayNames.getOrDefault(username, username),
                        count.get(username)[0], scale(rev.get(username))))
                .sorted(Comparator.comparing(StaffSales::revenue).reversed())
                .toList();
    }

    /* ----- helpers ----- */

    private List<Order> ordersOn(LocalDate from, LocalDate to) {
        ZoneId zone = props.getTimezone();
        Instant start = from.atStartOfDay(zone).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(zone).toInstant();
        return orders.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end);
    }

    private static BigDecimal revenueOf(List<Order> list) {
        return scale(list.stream().map(Order::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private static Long averagePrepSeconds(List<Order> list) {
        List<Long> preps = list.stream()
                .filter(o -> o.getStartedAt() != null && o.getReadyAt() != null)
                .map(o -> Duration.between(o.getStartedAt(), o.getReadyAt()).getSeconds())
                .toList();
        return preps.isEmpty() ? null : Math.round(preps.stream().mapToLong(Long::longValue).average().orElse(0));
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
