package edu.classproject.pricing;

import edu.classproject.common.Money;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory promo repository used by demos and unit tests instead of a database.
 */
public class InMemoryPromoRepository implements PromoRepository {
    private final Map<String, Promotion> byCode = new HashMap<>();

    /**
     * Default active promotions available when the service is created without custom test data.
     */
    public InMemoryPromoRepository() {
        this(List.of(
                new PercentagePromotion("SAVE10", LocalDate.now().plusYears(5), 10.0),
                new PercentagePromotion("SAVE20", LocalDate.now().plusYears(5), 20.0),
                new PercentagePromotion("HALF50", LocalDate.now().plusYears(5), 50.0),
                new FixedAmountPromotion("FLAT5", LocalDate.now().plusYears(5), Money.of(5.0)),
                new FixedAmountPromotion("WELCOME3", LocalDate.now().plusYears(5), Money.of(3.0)),
                new MinimumSubtotalPromotion(
                        "BIGORDER25",
                        LocalDate.now().plusYears(5),
                        Money.of(5.0),
                        new PercentagePromotion("BIGORDER25-RULE", LocalDate.now().plusYears(5), 25.0)
                )
        ));
    }

    /**
     * Allows tests to inject exactly the promo set needed for a scenario.
     */
    public InMemoryPromoRepository(List<Promotion> promotions) {
        promotions.forEach(this::addPromotion);
    }

    /**
     * Stores promo codes in uppercase to make lookup case-insensitive.
     */
    public void addPromotion(Promotion promotion) {
        byCode.put(promotion.code().toUpperCase(), promotion);
    }

    @Override
    public Optional<Promotion> findByCode(String code) {
        // Blank or null codes behave like "no promo" instead of failing checkout.
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(byCode.get(code.toUpperCase()));
    }
}
