package edu.classproject.pricing;

import edu.classproject.common.Money;

import java.math.BigDecimal;

public class TaxCalculator {
    private static final BigDecimal TAX_RATE = BigDecimal.valueOf(0.10);

    public Money calculate(Money amount) {
        return new Money(amount.amount().multiply(TAX_RATE));
    }
}