package edu.classproject.pricing;

import edu.classproject.cart.Cart;
import edu.classproject.common.Money;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Coordinates pricing calculation by combining subtotal, fees, tax, and promotions.
 */
public class PricingServiceImpl implements PricingService {
    private final TaxCalculator taxCalculator;
    private final FeeCalculator feeCalculator;
    private final PromoRepository promoRepository;

    /**
     * Convenience constructor for demos; production-style code can inject dependencies below.
     */
    public PricingServiceImpl() {
        this(new TaxCalculator(), new FeeCalculator(), new InMemoryPromoRepository());
    }

    /**
     * Constructor dependency injection makes calculators and promo lookup replaceable in tests.
     */
    public PricingServiceImpl(TaxCalculator taxCalculator,
                              FeeCalculator feeCalculator,
                              PromoRepository promoRepository) {
        this.taxCalculator = Objects.requireNonNull(taxCalculator, "taxCalculator must not be null");
        this.feeCalculator = Objects.requireNonNull(feeCalculator, "feeCalculator must not be null");
        this.promoRepository = Objects.requireNonNull(promoRepository, "promoRepository must not be null");
    }

    @Override
    public PriceBreakdown calculatePrice(Cart cart, String promoCode) {
        return calculatePrice(cart, promoCode == null ? List.of() : List.of(promoCode));
    }

    @Override
    public PriceBreakdown calculatePrice(Cart cart, List<String> promoCodes) {
        Objects.requireNonNull(cart, "cart must not be null");

        Money subtotal = calculateSubtotal(cart);
        Money deliveryFee = feeCalculator.calculateDeliveryFee(cart);
        Money tax = taxCalculator.calculate(subtotal.add(deliveryFee));
        Money discount = resolveDiscount(promoCodes, subtotal);
        Money totalBeforeDiscount = subtotal.add(deliveryFee).add(tax);
        Money total = subtractOrZero(totalBeforeDiscount, discount);

        return new PriceBreakdown(subtotal, deliveryFee, tax, discount, total);
    }

    private Money resolveDiscount(List<String> promoCodes, Money subtotal) {
        if (promoCodes == null || promoCodes.isEmpty()) {
            return Money.of(0.0);
        }

        Money totalDiscount = Money.of(0.0);
        Money remainingDiscountableSubtotal = subtotal;

        for (String promoCode : promoCodes) {
            Money currentDiscountableSubtotal = remainingDiscountableSubtotal;
            Money discount = promoRepository.findByCode(promoCode)
                    .filter(Promotion::isValid)
                    .map(promo -> promo.applyDiscount(currentDiscountableSubtotal))
                    .orElseGet(() -> Money.of(0.0));

            totalDiscount = totalDiscount.add(discount);
            // Each next promo applies only to the subtotal left after earlier discounts.
            remainingDiscountableSubtotal = subtractOrZero(remainingDiscountableSubtotal, discount);
        }

        return totalDiscount;
    }

    private Money calculateSubtotal(Cart cart) {
        // CartLine currently has quantity but no price, so subtotal uses unit value 1.0 per item.
        int itemCount = cart.lines() == null ? 0 : cart.lines().stream()
                .mapToInt(line -> Math.max(0, line.quantity()))
                .sum();

        return Money.of(itemCount);
    }

    private Money subtractOrZero(Money amount, Money discount) {
        // Guard against combined promotions pushing a money value below zero.
        BigDecimal result = amount.amount().subtract(discount.amount());
        if (result.signum() < 0) {
            return Money.of(0.0);
        }
        return new Money(result);
    }
}
