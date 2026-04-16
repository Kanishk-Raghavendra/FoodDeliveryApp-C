package edu.classproject.pricing;

import edu.classproject.common.Money;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryPromoRepository implements PromoRepository {
    private final Map<String, Promotion> byCode = new HashMap<>();

    public InMemoryPromoRepository() {
        this(List.of(
                new PercentagePromotion("SAVE10", LocalDate.now().plusYears(5), 10.0),
                new FixedAmountPromotion("FLAT5", LocalDate.now().plusYears(5), Money.of(5.0))
        ));
    }

    public InMemoryPromoRepository(List<Promotion> promotions) {
        promotions.forEach(this::addPromotion);
    }

    public void addPromotion(Promotion promotion) {
        byCode.put(promotion.code().toUpperCase(), promotion);
    }

    @Override
    public Optional<Promotion> findByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(byCode.get(code.toUpperCase()));
    }
}