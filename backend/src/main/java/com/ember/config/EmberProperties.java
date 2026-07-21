package com.ember.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZoneId;
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

    /** Origins allowed to call the REST API and open the WebSocket (the station apps). */
    private List<String> allowedOrigins = List.of("http://localhost:5173", "http://localhost:3000");

    /** Outlet timezone — day boundaries for the day-summary report are computed in this zone. */
    private ZoneId timezone = ZoneId.of("UTC");

    /** JWT settings for staff authentication. */
    private final Jwt jwt = new Jwt();

    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }

    public BigDecimal getMealUpcharge() { return mealUpcharge; }
    public void setMealUpcharge(BigDecimal mealUpcharge) { this.mealUpcharge = mealUpcharge; }

    public List<String> getAllowedOrigins() { return allowedOrigins; }
    public void setAllowedOrigins(List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }

    public ZoneId getTimezone() { return timezone; }
    public void setTimezone(ZoneId timezone) { this.timezone = timezone; }

    public Jwt getJwt() { return jwt; }

    /** JWT signing secret (HMAC) and token lifetime. */
    public static class Jwt {
        /**
         * HMAC-SHA256 signing key. Must be at least 32 chars (256 bits). The default is a
         * dev-only value — override with {@code EMBER_JWT_SECRET} in production.
         */
        private String secret = "dev-only-ember-jwt-secret-change-me-in-production-please";
        private Duration ttl = Duration.ofHours(12);

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }

        public Duration getTtl() { return ttl; }
        public void setTtl(Duration ttl) { this.ttl = ttl; }
    }
}
