package edu.classproject.pricing;

import edu.classproject.cart.Cart;

public interface PricingService {
    PriceBreakdown calculatePrice(Cart cart, String promoCode);

    default PriceBreakdown calculate(Cart cart, String couponCode) {
        return calculatePrice(cart, couponCode);
    }
}
