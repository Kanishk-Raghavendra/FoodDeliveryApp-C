package edu.classproject.pricing;

import edu.classproject.cart.Cart;

public class FeeCalculator {
    private static final double BASE_FEE = 2.99;
    private static final double PER_ITEM_FEE = 0.50;

    public edu.classproject.common.Money calculateDeliveryFee(Cart cart) {
        if (cart.lines() == null || cart.lines().isEmpty()) {
            return edu.classproject.common.Money.of(0.0);
        }

        int itemCount = cart.lines().stream()
                .mapToInt(line -> Math.max(0, line.quantity()))
                .sum();

        return edu.classproject.common.Money.of(BASE_FEE + (itemCount * PER_ITEM_FEE));
    }
}