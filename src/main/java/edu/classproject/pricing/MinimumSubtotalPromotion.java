package edu.classproject.pricing;

import edu.classproject.common.Money;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Promotion strategy that applies another promotion only when the subtotal reaches a minimum.
 * This shows how new promo behavior can be added without changing existing promotion classes.
 */
public class MinimumSubtotalPromotion extends Promotion {
    private final Money minimumSubtotal;
    private final Promotion delegate;

    /**
     * The delegate promotion contains the actual discount rule, such as fixed amount or percentage.
     */
    public MinimumSubtotalPromotion(String code, LocalDate expiryDate, Money minimumSubtotal, Promotion delegate) {
        super(code, expiryDate);
        this.minimumSubtotal = Objects.requireNonNull(minimumSubtotal, "minimumSubtotal must not be null");
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        if (minimumSubtotal.amount().signum() < 0) {
            throw new IllegalArgumentException("minimumSubtotal must be non-negative");
        }
    }

    @Override
    public Money applyDiscount(Money subtotal) {
        // Below the threshold, the promo is valid but gives no discount.
        if (subtotal.amount().compareTo(minimumSubtotal.amount()) < 0) {
            return Money.of(0.0);
        }
        return delegate.applyDiscount(subtotal);
    }
}
