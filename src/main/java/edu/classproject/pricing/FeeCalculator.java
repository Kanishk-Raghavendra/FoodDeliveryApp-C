package edu.classproject.pricing;

import edu.classproject.cart.Cart;
import edu.classproject.common.Money;

/**
 * Calculates delivery charges from cart size.
 */
public class FeeCalculator {
    private static final double BASE_FEE = 2.99;
    private static final double PER_ITEM_FEE = 0.50;

    /**
     * Empty carts have no delivery fee; otherwise fee is base fee plus per-item fee.
     */
    public Money calculateDeliveryFee(Cart cart) {
        if (cart.lines() == null || cart.lines().isEmpty()) {
            return Money.of(0.0);
        }

        int itemCount = cart.lines().stream()
                .mapToInt(line -> Math.max(0, line.quantity()))
                .sum();

        return Money.of(BASE_FEE + (itemCount * PER_ITEM_FEE));
    }
}
