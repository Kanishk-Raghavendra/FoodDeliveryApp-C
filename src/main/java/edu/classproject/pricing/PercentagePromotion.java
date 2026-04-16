package edu.classproject.pricing;

import edu.classproject.common.Money;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PercentagePromotion extends Promotion {
    private final BigDecimal percentage;

    public PercentagePromotion(String code, LocalDate expiryDate, double percentage) {
        super(code, expiryDate);
        if (percentage < 0.0 || percentage > 100.0) {
            throw new IllegalArgumentException("percentage must be between 0 and 100");
        }
        this.percentage = BigDecimal.valueOf(percentage);
    }

    @Override
    public Money applyDiscount(Money subtotal) {
        BigDecimal discount = subtotal.amount()
                .multiply(percentage)
                .divide(BigDecimal.valueOf(100));

        if (discount.compareTo(subtotal.amount()) > 0) {
            discount = subtotal.amount();
        }

        return new Money(discount);
    }
}