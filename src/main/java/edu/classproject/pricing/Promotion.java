package edu.classproject.pricing;

import edu.classproject.common.Money;

import java.time.LocalDate;
import java.util.Objects;

public abstract class Promotion {
    private final String code;
    private final LocalDate expiryDate;

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

    public boolean isValid() {
        return !LocalDate.now().isAfter(expiryDate);
    }

    public abstract Money applyDiscount(Money subtotal);
}