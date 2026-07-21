package com.ember.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.List;

/**
 * Business configuration, bound from the {@code ember.*} keys in application.yml.
 * Keeping tax and the meal upcharge here means outlets can be re-priced without
 * touching code.
 */
@ConfigurationProperties(prefix = "ember")
public class EmberProperties {

    /** Sales tax applied to the order subtotal, e.g. 0.085 for 8.5%. */
    private BigDecimal taxRate = new BigDecimal("0.085");

    /** Flat upcharge for turning an eligible item into a meal (fries + drink). */
    private BigDecimal mealUpcharge = new BigDecimal("3.50");

    /** Origins allowed to call the REST API and open the WebSocket (the three station apps). */
    private List<String> allowedOrigins = List.of("http://localhost:5173", "http://localhost:3000");

    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }

    public BigDecimal getMealUpcharge() { return mealUpcharge; }
    public void setMealUpcharge(BigDecimal mealUpcharge) { this.mealUpcharge = mealUpcharge; }

    public List<String> getAllowedOrigins() { return allowedOrigins; }
    public void setAllowedOrigins(List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }
}
