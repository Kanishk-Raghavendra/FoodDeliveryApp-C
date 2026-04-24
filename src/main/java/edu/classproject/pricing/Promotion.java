package edu.classproject.pricing;

import edu.classproject.common.Money;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Base class for all promotion strategies.
 */
public abstract class Promotion {
    private final String code;
    private final LocalDate expiryDate;

    /**
     * Common validation shared by all promotion types.
     */
    protected Promotion(String code, LocalDate expiryDate) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Promotion code must not be blank");
        }
        this.code = code;
        this.expiryDate = Objects.requireNonNull(expiryDate, "expiryDate must not be null");
    }

    public String code() {
        return code;
    }

    public LocalDate expiryDate() {
        return expiryDate;
    }

    /**
     * A promotion is valid through its expiry date.
     */
    public boolean isValid() {
        return !LocalDate.now().isAfter(expiryDate);
    }

    /**
     * Each concrete promotion decides how much discount it gives.
     */
    public abstract Money applyDiscount(Money subtotal);
}
