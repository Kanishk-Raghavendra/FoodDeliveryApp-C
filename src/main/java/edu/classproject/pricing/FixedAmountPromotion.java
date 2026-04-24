package edu.classproject.pricing;

import edu.classproject.common.Money;

import java.time.LocalDate;

/**
 * Promotion strategy that subtracts a fixed amount from the current subtotal.
 */
public class FixedAmountPromotion extends Promotion {
    private final Money amount;

    /**
     * Fixed discount amount must not be negative.
     */
    public FixedAmountPromotion(String code, LocalDate expiryDate, Money amount) {
        super(code, expiryDate);
        if (amount.amount().signum() < 0) {
            throw new IllegalArgumentException("amount must be non-negative");
        }
        this.amount = amount;
    }

    @Override
    public Money applyDiscount(Money subtotal) {
        // Cap the fixed discount at the subtotal so the discountable amount never goes negative.
        if (amount.amount().compareTo(subtotal.amount()) > 0) {
            return subtotal;
        }
        return amount;
    }
}
