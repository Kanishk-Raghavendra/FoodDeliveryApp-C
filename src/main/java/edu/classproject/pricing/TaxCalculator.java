package edu.classproject.pricing;

import edu.classproject.common.Money;

import java.math.BigDecimal;

/**
 * Handles tax calculation separately so the tax rule is not mixed into the main service.
 */
public class TaxCalculator {
    private static final BigDecimal TAX_RATE = BigDecimal.valueOf(0.10);

    /**
     * Applies the current tax rate to the taxable amount.
     */
    public Money calculate(Money amount) {
        return new Money(amount.amount().multiply(TAX_RATE));
    }
}
