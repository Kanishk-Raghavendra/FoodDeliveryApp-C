package edu.classproject.pricing;

import edu.classproject.cart.Cart;
import edu.classproject.cart.CartLine;
import edu.classproject.common.Money;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PricingServiceImplTest {

    @Test
    void calculatePrice_shouldReturnBreakdown_whenNoPromo() {
        PricingService service = new PricingServiceImpl(
                new TaxCalculator(),
                new FeeCalculator(),
                new InMemoryPromoRepository(List.of())
        );

        Cart cart = sampleCart();
        PriceBreakdown breakdown = service.calculatePrice(cart, null);

        assertEquals(Money.of(3.0), breakdown.subtotal());
        assertEquals(Money.of(4.49), breakdown.deliveryFee());
        assertEquals(Money.of(0.75), breakdown.tax());
        assertEquals(Money.of(0.0), breakdown.discount());
        assertEquals(Money.of(8.24), breakdown.total());
    }

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