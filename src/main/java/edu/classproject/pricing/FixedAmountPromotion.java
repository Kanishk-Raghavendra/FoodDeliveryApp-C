package edu.classproject.pricing;

import edu.classproject.common.Money;

import java.time.LocalDate;

public class FixedAmountPromotion extends Promotion {
    private final Money amount;

    public FixedAmountPromotion(String code, LocalDate expiryDate, Money amount) {
        super(code, expiryDate);
        if (amount.amount().signum() < 0) {
            throw new IllegalArgumentException("amount must be non-negative");
        }
        this.amount = amount;
    }

    @Override
    public Money applyDiscount(Money subtotal) {
        if (amount.amount().compareTo(subtotal.amount()) > 0) {
            return subtotal;
        }
        return amount;
    }
}