package com.ember.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A sellable item on the menu. The id is a short human code ("b1", "d3") so it is
 * stable and readable across the POS, receipts and the seed data.
 *
 * <p>Sizes and add-ons carry their own price deltas, so the server is the single
 * source of truth for pricing — the POS never sends a price it computed itself.</p>
 */
@Entity
@Table(name = "menu_item")
public class MenuItem {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;

    @Column(name = "base_price", nullable = false, precision = 8, scale = 2)
    private BigDecimal basePrice;

    /** Optional size choices; empty when the item has no size. */
    @ElementCollection
    @CollectionTable(name = "menu_item_size", joinColumns = @JoinColumn(name = "menu_item_id"))
    private List<PriceModifier> sizes = new ArrayList<>();

    /** Optional add-ons / special requests. */
    @ElementCollection
    @CollectionTable(name = "menu_item_addon", joinColumns = @JoinColumn(name = "menu_item_id"))
    private List<PriceModifier> addons = new ArrayList<>();

    /** When true, the item can be upgraded to a meal for the configured upcharge. */
    @Column(name = "meal_available", nullable = false)
    private boolean mealAvailable = false;

    /** Manual "86" switch — false means sold out regardless of stock. */
    @Column(nullable = false)
    private boolean available = true;

    /** When true, {@link #stock} is decremented on sale and enforced. */
    @Column(name = "tracks_stock", nullable = false)
    private boolean tracksStock = false;

    @Column(nullable = false)
    private int stock = 0;

    @Column(name = "low_stock_threshold", nullable = false)
    private int lowStockThreshold = 0;

    protected MenuItem() { }

    public MenuItem(String id, String name, String category, BigDecimal basePrice, boolean mealAvailable) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.basePrice = basePrice;
        this.mealAvailable = mealAvailable;
    }

    /** Look up a size option by its label. */
    public Optional<PriceModifier> findSize(String label) {
        return sizes.stream().filter(s -> s.getLabel().equals(label)).findFirst();
    }

    /** Look up an add-on option by its label. */
    public Optional<PriceModifier> findAddon(String label) {
        return addons.stream().filter(a -> a.getLabel().equals(label)).findFirst();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }

    public List<PriceModifier> getSizes() { return sizes; }
    public void setSizes(List<PriceModifier> sizes) { this.sizes = sizes; }

    public List<PriceModifier> getAddons() { return addons; }
    public void setAddons(List<PriceModifier> addons) { this.addons = addons; }

    public boolean isMealAvailable() { return mealAvailable; }
    public void setMealAvailable(boolean mealAvailable) { this.mealAvailable = mealAvailable; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public boolean isTracksStock() { return tracksStock; }
    public void setTracksStock(boolean tracksStock) { this.tracksStock = tracksStock; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public int getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(int lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }

    /** Effective sold-out: manually 86'd, or tracking stock and none left. */
    public boolean isSoldOut() {
        return !available || (tracksStock && stock <= 0);
    }

    /** Running low: tracking stock and at/below the alert threshold (includes sold out). */
    public boolean isLowStock() {
        return tracksStock && stock <= lowStockThreshold;
    }
}
