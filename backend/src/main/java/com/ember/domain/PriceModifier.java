package com.ember.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

/**
 * A selectable option on a menu item — a size ("Large") or an add-on ("Bacon") —
 * together with what it adds to the price. Embedded into {@link MenuItem}.
 */
@Embeddable
public class PriceModifier {

    @Column(nullable = false)
    private String label;

    @Column(name = "price_delta", nullable = false, precision = 8, scale = 2)
    private BigDecimal priceDelta = BigDecimal.ZERO;

    protected PriceModifier() { }

    public PriceModifier(String label, BigDecimal priceDelta) {
        this.label = label;
        this.priceDelta = priceDelta;
    }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public BigDecimal getPriceDelta() { return priceDelta; }
    public void setPriceDelta(BigDecimal priceDelta) { this.priceDelta = priceDelta; }
}
