package edu.classproject.pricing;

import edu.classproject.cart.Cart;
import edu.classproject.common.Money;

import java.math.BigDecimal;
import java.util.Objects;

public class PricingServiceImpl implements PricingService {
    private final TaxCalculator taxCalculator;
    private final FeeCalculator feeCalculator;
    private final PromoRepository promoRepository;

    public PricingServiceImpl() {
        this(new TaxCalculator(), new FeeCalculator(), new InMemoryPromoRepository());
    }

    public PricingServiceImpl(TaxCalculator taxCalculator,
                              FeeCalculator feeCalculator,
                              PromoRepository promoRepository) {
        this.taxCalculator = Objects.requireNonNull(taxCalculator, "taxCalculator must not be null");
        this.feeCalculator = Objects.requireNonNull(feeCalculator, "feeCalculator must not be null");
        this.promoRepository = Objects.requireNonNull(promoRepository, "promoRepository must not be null");
    }

    @Override
    public PriceBreakdown calculatePrice(Cart cart, String promoCode) {
        Objects.requireNonNull(cart, "cart must not be null");

        Money subtotal = calculateSubtotal(cart);
        Money deliveryFee = feeCalculator.calculateDeliveryFee(cart);
        Money tax = taxCalculator.calculate(subtotal.add(deliveryFee));
        Money discount = resolveDiscount(promoCode, subtotal);
        Money totalBeforeDiscount = subtotal.add(deliveryFee).add(tax);
        Money total = subtractOrZero(totalBeforeDiscount, discount);

        return new PriceBreakdown(subtotal, deliveryFee, tax, discount, total);
    }

    private Money resolveDiscount(String promoCode, Money subtotal) {
        return promoRepository.findByCode(promoCode)
                .filter(Promotion::isValid)
                .map(promo -> promo.applyDiscount(subtotal))
                .orElseGet(() -> Money.of(0.0));
    }

    private Money calculateSubtotal(Cart cart) {
        // CartLine currently has quantity but no price, so subtotal uses unit value 1.0 per item.
        int itemCount = cart.lines() == null ? 0 : cart.lines().stream()
                .mapToInt(line -> Math.max(0, line.quantity()))
                .sum();

        return Money.of(itemCount);
    }

    private Money subtractOrZero(Money amount, Money discount) {
        BigDecimal result = amount.amount().subtract(discount.amount());
        if (result.signum() < 0) {
            return Money.of(0.0);
        }
        return new Money(result);
    }
}