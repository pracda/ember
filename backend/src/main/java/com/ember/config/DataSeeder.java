package com.ember.config;

import com.ember.domain.MenuItem;
import com.ember.domain.PriceModifier;
import com.ember.repository.MenuItemRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

/**
 * Seeds the menu once, on an empty database, so the API is usable immediately and
 * matches the prototype the POS was designed against. In production the menu is
 * managed through an admin tool, not this seeder.
 */
@Configuration
public class DataSeeder {

    private static final List<PriceModifier> SIZES = List.of(
            new PriceModifier("Small", bd("0.00")),
            new PriceModifier("Medium", bd("1.00")),
            new PriceModifier("Large", bd("1.80")));

    private static final List<PriceModifier> BURGER_ADDONS = List.of(
            new PriceModifier("Extra cheese", bd("0.90")),
            new PriceModifier("Bacon", bd("1.50")),
            new PriceModifier("No onion", bd("0.00")),
            new PriceModifier("No pickle", bd("0.00")),
            new PriceModifier("Extra spicy", bd("0.00")));

    private static final List<PriceModifier> CHICKEN_ADDONS = List.of(
            new PriceModifier("Make it spicy", bd("0.00")),
            new PriceModifier("Ranch dip", bd("0.50")),
            new PriceModifier("Extra crispy", bd("0.00")));

    @Bean
    CommandLineRunner seedMenu(MenuItemRepository repo) {
        return args -> {
            if (repo.count() > 0) {
                return;
            }
            repo.saveAll(List.of(
                    burger("b1", "Ember Smash", "6.50"),
                    burger("b2", "Double Char", "8.75"),
                    burger("b3", "Bacon Stack", "9.25"),
                    burger("b4", "Veggie Flame", "7.00"),
                    chicken("c1", "Crispy Tenders", "6.25"),
                    chicken("c2", "Spicy Sandwich", "7.50"),
                    chicken("c3", "Nashville Hot", "8.00"),
                    sized("s1", "Fries", "Sides", "3.00"),
                    plain("s2", "Loaded Fries", "Sides", "5.50"),
                    sized("s3", "Onion Rings", "Sides", "4.25"),
                    plain("s4", "Side Salad", "Sides", "4.00"),
                    sized("d1", "Fountain Soda", "Drinks", "2.25"),
                    sized("d2", "Iced Tea", "Drinks", "2.25"),
                    sized("d3", "Milkshake", "Drinks", "4.50"),
                    plain("d4", "Bottled Water", "Drinks", "1.75"),
                    plain("w1", "Cookie", "Sweets", "2.00"),
                    sized("w2", "Soft Serve", "Sweets", "3.25"),
                    plain("w3", "Apple Pie", "Sweets", "2.75")));
        };
    }

    private static MenuItem burger(String id, String name, String price) {
        MenuItem m = new MenuItem(id, name, "Burgers", bd(price), true);
        m.setAddons(new java.util.ArrayList<>(BURGER_ADDONS));
        return m;
    }

    private static MenuItem chicken(String id, String name, String price) {
        MenuItem m = new MenuItem(id, name, "Chicken", bd(price), true);
        m.setAddons(new java.util.ArrayList<>(CHICKEN_ADDONS));
        return m;
    }

    private static MenuItem sized(String id, String name, String category, String price) {
        MenuItem m = new MenuItem(id, name, category, bd(price), false);
        m.setSizes(new java.util.ArrayList<>(SIZES));
        return m;
    }

    private static MenuItem plain(String id, String name, String category, String price) {
        return new MenuItem(id, name, category, bd(price), false);
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
