package com.ember.service;

import com.ember.config.EmberProperties;
import com.ember.domain.MenuItem;
import com.ember.web.dto.OrderLineRequest;
import com.ember.web.error.BadRequestException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The single source of truth for prices. Given a menu item and the customer's
 * choices, it validates the choices and returns the unit price. The POS never
 * sends a price of its own.
 */
@Service
public class PricingService {

    private final EmberProperties props;

    public PricingService(EmberProperties props) {
        this.props = props;
    }

    /** Compute the (pre-tax) unit price for one line, rejecting options the item does not offer. */
    public BigDecimal unitPrice(MenuItem item, OrderLineRequest line) {
        BigDecimal price = item.getBasePrice();

        if (line.size() != null && !line.size().isBlank()) {
            var size = item.findSize(line.size())
                    .orElseThrow(() -> new BadRequestException(
                            "Size '" + line.size() + "' is not available for " + item.getName()));
            price = price.add(size.getPriceDelta());
        }

        for (String addonLabel : line.addons()) {
            var addon = item.findAddon(addonLabel)
                    .orElseThrow(() -> new BadRequestException(
                            "Add-on '" + addonLabel + "' is not available for " + item.getName()));
            price = price.add(addon.getPriceDelta());
        }

        if (line.meal()) {
            if (!item.isMealAvailable()) {
                throw new BadRequestException(item.getName() + " cannot be made a meal");
            }
            price = price.add(props.getMealUpcharge());
        }

        return scale(price);
    }

    public BigDecimal tax(BigDecimal subtotal) {
        return scale(subtotal.multiply(props.getTaxRate()));
    }

    public BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
