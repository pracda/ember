package com.ember.service;

import com.ember.config.EmberProperties;
import com.ember.domain.MenuItem;
import com.ember.domain.PriceModifier;
import com.ember.web.dto.OrderLineRequest;
import com.ember.web.error.BadRequestException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The server owns pricing. These tests pin the delta arithmetic and the
 * rejection of options an item does not offer, using the same config values as
 * the spec (tax 0.085, meal upcharge 3.50).
 */
class PricingServiceTest {

    private final PricingService pricing = new PricingService(props());

    private static EmberProperties props() {
        EmberProperties p = new EmberProperties();
        p.setTaxRate(new BigDecimal("0.085"));
        p.setMealUpcharge(new BigDecimal("3.50"));
        return p;
    }

    private static MenuItem burger() {
        MenuItem m = new MenuItem("b1", "Ember Smash", "Burgers", new BigDecimal("6.50"), true);
        m.setAddons(List.of(
                new PriceModifier("Extra cheese", new BigDecimal("0.90")),
                new PriceModifier("Bacon", new BigDecimal("1.50")),
                new PriceModifier("No onion", new BigDecimal("0.00"))));
        return m;
    }

    private static MenuItem soda() {
        MenuItem m = new MenuItem("d1", "Fountain Soda", "Drinks", new BigDecimal("2.25"), false);
        m.setSizes(List.of(
                new PriceModifier("Small", new BigDecimal("0.00")),
                new PriceModifier("Medium", new BigDecimal("1.00")),
                new PriceModifier("Large", new BigDecimal("1.80"))));
        return m;
    }

    private static OrderLineRequest line(String itemId, String size, boolean meal, List<String> addons) {
        return new OrderLineRequest(itemId, 1, size, meal, addons, null);
    }

    @Test
    void basePriceWhenNoOptions() {
        BigDecimal price = pricing.unitPrice(burger(), line("b1", null, false, List.of()));
        assertThat(price).isEqualByComparingTo("6.50");
        assertThat(price.scale()).isEqualTo(2);
    }

    @Test
    void addsSizeDelta() {
        assertThat(pricing.unitPrice(soda(), line("d1", "Large", false, List.of())))
                .isEqualByComparingTo("4.05"); // 2.25 + 1.80
    }

    @Test
    void addsAddonDeltas() {
        assertThat(pricing.unitPrice(burger(), line("b1", null, false, List.of("Extra cheese", "Bacon"))))
                .isEqualByComparingTo("8.90"); // 6.50 + 0.90 + 1.50
    }

    @Test
    void addsMealUpcharge() {
        assertThat(pricing.unitPrice(burger(), line("b1", null, true, List.of())))
                .isEqualByComparingTo("10.00"); // 6.50 + 3.50
    }

    @Test
    void combinesMealSizeAndAddons() {
        // burger base 6.50 + meal 3.50 + Extra cheese 0.90 + No onion 0.00
        assertThat(pricing.unitPrice(burger(), line("b1", null, true, List.of("No onion", "Extra cheese"))))
                .isEqualByComparingTo("10.90");
    }

    @Test
    void rejectsSizeNotOffered() {
        assertThatThrownBy(() -> pricing.unitPrice(soda(), line("d1", "Gigantic", false, List.of())))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Gigantic");
    }

    @Test
    void rejectsAddonNotOffered() {
        assertThatThrownBy(() -> pricing.unitPrice(burger(), line("b1", null, false, List.of("Gold flakes"))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Gold flakes");
    }

    @Test
    void rejectsMealOnNonMealItem() {
        assertThatThrownBy(() -> pricing.unitPrice(soda(), line("d1", null, true, List.of())))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("meal");
    }

    @Test
    void taxRoundsHalfUpToTwoPlaces() {
        // 14.95 * 0.085 = 1.27075 -> 1.27
        BigDecimal tax = pricing.tax(new BigDecimal("14.95"));
        assertThat(tax).isEqualByComparingTo("1.27");
        assertThat(tax.scale()).isEqualTo(2);
    }
}
