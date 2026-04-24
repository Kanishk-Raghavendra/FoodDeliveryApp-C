package edu.classproject.pricing;

import edu.classproject.cart.Cart;

import java.util.List;

/**
 * Public contract for the pricing module.
 * Other modules call this interface instead of depending on PricingServiceImpl directly.
 */
public interface PricingService {
    /**
     * Backward-compatible method for callers that still pass only one promo code.
     */
    default PriceBreakdown calculatePrice(Cart cart, String promoCode) {
        return calculatePrice(cart, promoCode == null ? List.of() : List.of(promoCode));
    }

    /**
     * Calculates a full price summary for a cart and applies multiple promo codes in order.
     */
    PriceBreakdown calculatePrice(Cart cart, List<String> promoCodes);

    /**
     * Alias kept for older checkout code that used the word coupon instead of promo.
     */
    default PriceBreakdown calculate(Cart cart, String couponCode) {
        return calculatePrice(cart, couponCode);
    }
}
