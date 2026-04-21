package edu.classproject.pricing;

import edu.classproject.cart.Cart;
import edu.classproject.cart.CartLine;
import edu.classproject.common.Money;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PricingServiceImplTest {

    // Verifies the basic pricing flow when no promo code is supplied.
    // Expected: subtotal + delivery fee + tax, with zero discount.
    @Test
    void calculatePrice_shouldReturnBreakdown_whenNoPromo() {
        PricingService service = new PricingServiceImpl(
                new TaxCalculator(),
                new FeeCalculator(),
                new InMemoryPromoRepository(List.of())
        );

        Cart cart = sampleCart();
        PriceBreakdown breakdown = service.calculatePrice(cart, (String) null);

        assertEquals(Money.of(3.0), breakdown.subtotal());
        assertEquals(Money.of(4.49), breakdown.deliveryFee());
        assertEquals(Money.of(0.75), breakdown.tax());
        assertEquals(Money.of(0.0), breakdown.discount());
        assertEquals(Money.of(8.24), breakdown.total());
    }

    // Verifies safe promo handling for unknown and expired promo codes.
    // Expected: checkout does not crash and discount remains zero.
    @Test
    void calculatePrice_shouldIgnoreInvalidOrExpiredPromo() {
        PricingService service = new PricingServiceImpl(
                new TaxCalculator(),
                new FeeCalculator(),
                new InMemoryPromoRepository(List.of(
                        new PercentagePromotion("OLD10", LocalDate.now().minusDays(1), 10.0)
                ))
        );

        Cart cart = sampleCart();
        PriceBreakdown unknownPromo = service.calculatePrice(cart, "NOPE");
        PriceBreakdown expiredPromo = service.calculatePrice(cart, "OLD10");

        assertEquals(Money.of(0.0), unknownPromo.discount());
        assertEquals(Money.of(0.0), expiredPromo.discount());
    }

    // Verifies percentage promotion calculation.
    // SAVE10 gives 10% off the subtotal, so $3.00 subtotal becomes $0.30 discount.
    @Test
    void calculatePrice_shouldApplyPercentagePromo() {
        PricingService service = new PricingServiceImpl(
                new TaxCalculator(),
                new FeeCalculator(),
                new InMemoryPromoRepository(List.of(
                        new PercentagePromotion("SAVE10", LocalDate.now().plusDays(1), 10.0)
                ))
        );

        PriceBreakdown breakdown = service.calculatePrice(sampleCart(), "SAVE10");

        assertEquals(Money.of(0.3), breakdown.discount());
        assertEquals(Money.of(7.94), breakdown.total());
        assertEquals("$7.94", breakdown.getFormattedTotal());
    }

    // Verifies fixed amount promotion calculation.
    // FLAT5 is larger than the $3.00 subtotal, so the discount is capped at subtotal.
    @Test
    void calculatePrice_shouldApplyFixedAmountPromoWithoutGoingBelowSubtotal() {
        PricingService service = new PricingServiceImpl(
                new TaxCalculator(),
                new FeeCalculator(),
                new InMemoryPromoRepository(List.of(
                        new FixedAmountPromotion("FLAT5", LocalDate.now().plusDays(1), Money.of(5.0))
                ))
        );

        PriceBreakdown breakdown = service.calculatePrice(sampleCart(), "FLAT5");

        assertEquals(Money.of(3.0), breakdown.subtotal());
        assertEquals(Money.of(3.0), breakdown.discount());
        assertEquals(Money.of(5.24), breakdown.total());
    }

    // Verifies that promo code lookup is case-insensitive.
    // The repository stores SAVE20, but the customer can enter save20.
    @Test
    void calculatePrice_shouldApplyDefaultPromoIgnoringCase() {
        PricingService service = new PricingServiceImpl();

        PriceBreakdown breakdown = service.calculatePrice(sampleCart(), "save20");

        assertEquals(Money.of(0.6), breakdown.discount());
        assertEquals(Money.of(7.64), breakdown.total());
    }

    // Verifies multiple promo support.
    // SAVE10 applies first, then SAVE20 applies to the remaining discountable subtotal.
    @Test
    void calculatePrice_shouldApplyMultiplePercentagePromosInOrder() {
        PricingService service = new PricingServiceImpl();

        PriceBreakdown breakdown = service.calculatePrice(sampleCart(), List.of("SAVE10", "SAVE20"));

        assertEquals(Money.of(3.0), breakdown.subtotal());
        assertEquals(Money.of(0.84), breakdown.discount());
        assertEquals(Money.of(7.40), breakdown.total());
    }

    // Verifies that multiple stacked promos are capped at the subtotal.
    // SAVE10 gives $0.30 off, then WELCOME3 is capped to the remaining $2.70 subtotal.
    @Test
    void calculatePrice_shouldCapCombinedPromoDiscountAtSubtotal() {
        PricingService service = new PricingServiceImpl();

        PriceBreakdown breakdown = service.calculatePrice(sampleCart(), List.of("SAVE10", "WELCOME3"));

        assertEquals(Money.of(3.0), breakdown.subtotal());
        assertEquals(Money.of(3.0), breakdown.discount());
        assertEquals(Money.of(5.24), breakdown.total());
    }

    // Verifies mixed promo lists.
    // Unknown and expired codes are skipped, while valid codes in the same list still apply.
    @Test
    void calculatePrice_shouldSkipInvalidPromosWhenApplyingMultiplePromoCodes() {
        PricingService service = new PricingServiceImpl(
                new TaxCalculator(),
                new FeeCalculator(),
                new InMemoryPromoRepository(List.of(
                        new PercentagePromotion("SAVE10", LocalDate.now().plusDays(1), 10.0),
                        new PercentagePromotion("OLD20", LocalDate.now().minusDays(1), 20.0)
                ))
        );

        PriceBreakdown breakdown = service.calculatePrice(sampleCart(), List.of("NOPE", "SAVE10", "OLD20"));

        assertEquals(Money.of(0.3), breakdown.discount());
        assertEquals(Money.of(7.94), breakdown.total());
    }

    // Verifies empty-cart pricing behavior.
    // Expected: no subtotal, no delivery fee, no tax, no discount, and zero total.
    @Test
    void calculatePrice_shouldReturnZeroTotalForEmptyCart() {
        PricingService service = new PricingServiceImpl();
        Cart emptyCart = new Cart("CART-EMPTY", "USR-1", "RST-1", List.of());

        PriceBreakdown breakdown = service.calculatePrice(emptyCart, "HALF50");

        assertEquals(Money.of(0.0), breakdown.subtotal());
        assertEquals(Money.of(0.0), breakdown.deliveryFee());
        assertEquals(Money.of(0.0), breakdown.tax());
        assertEquals(Money.of(0.0), breakdown.discount());
        assertEquals(Money.of(0.0), breakdown.total());
    }

    // Verifies validation in percentage promotions.
    // Expected: percentages above 100 are rejected during promo setup.
    @Test
    void percentagePromotion_shouldRejectPercentageAboveHundred() {
        assertThrows(IllegalArgumentException.class, () ->
                new PercentagePromotion("BAD150", LocalDate.now().plusDays(1), 150.0)
        );
    }

    // Verifies validation in fixed amount promotions.
    // Expected: negative fixed discounts are rejected during promo setup.
    @Test
    void fixedAmountPromotion_shouldRejectNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () ->
                new FixedAmountPromotion("NEGATIVE", LocalDate.now().plusDays(1), Money.of(-1.0))
        );
    }

    private Cart sampleCart() {
        return new Cart(
                "CART-1",
                "USR-1",
                "RST-1",
                List.of(
                        new CartLine("MI-1", "Burger", 2),
                        new CartLine("MI-2", "Fries", 1)
                )
        );
    }
}
