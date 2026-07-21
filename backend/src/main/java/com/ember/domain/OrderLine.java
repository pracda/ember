package com.ember.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * One line on an order. Everything the kitchen needs to read is snapshotted here
 * at order time — the item name, the chosen size/add-ons, and the computed unit
 * price — so that later menu or price changes never rewrite a historical ticket.
 */
@Entity
@Table(name = "order_line")
public class OrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Reference back to the menu item that was ordered (for reporting). */
    @Column(name = "menu_item_id", nullable = false)
    private String menuItemId;

    /** Snapshot of the item name at order time. */
    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(nullable = false)
    private int quantity;

    /** Chosen size label, or null if the item has no size. */
    private String size;

    @Column(name = "is_meal", nullable = false)
    private boolean meal;

    /** Chosen add-ons / requests ("No onion", "Extra cheese"). */
    @ElementCollection
    @CollectionTable(name = "order_line_addon", joinColumns = @JoinColumn(name = "order_line_id"))
    @Column(name = "addon")
    private List<String> addons = new ArrayList<>();

    /** Free-text kitchen note. */
    @Column(length = 255)
    private String notes;

    /** Server-computed unit price, frozen at order time. */
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    public OrderLine() { }

    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public Long getId() { return id; }

    public String getMenuItemId() { return menuItemId; }
    public void setMenuItemId(String menuItemId) { this.menuItemId = menuItemId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public boolean isMeal() { return meal; }
    public void setMeal(boolean meal) { this.meal = meal; }

    public List<String> getAddons() { return addons; }
    public void setAddons(List<String> addons) { this.addons = addons; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
}
