package edu.classproject.pricing;

import java.util.Optional;

public interface PromoRepository {
    Optional<Promotion> findByCode(String code);
}