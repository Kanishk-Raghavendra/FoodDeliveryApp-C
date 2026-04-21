package edu.classproject.pricing;

import edu.classproject.cart.Cart;
import edu.classproject.cart.CartLine;
import edu.classproject.common.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
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

    // Verifies a different promotion behavior using MinimumSubtotalPromotion.
    // BIGORDER25 only applies when the cart subtotal reaches the configured minimum.
    @Test
    void calculatePrice_shouldApplyMinimumSubtotalPromoOnlyWhenThresholdIsMet() {
        PricingService service = new PricingServiceImpl();

        PriceBreakdown smallCartBreakdown = service.calculatePrice(sampleCart(), "BIGORDER25");
        PriceBreakdown bigCartBreakdown = service.calculatePrice(bigCart(), "BIGORDER25");

        assertEquals(Money.of(0.0), smallCartBreakdown.discount());
        assertEquals(Money.of(1.25), bigCartBreakdown.discount());
        assertEquals(Money.of(10.29), bigCartBreakdown.total());
    }

    // Verifies null input validation at the service boundary.
    // Expected: a null cart is rejected instead of producing a misleading price.
    @Test
    void calculatePrice_shouldRejectNullCart() {
        PricingService service = new PricingServiceImpl();

        assertThrows(NullPointerException.class, () ->
                service.calculatePrice(null, "SAVE10")
        );
    }

    // Verifies null promo-list behavior for the multi-promo API.
    // Expected: null promo list behaves like no promo codes.
    @Test
    void calculatePrice_shouldTreatNullPromoListAsNoPromos() {
        PricingService service = new PricingServiceImpl();

        PriceBreakdown breakdown = service.calculatePrice(sampleCart(), (List<String>) null);

        assertEquals(Money.of(0.0), breakdown.discount());
        assertEquals(Money.of(8.24), breakdown.total());
    }

    // Verifies null and blank entries inside a promo list.
    // Expected: invalid entries are ignored and valid entries still apply.
    @Test
    void calculatePrice_shouldIgnoreNullAndBlankPromoCodesInsideList() {
        PricingService service = new PricingServiceImpl();

        PriceBreakdown breakdown = service.calculatePrice(sampleCart(), List.of("", "   ", "SAVE10"));

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

    // Verifies null cart lines are handled like an empty cart.
    // Expected: no subtotal, no delivery fee, no tax, no discount, and zero total.
    @Test
    void calculatePrice_shouldReturnZeroTotalWhenCartLinesAreNull() {
        PricingService service = new PricingServiceImpl();
        Cart cart = new Cart("CART-NULL-LINES", "USR-1", "RST-1", null);

        PriceBreakdown breakdown = service.calculatePrice(cart, "SAVE10");

        assertEquals(Money.of(0.0), breakdown.subtotal());
        assertEquals(Money.of(0.0), breakdown.deliveryFee());
        assertEquals(Money.of(0.0), breakdown.tax());
        assertEquals(Money.of(0.0), breakdown.discount());
        assertEquals(Money.of(0.0), breakdown.total());
    }

    // Verifies quantity boundary behavior.
    // Expected: zero and negative quantities do not contribute to subtotal or delivery fee.
    @Test
    void calculatePrice_shouldIgnoreZeroAndNegativeQuantities() {
        PricingService service = new PricingServiceImpl();
        Cart cart = new Cart(
                "CART-QUANTITY-BOUNDARY",
                "USR-1",
                "RST-1",
                List.of(
                        new CartLine("MI-1", "Zero Item", 0),
                        new CartLine("MI-2", "Negative Item", -2),
                        new CartLine("MI-3", "One Item", 1)
                )
        );

        PriceBreakdown breakdown = service.calculatePrice(cart, (String) null);

        assertEquals(Money.of(1.0), breakdown.subtotal());
        assertEquals(Money.of(3.49), breakdown.deliveryFee());
        assertEquals(Money.of(0.45), breakdown.tax());
        assertEquals(Money.of(4.94), breakdown.total());
    }

    // Verifies large-input behavior.
    // Expected: a very large item count still calculates a rounded total without overflow.
    @Test
    void calculatePrice_shouldHandleVeryLargeQuantities() {
        PricingService service = new PricingServiceImpl();
        Cart cart = new Cart(
                "CART-LARGE",
                "USR-1",
                "RST-1",
                List.of(new CartLine("MI-1", "Bulk Item", 1_000_000))
        );

        PriceBreakdown breakdown = service.calculatePrice(cart, (String) null);

        assertEquals(Money.of(1_000_000.0), breakdown.subtotal());
        assertEquals(Money.of(500_002.99), breakdown.deliveryFee());
        assertEquals(Money.of(150_000.30), breakdown.tax());
        assertEquals(Money.of(1_650_003.29), breakdown.total());
    }

    // Boundary value analysis for percentage promotion setup.
    // Expected: 0% and 100% are valid boundaries.
    @Test
    void percentagePromotion_shouldAcceptZeroAndHundredPercentBoundaries() {
        PercentagePromotion zeroPercent = new PercentagePromotion("ZERO", LocalDate.now().plusDays(1), 0.0);
        PercentagePromotion hundredPercent = new PercentagePromotion("HUNDRED", LocalDate.now().plusDays(1), 100.0);

        assertEquals(Money.of(0.0), zeroPercent.applyDiscount(Money.of(10.0)));
        assertEquals(Money.of(10.0), hundredPercent.applyDiscount(Money.of(10.0)));
    }

    // Boundary value analysis below the percentage range.
    // Expected: negative percentages are rejected.
    @Test
    void percentagePromotion_shouldRejectPercentageBelowZero() {
        assertThrows(IllegalArgumentException.class, () ->
                new PercentagePromotion("BADNEGATIVE", LocalDate.now().plusDays(1), -0.01)
        );
    }

    // Verifies validation in percentage promotions.
    // Expected: percentages above 100 are rejected during promo setup.
    @Test
    void percentagePromotion_shouldRejectPercentageAboveHundred() {
        assertThrows(IllegalArgumentException.class, () ->
                new PercentagePromotion("BAD150", LocalDate.now().plusDays(1), 150.0)
        );
    }

    // Boundary value analysis for fixed discount setup and application.
    // Expected: zero, exact-subtotal, and above-subtotal discounts are handled safely.
    @Test
    void fixedAmountPromotion_shouldHandleZeroExactAndAboveSubtotalBoundaries() {
        Money subtotal = Money.of(3.0);

        FixedAmountPromotion zero = new FixedAmountPromotion("ZERO", LocalDate.now().plusDays(1), Money.of(0.0));
        FixedAmountPromotion exact = new FixedAmountPromotion("EXACT", LocalDate.now().plusDays(1), Money.of(3.0));
        FixedAmountPromotion above = new FixedAmountPromotion("ABOVE", LocalDate.now().plusDays(1), Money.of(3.01));

        assertEquals(Money.of(0.0), zero.applyDiscount(subtotal));
        assertEquals(Money.of(3.0), exact.applyDiscount(subtotal));
        assertEquals(Money.of(3.0), above.applyDiscount(subtotal));
    }

    // Verifies validation in fixed amount promotions.
    // Expected: negative fixed discounts are rejected during promo setup.
    @Test
    void fixedAmountPromotion_shouldRejectNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () ->
                new FixedAmountPromotion("NEGATIVE", LocalDate.now().plusDays(1), Money.of(-1.0))
        );
    }

    // Verifies null validation behavior for fixed discount setup.
    // Expected: current implementation rejects null amount by throwing NullPointerException.
    @Test
    void fixedAmountPromotion_shouldRejectNullAmount() {
        assertThrows(NullPointerException.class, () ->
                new FixedAmountPromotion("NULL", LocalDate.now().plusDays(1), null)
        );
    }

    // Boundary value analysis for minimum subtotal promotion.
    // Expected: below threshold gives no discount; exact and above threshold apply delegate discount.
    @Test
    void minimumSubtotalPromotion_shouldHandleBelowExactAndAboveThreshold() {
        MinimumSubtotalPromotion promo = new MinimumSubtotalPromotion(
                "MIN10",
                LocalDate.now().plusDays(1),
                Money.of(10.0),
                new FixedAmountPromotion("MIN10-RULE", LocalDate.now().plusDays(1), Money.of(2.0))
        );

        assertEquals(Money.of(0.0), promo.applyDiscount(new Money(BigDecimal.valueOf(9.99))));
        assertEquals(Money.of(2.0), promo.applyDiscount(Money.of(10.0)));
        assertEquals(Money.of(2.0), promo.applyDiscount(new Money(BigDecimal.valueOf(10.01))));
    }

    // Verifies constructor validation for minimum-subtotal promotions.
    // Expected: invalid threshold or missing delegate is rejected immediately.
    @Test
    void minimumSubtotalPromotion_shouldRejectInvalidSetup() {
        assertThrows(IllegalArgumentException.class, () ->
                new MinimumSubtotalPromotion(
                        "BADMIN",
                        LocalDate.now().plusDays(1),
                        Money.of(-0.01),
                        new FixedAmountPromotion("BADMIN-RULE", LocalDate.now().plusDays(1), Money.of(1.0))
                )
        );

        assertThrows(NullPointerException.class, () ->
                new MinimumSubtotalPromotion("NODELEGATE", LocalDate.now().plusDays(1), Money.of(1.0), null)
        );
    }

    // Verifies expiry-date boundaries.
    // Expected: yesterday is expired, today and tomorrow are valid.
    @Test
    void promotion_shouldTreatTodayAsValidExpiryBoundary() {
        Promotion yesterday = new PercentagePromotion("YESTERDAY", LocalDate.now().minusDays(1), 10.0);
        Promotion today = new PercentagePromotion("TODAY", LocalDate.now(), 10.0);
        Promotion tomorrow = new PercentagePromotion("TOMORROW", LocalDate.now().plusDays(1), 10.0);

        assertEquals(false, yesterday.isValid());
        assertEquals(true, today.isValid());
        assertEquals(true, tomorrow.isValid());
    }

    // Verifies repeated promo-code behavior.
    // Expected: duplicate promo codes are applied repeatedly because the current policy permits it.
    @Test
    void calculatePrice_shouldApplyDuplicatePromoCodesRepeatedly() {
        PricingService service = new PricingServiceImpl();

        PriceBreakdown breakdown = service.calculatePrice(sampleCart(), List.of("SAVE10", "SAVE10"));

        assertEquals(Money.of(0.57), breakdown.discount());
        assertEquals(Money.of(7.67), breakdown.total());
    }

    // Verifies repository state and lookup behavior.
    // Expected: added promo can be found with any casing, while blanks are treated as absent.
    @Test
    void inMemoryPromoRepository_shouldFindAddedPromotionCaseInsensitively() {
        InMemoryPromoRepository repository = new InMemoryPromoRepository(List.of());
        repository.addPromotion(new PercentagePromotion("NEW10", LocalDate.now().plusDays(1), 10.0));

        assertEquals(true, repository.findByCode("new10").isPresent());
        assertEquals(true, repository.findByCode("NEW10").isPresent());
        assertEquals(true, repository.findByCode("   ").isEmpty());
    }

    // Verifies repository invalid-input behavior.
    // Expected: current implementation rejects null promotion when adding to the repository.
    @Test
    void inMemoryPromoRepository_shouldRejectNullPromotionWhenAdding() {
        InMemoryPromoRepository repository = new InMemoryPromoRepository(List.of());

        assertThrows(NullPointerException.class, () -> repository.addPromotion(null));
    }

    // Verifies collaborator failure behavior.
    // Expected: an unexpected repository failure propagates to the caller instead of being hidden.
    @Test
    void calculatePrice_shouldPropagatePromoRepositoryFailure() {
        PromoRepository failingRepository = code -> {
            throw new IllegalStateException("promo store unavailable");
        };
        PricingService service = new PricingServiceImpl(
                new TaxCalculator(),
                new FeeCalculator(),
                failingRepository
        );

        assertThrows(IllegalStateException.class, () ->
                service.calculatePrice(sampleCart(), "SAVE10")
        );
    }

    // Verifies tax calculator null-input behavior.
    // Expected: current implementation rejects null money amount.
    @Test
    void taxCalculator_shouldRejectNullAmount() {
        TaxCalculator calculator = new TaxCalculator();

        assertThrows(NullPointerException.class, () -> calculator.calculate(null));
    }

    // Verifies fee calculator null-input behavior.
    // Expected: current implementation rejects null cart.
    @Test
    void feeCalculator_shouldRejectNullCart() {
        FeeCalculator calculator = new FeeCalculator();

        assertThrows(NullPointerException.class, () -> calculator.calculateDeliveryFee(null));
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

    private Cart bigCart() {
        return new Cart(
                "CART-2",
                "USR-1",
                "RST-1",
                List.of(
                        new CartLine("MI-1", "Burger", 3),
                        new CartLine("MI-2", "Fries", 2)
                )
        );
    }
}
